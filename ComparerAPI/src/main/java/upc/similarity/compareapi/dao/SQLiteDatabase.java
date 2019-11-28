package upc.similarity.compareapi.dao;

import upc.similarity.compareapi.algorithms.clusters_algorithm.ClustersModel;
import upc.similarity.compareapi.dao.algorithm_models_dao.clusters_algorithm.ClustersModelDatabase;
import upc.similarity.compareapi.dao.algorithm_models_dao.similarity_algorithm.SimilarityModelDatabase;
import upc.similarity.compareapi.entity.*;
import upc.similarity.compareapi.algorithms.similarity_algorithm.SimilarityModel;
import upc.similarity.compareapi.util.Logger;
import upc.similarity.compareapi.entity.exception.InternalErrorException;
import upc.similarity.compareapi.entity.exception.NotFinishedException;
import upc.similarity.compareapi.entity.exception.NotFoundException;
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
    private ClustersModelDatabase clustersModelDatabase;
    private Logger logger = Logger.getInstance();

    public SQLiteDatabase(String dbPath, int sleepTime, SimilarityModelDatabase similarityModelDatabase, ClustersModelDatabase clustersModelDatabase) throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        this.dbPath = dbPath;
        this.sleepTime = sleepTime;
        this.similarityModelDatabase = similarityModelDatabase;
        this.clustersModelDatabase = clustersModelDatabase;
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
                if (organizationModels.hasClusters()) organizationModels.setClustersModel(clustersModelDatabase.getModel(conn));
                conn.commit();
            }
            return organizationModels;
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while loading organization models",organization);
        }
    }

    @Override
    public void saveOrganizationModels(String organization, OrganizationModels organizationModels, boolean saveSimilarityModel, boolean saveClustersModel) throws InternalErrorException {
        try {
            insertNewOrganization(organization);
            try (Connection conn = getConnection(organization)) {
                conn.setAutoCommit(false);
                clearOrganizationTables(conn);
                saveOrganizationInfo(organization, organizationModels, saveSimilarityModel, saveClustersModel, conn);
                conn.commit();
            }
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while saving the models of an organization",organization);
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

    public InternalErrorException treatSQLException(String sqlMessage, String exceptionMessage, String organizationId) {
        logger.showErrorMessage(sqlMessage + " " + organizationId);
        return new InternalErrorException("Database error: " + exceptionMessage);
    }

    private void saveOrganizationInfo(String organization, OrganizationModels organizationModels, boolean saveSimilarityModel, boolean saveClustersModel, Connection conn) throws InternalErrorException, SQLException {

        double threshold = organizationModels.getThreshold();
        boolean compare = organizationModels.isCompare();
        boolean useComponent = organizationModels.isUseComponent();
        boolean withClusters = organizationModels.hasClusters();
        SimilarityModel similarityModel = organizationModels.getSimilarityModel();
        ClustersModel clustersModel = organizationModels.getClustersModel();

        String sql = "INSERT INTO info(id, threshold, compare, useComponent, hasClusters) VALUES (?,?,?,?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,organization);
            ps.setDouble(2, threshold);
            int value = 0;
            if (compare) value = 1;
            ps.setInt(3, value);
            value = 0;
            if (useComponent) value = 1;
            ps.setInt(4, value);
            value = 0;
            if (withClusters) value = 1;
            ps.setInt(5, value);
            ps.execute();
        }
        saveRequirementsInfo(organizationModels.getReqComponent(),conn);

        if (saveSimilarityModel) similarityModelDatabase.saveModelInfo(similarityModel,conn);
        if (withClusters && saveClustersModel) clustersModelDatabase.saveModelInfo(clustersModel,conn);
    }

    private void saveRequirementsInfo(Map<String, String> reqComponent, Connection conn) throws SQLException {
        for (Map.Entry<String, String> entry : reqComponent.entrySet()) {
            String key = entry.getKey();
            String component = entry.getValue();
            String sql = "INSERT INTO requirements_info(id, component) VALUES (?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, key);
                ps.setString(2, component);
                ps.execute();
            }
        }
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

    public Connection getConnection(String organization) throws SQLException {
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
        String sql = "SELECT threshold, compare, useComponent, hasClusters FROM info WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, organizationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.setThreshold(rs.getDouble(1));
                    result.setCompare(rs.getBoolean(2));
                    result.setUseComponent(rs.getBoolean(3));
                    result.setHasCluster(rs.getBoolean(4));
                }
            }
        }
        result.setReqComponent(loadReqComponent(conn));
        return result;
    }

    private Map<String, String> loadReqComponent(Connection conn) throws SQLException {
        Map<String, String> result = new HashMap<>();
        String sql = "SELECT* FROM requirements_info";
        try (Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){
            while (rs.next()) {
                String key = rs.getString("id");
                String component = rs.getString("component");
                result.put(key,component);
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
                + " useComponent integer, \n"
                + " hasClusters integer"
                + ");";

        String sql2 = "CREATE TABLE requirements_info (\n"
                + " id varchar PRIMARY KEY, \n"
                + " component varchar \n"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
        }

        similarityModelDatabase.createModelTables(conn);
        clustersModelDatabase.createModelTables(conn);
    }

    private void clearOrganizationTables(Connection conn) throws InternalErrorException, SQLException {

        String sql1 = "DELETE FROM info";
        String sql2 = "DELETE FROM requirements_info";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
        }

        similarityModelDatabase.clearModelTables(conn);
        clustersModelDatabase.clearModelTables(conn);
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

    private long getCurrentTime() {
        return Time.getInstance().getCurrentMillis();
    }
}