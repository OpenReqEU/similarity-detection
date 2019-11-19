package upc.similarity.compareapi.dao.algorithm_models_dao.clusters_algorithm.max_graph;

import org.json.JSONArray;
import upc.similarity.compareapi.algorithms.clusters_algorithm.ClustersModel;
import upc.similarity.compareapi.algorithms.clusters_algorithm.max_graph.ClustersModelMaxGraph;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.dao.SQLiteDatabase;
import upc.similarity.compareapi.dao.algorithm_models_dao.clusters_algorithm.ClustersModelDatabase;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.entity.exception.InternalErrorException;
import upc.similarity.compareapi.entity.exception.NotFoundException;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClustersModelDatabaseMaxGraph implements ClustersModelDatabase {

    /*
    Override methods
     */

    @Override
    public void createModelTables(Connection conn) throws SQLException {

        String sql1 = "CREATE TABLE clusters_info (\n"
                + " id integer PRIMARY KEY, \n"
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
    }

    @Override
    public void clearModelTables(Connection conn) throws SQLException {

        String sql1 = "DELETE FROM clusters_info";
        String sql2 = "DELETE FROM clusters";
        String sql3 = "DELETE FROM dependencies";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
            stmt.execute(sql3);
        }
    }

    @Override
    public void saveModelInfo(ClustersModel clustersModel, Connection conn) throws InternalErrorException, SQLException {
        try {
            ClustersModelMaxGraph clustersModelMaxGraph = (ClustersModelMaxGraph) clustersModel;

            String sql = "INSERT INTO clusters_info(id, lastClusterId) VALUES (?,?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1,1);
                ps.setInt(2,clustersModelMaxGraph.getLastClusterId());
                ps.execute();
            }

            saveClusters(clustersModelMaxGraph.getClusters(), conn);
            saveDependencies(clustersModelMaxGraph.getDependencies(), conn, false);
        } catch (ClassCastException e) {
            throw new InternalErrorException("A max graph method received a model that is not max graph");
        }
    }

    @Override
    public ClustersModel getModel(Connection conn) throws InternalErrorException, SQLException {
        int lastClusterId = 0;

        String sql = "SELECT lastClusterId FROM clusters_info WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, 1);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lastClusterId = rs.getInt(1);
                } else throw new InternalErrorException("Error loading clusters from database");
            }
        }

        Map<Integer, List<String>> clusters = loadClusters(conn);
        return new ClustersModelMaxGraph(lastClusterId,clusters);
    }


    /*
    Exclusive max_graph methods
     */

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

    public List<Dependency> getDependencies(String organizationId) throws InternalErrorException {
        try (Connection conn = getConnection(organizationId)) {
            return loadDependencies(conn);
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while loading all the dependencies of an organization",organizationId);
        }
    }

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

    public void saveDependencies(String organizationId, List<Dependency> dependencies, boolean useAuxiliaryTable) throws InternalErrorException {
        try (Connection conn = getConnection(organizationId)) {
            conn.setAutoCommit(false);
            saveDependencies(dependencies, conn, useAuxiliaryTable);
            conn.commit();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while saving dependencies",organizationId);
        }
    }

    public void deleteDependencies(String organizationId, String fromid, String toid, boolean useAuxiliaryTable) throws InternalErrorException {
        String sql1 = "DELETE FROM dependencies WHERE ((fromid = ? AND toid = ?) OR (fromid = ? AND toid = ?))";
        if (useAuxiliaryTable) sql1 = "DELETE FROM  aux_dependencies WHERE ((fromid = ? AND toid = ?) OR (fromid = ? AND toid = ?))";
        try (Connection conn = getConnection(organizationId);
             PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setString(1, fromid);
            ps.setString(2, toid);
            ps.setString(3, toid);
            ps.setString(4, fromid);
            ps.executeUpdate();
        } catch (SQLException sql) {
            throw treatSQLException(sql.getMessage(),"Error while deleting dependencies", organizationId);
        }
    }

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

    public void updateClustersAndDependencies(String organization, OrganizationModels organizationModels, List<Dependency> dependencies, boolean useDepsAuxiliaryTable) throws InternalErrorException {
        try {
            ClustersModelMaxGraph clustersModelMaxGraph = (ClustersModelMaxGraph) organizationModels.getClustersModel();
            try (Connection conn = getConnection(organization)) {
                conn.setAutoCommit(false);
                clearModelTables(conn);
                if (organizationModels.hasClusters()) {
                    updateOrganizationClustersInfo(clustersModelMaxGraph.getLastClusterId(), conn);
                    saveClusters(clustersModelMaxGraph.getClusters(), conn);
                    if (!useDepsAuxiliaryTable) saveDependencies(dependencies, conn, false);
                    else insertAuxiliaryDepsTable(conn);
                }
                conn.commit();
            } catch (SQLException sql) {
                throw treatSQLException(sql.getMessage(), "Error while updating clusters and dependencies", organization);
            }
        } catch (ClassCastException e) {
            throw new InternalErrorException("A max_graph method received a model that is not max_graph");
        }
    }

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
    Private methods
     */

    private InternalErrorException treatSQLException(String message, String s, String organization) {
        SQLiteDatabase sqLiteDatabase = (SQLiteDatabase) Constants.getInstance().getDatabaseModel();
        return sqLiteDatabase.treatSQLException(message,s,organization);
    }

    private Connection getConnection(String organization) throws SQLException {
        SQLiteDatabase sqLiteDatabase = (SQLiteDatabase) Constants.getInstance().getDatabaseModel();
        return sqLiteDatabase.getConnection(organization);
    }

    private void insertAuxiliaryDepsTable(Connection conn) throws SQLException {
        String sql1 = "INSERT INTO dependencies SELECT * FROM aux_dependencies;";
        String sql2 = "DROP TABLE aux_dependencies;";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
        }
    }

    private void updateOrganizationClustersInfo(int lastClusterId, Connection conn) throws SQLException {

        String sql = "INSERT INTO clusters_info(id, lastClusterId) VALUES (?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,1);
            ps.setInt(2,lastClusterId);
            ps.execute();
        }
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
}
