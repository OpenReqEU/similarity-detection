package upc.similarity.compareapi.dao;

import org.json.JSONArray;
import upc.similarity.compareapi.dao.algorithm_models_dao.similarity_algorithm.SimilarityModelDatabase;
import upc.similarity.compareapi.entity.*;
import upc.similarity.compareapi.similarity_algorithm.SimilarityModel;
import upc.similarity.compareapi.util.Logger;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.exception.NotFoundException;
import upc.similarity.compareapi.util.Time;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public class SQLiteDatabase implements DatabaseModel {

    private Lock mainDbLock = new ReentrantLock(true);
    private String dbMainName = "main";
    private String dbPath;
    private Integer sleepTime;
    private SimilarityModelDatabase similarityModelDatabase;
    private Logger logger = Logger.getInstance();

    public SQLiteDatabase(String dbPath, int sleepTime, SimilarityModelDatabase similarityModelDatabase) throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        this.dbPath = dbPath;
        this.sleepTime = sleepTime;
        this.similarityModelDatabase = similarityModelDatabase;
    }


    /*
    Sync methods
     */

    //is public to be accessible by tests
    public void getAccessToMainDb() throws InternalErrorException {
        try {
            if (!mainDbLock.tryLock(sleepTime, TimeUnit.SECONDS)) throw new InternalErrorException("The main database is locked, another thread is using it");
        } catch (InterruptedException e) {
            Logger.getInstance().showErrorMessage(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    //is public to be accessible by tests
    public void releaseAccessToMainDb() {
        mainDbLock.unlock();
    }


     /*
    Public methods
    Main methods
     */

    @Override
    public boolean existsOrganization(String organizationId) throws InternalErrorException {
        try {
            boolean result = true;
            try (Connection conn = getConnection(dbMainName);
                 PreparedStatement ps = conn.prepareStatement("SELECT id FROM organizations WHERE id = ?")) {
                ps.setString(1, organizationId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) result = false;
                }
            }
            return result;
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while checking the existence of an organization",organizationId);
        }
    }

    @Override
    public boolean existReqInOrganizationModel(String organizationId, String requirement) throws NotFoundException, InternalErrorException {
        if (!existsOrganization(organizationId)) throw new NotFoundException("The organization " + organizationId + " does not exist");
        try (Connection conn = getConnection(organizationId)) {
            return similarityModelDatabase.existsReqInsideModel(requirement,conn);
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while checking if a requirement was inside an organizaton", organizationId);
        }
    }

    @Override
    public Organization getOrganizationInfo(String organizationId) throws NotFoundException, InternalErrorException {

        if (!existsOrganization(organizationId)) throw new NotFoundException("The organization with id " + organizationId + " does not exist");
        try {
            List<Execution> currentExecutions = new ArrayList<>();
            List<Execution> pendingResponses = new ArrayList<>();
            OrganizationModels organizationInfo;
            try (Connection conn = getConnection(organizationId)) {
                organizationInfo = getOrganizationInfo(organizationId, conn);
            }
            try (Connection conn = getConnection(dbMainName)) {
                String sql = "SELECT responseId, maxPages, actualPage, finished, startTime, finalTime, methodName FROM responses WHERE organizationId = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, organizationId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String responseId = rs.getString("responseId");
                            boolean finished = rs.getBoolean("finished");
                            long startTime = rs.getLong("startTime");
                            long finalTime = rs.getLong("finalTime");
                            String methodName = rs.getString("methodName");
                            Integer pagesLeft = null;
                            Execution execution;
                            if (finished) {
                                int maxPages = rs.getInt("maxPages");
                                int actualPage = rs.getInt("actualPage");
                                pagesLeft = maxPages - actualPage;
                                execution = new Execution(responseId, methodName, pagesLeft, startTime, finalTime);
                            } else {
                                execution = new Execution(responseId, methodName, pagesLeft, startTime, null);
                            }
                            if (finished) pendingResponses.add(execution);
                            else currentExecutions.add(execution);
                        }
                    }
                }
            }
            return new Organization(organizationId, organizationInfo.getThreshold(), organizationInfo.isCompare(), organizationInfo.hasClusters(), currentExecutions, pendingResponses);
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while laoding the information of an organization",organizationId);
        }
    }

    @Override
    public OrganizationModels getOrganizationModels(String organization, boolean readOnly) throws NotFoundException, InternalErrorException {

        if (!existsOrganization(organization)) throw new NotFoundException("The organization with id " + organization + " does not exist");
        try {
            OrganizationModels organizationModels;
            try (Connection conn = getConnection(organization)) {
                conn.setAutoCommit(false);
                organizationModels = getOrganizationInfo(organization, conn);
                organizationModels.setSimilarityModel(similarityModelDatabase.getModel(readOnly, conn));
                if (organizationModels.hasClusters()) {
                    Map<Integer, List<String>> clusters = loadClusters(conn);
                    organizationModels.setClusters(clusters);
                }
                conn.commit();
            }
            return organizationModels;
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while loading organization models",organization);
        }
    }

    @Override
    public void saveOrganizationModels(String organization, OrganizationModels organizationModels) throws InternalErrorException {
        try {
            insertNewOrganization(organization);
            try (Connection conn = getConnection(organization)) {
                conn.setAutoCommit(false);
                clearOrganizationTables(conn);
                saveOrganizationInfo(organization, organizationModels, conn);
                conn.commit();
            }
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while saving the models of an organization",organization);
        }
    }


    /*
    Public methods
    Cluster methods
     */

    @Override
    public Dependency getDependency(String organizationId, String fromid, String toid, boolean useAuxiliaryTables) throws NotFoundException, InternalErrorException {
        Dependency result;
        try (Connection conn = getConnection(organizationId)) {
            String sql = "SELECT status, clusterId, score FROM dependencies WHERE (fromid = ? AND toid = ?) OR (fromid = ? AND toid = ?)";
            if (useAuxiliaryTables) sql = "SELECT status, clusterId, score FROM aux_dependencies WHERE (fromid = ? AND toid = ?) OR (fromid = ? AND toid = ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, fromid);
                ps.setString(2, toid);
                ps.setString(3, toid);
                ps.setString(4, fromid);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString(1);
                        int clusterId = rs.getInt(2);
                        double score = rs.getDouble(3);
                        result = new Dependency(fromid, toid, status, score, clusterId);
                    } else throw new NotFoundException("The dependency between " + fromid + " and " + toid + " does not exist ");
                }
            }
            return result;
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while loading a dependency from the database",organizationId);
        }
    }

    @Override
    public List<Dependency> getDependencies(String organizationId) throws InternalErrorException {
        try (Connection conn = getConnection(organizationId)) {
            return loadDependencies(conn);
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while loading all the dependencies of an organization",organizationId);
        }
    }

    @Override
    public List<Dependency> getDependenciesByStatus(String organizationId, String status, boolean useAuxiliaryTable) throws InternalErrorException {
        List<Dependency> result = new ArrayList<>();
        try (Connection conn = getConnection(organizationId)) {
            String sql = "SELECT fromid, toid FROM dependencies WHERE status = ?";
            if (useAuxiliaryTable) sql = "SELECT fromid, toid FROM aux_dependencies WHERE status = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String fromid = rs.getString("fromid");
                        String toid = rs.getString("toid");
                        result.add(new Dependency(fromid,toid));
                    }
                }
            }
            return result;
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while loading the dependencies by status",organizationId);
        }
    }

    @Override
    public List<Dependency> getReqDependencies(String organizationId, String requirementId, String status, boolean useAuxiliaryTable) throws InternalErrorException {
        List<Dependency> result = new ArrayList<>();
        try (Connection conn = getConnection(organizationId)) {
            String sql = "SELECT toid, score FROM dependencies WHERE fromid = ? AND status = ?";
            if (useAuxiliaryTable) sql = "SELECT toid, score FROM aux_dependencies WHERE fromid = ? AND status = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, requirementId);
                ps.setString(2, status);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String toid = rs.getString("toid");
                        double score = rs.getDouble("score");
                        result.add(new Dependency(requirementId,toid,status,score,-1));
                    }
                }
            }
            return result;
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while loading the dependecies of a requirement",organizationId);
        }
    }

    @Override
    public List<Dependency> getClusterDependencies(String organizationId, int clusterId, boolean useAuxiliaryTable) throws InternalErrorException {
        List<Dependency> result = new ArrayList<>();
        try (Connection conn = getConnection(organizationId)) {
            String sql = "SELECT fromid, toid FROM dependencies WHERE clusterId = ? AND status = ?";
            if (useAuxiliaryTable) sql = "SELECT fromid, toid FROM aux_dependencies WHERE clusterId = ? AND status = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, clusterId);
                ps.setString(2, "accepted");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String fromid = rs.getString("fromid");
                        String toid = rs.getString("toid");
                        result.add(new Dependency(fromid,toid));
                    }
                }
            }
            return result;
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while loading the dependencies of a cluster",organizationId);
        }
    }

    @Override
    public void createDepsAuxiliaryTable(String organizationId) throws InternalErrorException {

        String sql1 = "DROP TABLE IF EXISTS aux_dependencies;";

        String sql2 = "CREATE TABLE aux_dependencies (\n"
                + "	fromid varchar, \n"
                + " toid varchar, \n"
                + " status varchar, \n"
                + " score double, \n"
                + " clusterId integer, \n"
                + " PRIMARY KEY(fromid, toid)"
                + ");";

        String sql3 = "INSERT INTO aux_dependencies SELECT * FROM dependencies;";

        try (Connection conn = getConnection(organizationId);
             Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(false);
            stmt.execute(sql1);
            stmt.execute(sql2);
            stmt.execute(sql3);
            conn.commit();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while creating a dependencies auxiliary table",organizationId);
        }

    }

    @Override
    public void saveDependencyOrReplace(String organizationId, Dependency dependency, boolean useAuxiliaryTable) throws InternalErrorException {
        String sql1 = "INSERT OR REPLACE INTO dependencies(fromid, toid, status, score, clusterId) VALUES (?,?,?,?,?)";
        if (useAuxiliaryTable) sql1 = "INSERT OR REPLACE INTO aux_dependencies(fromid, toid, status, score, clusterId) VALUES (?,?,?,?,?)";
        try (Connection conn = getConnection(organizationId);
             PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setString(1, dependency.getFromid());
            ps.setString(2, dependency.getToid());
            ps.setString(3, dependency.getStatus());
            ps.setDouble(4, dependency.getDependencyScore());
            ps.setInt(5, dependency.getClusterId());
            ps.execute();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while saving or replacing dependencies",organizationId);
        }
    }

    @Override
    public void saveDependencies(String organizationId, List<Dependency> dependencies, boolean useAuxiliaryTable) throws InternalErrorException {
        try (Connection conn = getConnection(organizationId)) {
            conn.setAutoCommit(false);
            saveDependencies(dependencies, conn, useAuxiliaryTable);
            conn.commit();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while saving dependencies",organizationId);
        }
    }

    @Override
    public void updateDependencyStatus(String organizationId, String fromid, String toid, String newStatus, int newClusterId, boolean useAuxiliaryTable) throws InternalErrorException {
        String sql1 = "UPDATE dependencies SET status = ?, clusterId = ? WHERE ((fromid = ? AND toid = ?) OR (fromid = ? AND toid = ?))";
        if (useAuxiliaryTable) sql1 = "UPDATE aux_dependencies SET status = ?, clusterId = ? WHERE ((fromid = ? AND toid = ?) OR (fromid = ? AND toid = ?))";
        try (Connection conn = getConnection(organizationId);
             PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setString(1, newStatus);
            ps.setInt(2, newClusterId);
            ps.setString(3, fromid);
            ps.setString(4, toid);
            ps.setString(5, toid);
            ps.setString(6, fromid);
            ps.executeUpdate();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while updating dependencies by status", organizationId);
        }
    }

    @Override
    public void updateClusterDependencies(String organizationId, int oldClusterId, int newClusterId, boolean useAuxiliaryTable) throws InternalErrorException {
        String sql1 = "UPDATE dependencies SET clusterId = ? WHERE clusterId = ?";
        if (useAuxiliaryTable) sql1 = "UPDATE aux_dependencies SET clusterId = ? WHERE clusterId = ?";
        try (Connection conn = getConnection(organizationId);
             PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setInt(1,newClusterId);
            ps.setInt(2, oldClusterId);
            ps.executeUpdate();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while updating cluster dependencies from an old cluster", organizationId);
        }
    }

    @Override
    public void updateClusterDependencies(String organizationId, String requirementId, int newClusterId, boolean useAuxiliaryTable) throws InternalErrorException {
        String sql1 = "UPDATE dependencies SET clusterId = ? WHERE status = ? AND (fromid = ? OR toid = ?)";
        if (useAuxiliaryTable) sql1 = "UPDATE aux_dependencies SET clusterId = ? WHERE status = ? AND (fromid = ? OR toid = ?)";
        try (Connection conn = getConnection(organizationId);
             PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setInt(1,newClusterId);
            ps.setString(2, "accepted");
            ps.setString(3, requirementId);
            ps.setString(4, requirementId);
            ps.executeUpdate();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while updating cluster dependencies",organizationId);
        }
    }

    @Override
    public void updateClustersAndDependencies(String organization, OrganizationModels organizationModels, List<Dependency> dependencies, boolean useDepsAuxiliaryTable) throws InternalErrorException {
        try (Connection conn = getConnection(organization)) {
            conn.setAutoCommit(false);
            clearClusterTables(conn);
            if (organizationModels.hasClusters()) {
                updateOrganizationClustersInfo(organization,organizationModels.getLastClusterId(),conn);
                saveClusters(organizationModels.getClusters(), conn);
                if (!useDepsAuxiliaryTable) saveDependencies(dependencies, conn, false);
                else insertAuxiliaryDepsTable(conn);
            }
            conn.commit();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while updating clusters and dependencies",organization);
        }
    }

    @Override
    public void deleteReqDependencies(String organizationId, String reqId, boolean useAuxiliaryTable) throws InternalErrorException {
        String sql1 = "DELETE FROM dependencies WHERE (fromid = ? OR toid = ?)";
        if (useAuxiliaryTable) sql1 = "DELETE FROM aux_dependencies WHERE (fromid = ? OR toid = ?)";
        try (Connection conn = getConnection(organizationId);
             PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setString(1,reqId);
            ps.setString(2, reqId);
            ps.executeUpdate();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while deleting requirement dependencies",organizationId);
        }
    }

    @Override
    public void deleteProposedClusterDependencies(String organizationId, int clusterId, boolean useAuxiliaryTable) throws InternalErrorException {
        String sql1 = "DELETE FROM dependencies WHERE clusterId = ? AND status = ?";
        if (useAuxiliaryTable) sql1 = "DELETE FROM aux_dependencies WHERE clusterId = ? AND status = ?";
        try (Connection conn = getConnection(organizationId);
             PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setInt(1, clusterId);
            ps.setString(2, "proposed");
            ps.executeUpdate();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while deleting proposed cluster dependencies",organizationId);
        }
    }


    /*
    Public methods
    Responses methods
     */

    @Override
    public String getResponsePage(String organizationId, String responseId) throws NotFoundException, NotFinishedException, InternalErrorException {
        String sql1 = "SELECT actualPage, maxPages, finished FROM responses WHERE organizationId = ? AND responseId = ?";
        String result;
        try (Connection conn = getConnection(dbMainName)) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql1)) {
                ps.setString(1, organizationId);
                ps.setString(2, responseId);

                int actualPage;
                int maxPages;
                int finished;
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        actualPage = rs.getInt("actualPage");
                        maxPages = rs.getInt("maxPages");
                        finished = rs.getInt("finished");
                    } else throw new NotFoundException("The organization " + organizationId + " has not a response with id " + responseId);
                }

                if (finished == 0) throw new NotFinishedException("The computation is not finished yet");
                else {
                    if (actualPage == maxPages) {
                        deleteResponse(organizationId, responseId, conn);
                        result = "{}";
                    } else {
                        result = getResponsePage(organizationId, responseId, actualPage, conn);
                    }
                }
            }
            conn.commit();
            return result;
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while loading response page",organizationId);
        }
    }

    @Override
    public void saveResponse(String organizationId, String responseId, String methodName) throws InternalErrorException {
        String sql1 = "INSERT INTO responses(organizationId, responseId, actualPage, maxPages, finished, startTime, finalTime, methodName) VALUES (?,?,?,?,?,?,?,?)";
        getAccessToMainDb();
        try (Connection conn = getConnection(dbMainName);
             PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setString(1,organizationId);
            ps.setString(2,responseId);
            ps.setInt(3,0);
            ps.setInt(4,0);
            ps.setInt(5,0);
            ps.setLong(6, getCurrentTime());
            ps.setLong(7, getCurrentTime());
            ps.setString(8, methodName);
            ps.execute();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while saving a response",organizationId);
        } finally {
            releaseAccessToMainDb();
        }
    }

    @Override
    public void saveResponsePage(String organizationId, String responseId, String jsonResponse) throws NotFoundException, InternalErrorException {
        getAccessToMainDb();
        try (Connection conn = getConnection(dbMainName)) {
            conn.setAutoCommit(false);
            int page = getTotalPages(organizationId, responseId, conn);
            insertResponsePage(organizationId,responseId,page,jsonResponse,conn);
            conn.commit();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while saving response page",organizationId);
        } finally {
            releaseAccessToMainDb();
        }
    }

    @Override
    public void finishComputation(String organizationId, String responseId) throws InternalErrorException {
        String sql1 = "UPDATE responses SET finished = ?, finalTime = ? WHERE organizationId = ? AND responseId = ?";
        getAccessToMainDb();
        try (Connection conn = getConnection(dbMainName);
             PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setInt(1,1);
            ps.setLong(2, getCurrentTime());
            ps.setString(3,organizationId);
            ps.setString(4,responseId);
            ps.executeUpdate();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while finishing computation in database",organizationId);
        } finally {
            releaseAccessToMainDb();
        }
    }

    @Override
    public void saveExceptionAndFinishComputation(String organizationId, String responseId, String jsonResponse) throws InternalErrorException {
        getAccessToMainDb();
        try (Connection conn = getConnection(dbMainName)) {
            conn.setAutoCommit(false);
            deleteAllResponsePages(organizationId, responseId, conn);
            insertResponsePage(organizationId, responseId, 0, jsonResponse, conn);
            String sql = "UPDATE responses SET finished = ?, finalTime = ? WHERE organizationId = ? AND responseId = ?";
            try(PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1,1);
                ps.setLong(2, getCurrentTime());
                ps.setString(3,organizationId);
                ps.setString(4,responseId);
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while saving exception",organizationId);
        } finally {
            releaseAccessToMainDb();
        }
    }

    @Override
    public void deleteOrganizationResponses(String organizationId) throws NotFoundException, InternalErrorException {
        String sql1 = "SELECT responseId FROM responses WHERE organizationId = ? AND finished = ?";
        getAccessToMainDb();
        try (Connection conn = getConnection(dbMainName)) {
            conn.setAutoCommit(false);
            if (!existsOrganization(organizationId)) throw new NotFoundException("The organization " + organizationId + " does not exist");
            try (PreparedStatement ps = conn.prepareStatement(sql1)) {
                ps.setString(1, organizationId);
                ps.setInt(2, 1);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String responseId = rs.getString("responseId");
                        deleteAllResponsePages(organizationId, responseId, conn);
                        deleteResponse(organizationId,responseId,conn);
                    }
                }
            }
            conn.commit();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while deleting organization responses",organizationId);
        } finally {
            releaseAccessToMainDb();
        }
    }

    @Override
    public void deleteOldResponses(long borderTime) throws InternalErrorException {
        String sql1 = "SELECT organizationId, responseId FROM responses WHERE finalTime < ? AND finished = ?";
        getAccessToMainDb();
        try (Connection conn = getConnection(dbMainName)) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql1)) {
                ps.setLong(1, borderTime);
                ps.setInt(2, 1);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String organizationId = rs.getString("organizationId");
                        String responseId = rs.getString("responseId");
                        deleteAllResponsePages(organizationId, responseId, conn);
                        deleteResponse(organizationId,responseId,conn);
                    }
                }
            }
            conn.commit();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while deleting old responses","");
        } finally {
            releaseAccessToMainDb();
        }
    }


    /*
    Public methods
    Auxiliary methods
     */

    @Override
    public void deleteOrganization(String organizationId) throws NotFoundException,InternalErrorException {
        if (!existsOrganization(organizationId)) throw new NotFoundException("The organization " + organizationId + " does not exist");
        getAccessToMainDb();
        if (!existsOrganization(organizationId)) throw new NotFoundException("The organization " + organizationId + " does not exist"); //concurrency check
        try (Connection conn = getConnection(dbMainName);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM organizations WHERE id = ?")) {
            deleteDataFiles(buildFileName(organizationId));
            ps.setString(1, organizationId);
            ps.executeUpdate();
        } catch (IOException | SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while deleting an organization",organizationId);
        } finally {
            releaseAccessToMainDb();
        }
    }

    @Override
    public void clearDatabase() throws InternalErrorException {
        resetMainDatabase();
    }


    /*
    Private methods
     */

    private InternalErrorException treatSQLException(String sqlMessage, String exceptionMessage, String organizationId) {
        logger.showErrorMessage(sqlMessage + " " + organizationId);
        return new InternalErrorException("Database error: " + exceptionMessage);
    }

    private void clearClusterTables(Connection conn) throws SQLException {

        String sql1 = "DELETE FROM clusters";
        String sql2 = "DELETE FROM dependencies";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
        }

    }

    private void saveOrganizationInfo(String organization, OrganizationModels organizationModels, Connection conn) throws InternalErrorException, SQLException {

        double threshold = organizationModels.getThreshold();
        boolean compare = organizationModels.isCompare();
        boolean withClusters = organizationModels.hasClusters();
        int lastClusterId = organizationModels.getLastClusterId();
        SimilarityModel similarityModel = organizationModels.getSimilarityModel();

        String sql = "INSERT INTO info(id, threshold, compare, hasClusters, lastClusterId) VALUES (?,?,?,?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,organization);
            ps.setDouble(2, threshold);
            int value = 0;
            if (compare) value = 1;
            ps.setInt(3, value);
            value = 0;
            if (withClusters) value = 1;
            ps.setInt(4, value);
            ps.setInt(5,lastClusterId);
            ps.execute();
        }

        if (withClusters) {
            saveClusters(organizationModels.getClusters(), conn);
            saveDependencies(organizationModels.getDependencies(), conn, false);
        }

        similarityModelDatabase.saveModelInfo(similarityModel,conn);
    }


    private String buildDbUrl(String organization) {
        String driversName = "jdbc:sqlite:";
        return driversName + dbPath + buildFileName(organization);
    }

    private String buildFileName(String organization) {
        return organization + ".db";
    }

    private void configureOrganizationDatabase(String organization) throws SQLException {
        String sql = "PRAGMA journal_mode=WAL;";
        try (Connection conn = getConnection(organization);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            boolean correct = true;
            if (rs.next()) {
                String response = rs.getString(1);
                if (!response.equals("wal")) correct = false;
            } else correct = false;
            if (!correct) throw  new SQLException("Error when setting wal-mode");
        }
    }

    private void deleteDataFiles(String text) throws IOException, InternalErrorException {
        Path dirPath = Paths.get(dbPath);
        class Control {
            private volatile boolean error = false;
        }
        final Control control = new Control();
        try (Stream<Path> walk = Files.walk(dirPath)) {
            walk.map(Path::toFile)
                    .forEach(file -> {
                                if (!file.isDirectory() && file.getName().contains(text)) {
                                    if(!file.delete()) control.error = true;
                                }
                            }
                    );
        }
        if (control.error) throw new InternalErrorException("Error while deleting a file");
    }

    private void createOrganizationFiles(String organization) throws IOException, InternalErrorException {
        File file = new File(dbPath + buildFileName(organization));
        if(!file.createNewFile()) throw new InternalErrorException("Error while creating a new file");
    }

    private void insertNewOrganization(String organization) throws InternalErrorException {
        try {
            if (!existsOrganization(organization)) {
                createOrganizationFiles(organization);
                configureOrganizationDatabase(organization);
                try (Connection conn = getConnection(organization)) {
                    createOrganizationTables(conn);
                }
                insertOrganization(organization);
            }
        } catch (IOException | SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while inserting a new organization",organization);
        }
    }

    private Connection getConnection(String organization) throws SQLException {
        return DriverManager.getConnection(buildDbUrl(organization));
    }

    private void resetMainDatabase() throws InternalErrorException {

        getAccessToMainDb();

        try {
            deleteDataFiles(".db");
            createOrganizationFiles(dbMainName);

            String sql1 = "CREATE TABLE organizations (\n"
                    + "	id varchar PRIMARY KEY\n"
                    + ");";

            String sql2 = "CREATE TABLE responses (\n"
                    + "	organizationId varchar, \n"
                    + " responseId varchar, \n"
                    + " actualPage integer, \n"
                    + " maxPages integer, \n"
                    + " finished integer, \n"
                    + " startTime long, \n"
                    + " finalTIme long, \n"
                    + " methodName varchar, \n"
                    + " PRIMARY KEY(organizationId, responseId)"
                    + ");";

            String sql3 = "CREATE TABLE responsePages (\n"
                    + "	organizationId varchar, \n"
                    + " responseId varchar, \n"
                    + " page integer, \n"
                    + " jsonResponse text, \n"
                    + " FOREIGN KEY(organizationId, responseId) REFERENCES responses(organizationId, responseId), \n"
                    + " PRIMARY KEY(organizationId, responseId, page)"
                    + ");";

            try (Connection conn = getConnection(dbMainName);
                 Statement stmt = conn.createStatement()) {
                conn.setAutoCommit(false);
                stmt.execute(sql1);
                stmt.execute(sql2);
                stmt.execute(sql3);
                conn.commit();
            }
            configureOrganizationDatabase(dbMainName);
        } catch (IOException | SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while reseting database","");
        } finally {
            releaseAccessToMainDb();
        }
    }

    private int getTotalPages(String organizationId, String responseId, Connection conn) throws SQLException, NotFoundException {

        String sql = "SELECT maxPages FROM responses WHERE organizationId = ? AND responseId = ?";
        int totalPages;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, organizationId);
            ps.setString(2, responseId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalPages = rs.getInt(1);
                } else throw new NotFoundException("The organization " + organizationId + " has not a response with id " + responseId);
            }
        }

        return totalPages;
    }

    private void insertAuxiliaryDepsTable(Connection conn) throws SQLException {
        String sql1 = "INSERT INTO dependencies SELECT * FROM aux_dependencies;";
        String sql2 = "DROP TABLE aux_dependencies;";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
        }
    }

    private void saveDependencies(List<Dependency> dependencies, Connection conn, boolean useAuxiliaryTable) throws SQLException {
        String sqlProposed = "INSERT OR IGNORE INTO dependencies(fromid,toid,status,score,clusterId) VALUES (?,?,?,?,?)";
        String sqlAccepted = "INSERT OR REPLACE INTO dependencies(fromid,toid,status,score,clusterId) VALUES (?,?,?,?,?)";
        if (useAuxiliaryTable) {
            sqlProposed = "INSERT OR IGNORE INTO aux_dependencies(fromid,toid,status,score,clusterId) VALUES (?,?,?,?,?)";
            sqlAccepted = "INSERT OR REPLACE INTO aux_dependencies(fromid,toid,status,score,clusterId) VALUES (?,?,?,?,?)";
        }
        for (Dependency dependency : dependencies) {
            String sql = (dependency.getStatus().equals("proposed")) ? sqlProposed : sqlAccepted;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, dependency.getFromid());
                ps.setString(2, dependency.getToid());
                ps.setString(3, dependency.getStatus());
                ps.setDouble(4, dependency.getDependencyScore());
                ps.setInt(5, dependency.getClusterId());
                ps.execute();
            }
        }
    }

    private void deleteAllResponsePages(String organizationId, String responseId, Connection conn) throws SQLException {
        String sql = "DELETE FROM responsePages WHERE organizationId = ? AND responseId = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,organizationId);
            ps.setString(2,responseId);
            ps.execute();
        }
    }

    private void deleteResponse(String organizationId, String responseId, Connection conn) throws SQLException {
        String sql = "DELETE FROM responses WHERE organizationId = ? AND responseId = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,organizationId);
            ps.setString(2,responseId);
            ps.executeUpdate();
        }
    }

    private OrganizationModels getOrganizationInfo(String organizationId, Connection conn) throws SQLException {
        OrganizationModels result = new OrganizationModels();
        String sql = "SELECT threshold, compare, hasClusters, lastClusterId FROM info WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, organizationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.setThreshold(rs.getDouble(1));
                    result.setCompare(rs.getBoolean(2));
                    result.setHasCluster(rs.getBoolean(3));
                    result.setLastClusterId(rs.getInt(4));
                }
            }
        }
        return result;
    }

    private void updateMaxPages(String organizationId, String responseId, int maxPages, Connection conn) throws SQLException {
        String sql1 = "UPDATE responses SET maxPages = ? WHERE organizationId = ? AND responseID = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setInt(1,maxPages);
            ps.setString(2,organizationId);
            ps.setString(3,responseId);
            ps.executeUpdate();
        }
    }

    private void insertResponsePage(String organizationId, String responseId, int page, String jsonResponse, Connection conn) throws SQLException {
        String sql1 = "INSERT INTO responsePages(organizationId, responseId, page, jsonResponse) VALUES (?,?,?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setString(1, organizationId);
            ps.setString(2, responseId);
            ps.setInt(3, page);
            ps.setString(4, jsonResponse);
            ps.execute();
        }

        updateMaxPages(organizationId, responseId, page+1, conn);
    }

    private String getResponsePage(String organizationId, String responseId, int page, Connection conn) throws SQLException {
        String sql1 = "SELECT jsonResponse FROM responsePages WHERE organizationId = ? AND responseId = ? AND page = ?";
        String sql2 = "DELETE FROM responsePages WHERE organizationId = ? AND responseId = ? AND page = ?";
        String sql3 = "UPDATE responses SET actualPage = ? WHERE organizationId = ? AND responseId = ?";

        String result = null;

        try (PreparedStatement ps = conn.prepareStatement(sql1)){
            ps.setString(1,organizationId);
            ps.setString(2,responseId);
            ps.setInt(3,page);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result = rs.getString("jsonResponse");
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(sql2)) {
            ps.setString(1,organizationId);
            ps.setString(2, responseId);
            ps.setInt(3,page);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(sql3)) {
            ps.setInt(1,page+1);
            ps.setString(2,organizationId);
            ps.setString(3, responseId);
            ps.executeUpdate();
        }

        return result;
    }

    private void createOrganizationTables(Connection conn) throws InternalErrorException, SQLException {

        String sql1 = "CREATE TABLE info (\n"
                + " id varchar PRIMARY KEY, \n"
                + " threshold double, \n"
                + " compare integer, \n"
                + " hasClusters integer, \n"
                + " lastClusterId integer"
                + ");";

        String sql2 = "CREATE TABLE clusters (\n"
                + " id integer PRIMARY KEY, \n"
                + " definition text"
                + ");";

        String sql3 = "CREATE TABLE dependencies (\n"
                + "	fromid varchar, \n"
                + " toid varchar, \n"
                + " status varchar, \n"
                + " score double, \n"
                + " clusterId integer, \n"
                + " PRIMARY KEY(fromid, toid)"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
            stmt.execute(sql3);
        }

        similarityModelDatabase.createModelTables(conn);
    }

    private void clearOrganizationTables(Connection conn) throws InternalErrorException, SQLException {

        String sql1 = "DELETE FROM info";
        String sql2 = "DELETE FROM clusters";
        String sql3 = "DELETE FROM dependencies";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
            stmt.execute(sql3);
        }

        similarityModelDatabase.clearModelTables(conn);
    }

    private void insertOrganization(String organization) throws SQLException, InternalErrorException {

        String sql = "INSERT INTO organizations(id) VALUES (?)";

        getAccessToMainDb();
        try (Connection conn = getConnection(dbMainName);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,organization);
            ps.execute();
        } finally {
            releaseAccessToMainDb();
        }

    }

    private void updateOrganizationClustersInfo(String organizationId, int lastClusterId, Connection conn) throws SQLException {

        String sql = "UPDATE info SET lastClusterId = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, lastClusterId);
            ps.setString(2,organizationId);
            ps.executeUpdate();
        }
    }

    private void saveClusters(Map<Integer, List<String>> clusters, Connection conn) throws SQLException {

        for (Map.Entry<Integer, List<String>> entry : clusters.entrySet()) {
            int key = entry.getKey();
            List<String> requirements = entry.getValue();
            String sql = "INSERT INTO clusters(id, definition) VALUES (?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, key);
                ps.setString(2, requirementsArrayConversionToJson(requirements).toString());
                ps.execute();
            }
        }
    }

    private JSONArray requirementsArrayConversionToJson(List<String> requirements) {
        JSONArray jsonArray = new JSONArray();
        for (String requirement: requirements) {
            jsonArray.put(requirement);
        }
        return jsonArray;
    }

    private Map<Integer, List<String>> loadClusters(Connection conn) throws SQLException {

        Map<Integer, List<String>> result = new HashMap<>();

        String sql = "SELECT* FROM clusters";

        try (Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){

            while (rs.next()) {
                int key = rs.getInt("id");
                String definition = rs.getString("definition");
                result.put(key,requirementsArrayConversionToMap(definition));
            }
        }

        return result;
    }

    private List<String> requirementsArrayConversionToMap(String text) {

        List<String> result = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(text);
        for (int i = 0; i < jsonArray.length(); ++i) {
            result.add(jsonArray.getString(i));
        }
        return result;
    }

    private List<Dependency> loadDependencies(Connection conn) throws SQLException {

        List<Dependency> dependencies = new ArrayList<>();

        String sql = "SELECT* FROM dependencies";

        try (Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){

            while (rs.next()) {
                String fromid = rs.getString("fromid");
                String toid = rs.getString("toid");
                String status = rs.getString("status");
                double score = rs.getDouble("score");
                int clusterId = rs.getInt("clusterId");
                dependencies.add(new Dependency(fromid,toid,status,score,clusterId));
            }
        }

        return dependencies;
    }

    private long getCurrentTime() {
        return Time.getInstance().getCurrentMillis();
    }
}