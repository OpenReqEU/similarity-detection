package upc.similarity.compareapi.dao;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Model;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.exception.NotFoundException;

import java.sql.*;
import java.util.*;

public class SQLiteDatabase implements DatabaseModel {

    private static String dbName = "models.db";
    private static String dbUrl = "jdbc:sqlite:"+dbName;

    public SQLiteDatabase() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
    }

    public static void setDbName(String dbName) {
        SQLiteDatabase.dbName = dbName;
        dbUrl = "jdbc:sqlite:"+dbName;
    }

    public static String getDbName() {
        return dbName;
    }

    @Override
    public void createDatabase() throws SQLException {

        String sql1 = "CREATE TABLE IF NOT EXISTS organizations (\n"
                + "	id varchar PRIMARY KEY, \n"
                + " hasClusters integer, \n"
                + " lastClusterId integer"
                + ");";

        String sql2 = "CREATE TABLE IF NOT EXISTS responses (\n"
                + "	organizationId varchar, \n"
                + " responseId varchar, \n"
                + " actualPage integer, \n"
                + " maxPages integer, \n"
                + " finished integer, \n"
                + " PRIMARY KEY(organizationId, responseId)"
                + ");";

        String sql3 = "CREATE TABLE IF NOT EXISTS responsePages (\n"
                + "	organizationId varchar, \n"
                + " responseId varchar, \n"
                + " page integer, \n"
                + " jsonResponse text, \n"
                + " FOREIGN KEY(organizationId) REFERENCES responses(organizationId), \n"
                + " FOREIGN KEY(responseId) REFERENCES responses(responseId), \n"
                + " PRIMARY KEY(organizationId, responseId, page)"
                + ");";

        String sql4 = "CREATE TABLE IF NOT EXISTS dependencies (\n"
                + "	fromid varchar, \n"
                + " toid varchar, \n"
                + " status varchar, \n"
                + " organizationId varchar, \n"
                + " clusterId integer, \n"
                + " PRIMARY KEY(fromid, toid, organizationId)"
                + ");";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
            stmt.execute(sql3);
            stmt.execute(sql4);
        }
    }

    @Override
    public void saveModel(String organization, Model model) throws SQLException {

        boolean found = true;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement("SELECT (count(*) > 0) as found FROM organizations WHERE id = ?")) {
            ps.setString(1, organization);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    found = rs.getBoolean(1);
                }
            }
        }
        if (found) {
            deleteOrganizationTables(organization);
        } else {
            insertOrganization(organization,model.hasClusters(),model.getLastClusterId());
        }
        if (model.hasClusters()) setClusters(organization,1);
        else setClusters(organization,0);
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            createOrganizationTables(organization, conn, model.hasClusters());
            saveDocs(organization, model.getDocs(), conn);
            saveCorpusFrequency(organization, model.getCorpusFrequency(), conn);
            if (model.hasClusters()) {
                saveClusters(organization, model.getClusters(), conn);
                saveDependencies(model.getDependencies(), conn);
            }
            conn.commit();
        }
    }

    @Override
    public Model getModel(String organization, boolean withFrequency) throws SQLException, NotFoundException {

        Map<String, Map<String, Double>> docs = null;
        Map<String, Integer> corpusFrequency = null;
        Map<Integer, List<String>> clusters = null;

        boolean hasClusters;
        int lastClusterId;

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            Organization aux = getOrganization(organization,conn);
            hasClusters = aux.hasClusters;
            lastClusterId = aux.lastClusterId;
            docs = loadDocs(organization,conn);
            if (withFrequency) corpusFrequency = loadCorpusFrequency(organization,conn);
            if (hasClusters) {
                clusters = loadClusters(organization, conn);
            }
            conn.commit();
        }

        return (hasClusters) ? new Model(docs, corpusFrequency, lastClusterId, clusters, null) : new Model(docs, corpusFrequency);
    }

    @Override
    public void saveDependency(Dependency dependency) throws SQLException {

        String sql = "INSERT INTO dependencies(fromid, toid, status, organizationId, clusterId) VALUES (?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dependency.getFromid());
            ps.setString(2, dependency.getToid());
            ps.setString(3, dependency.getStatus());
            ps.setInt(4, dependency.getClusterId());
            ps.execute();
        }
    }

    @Override
    public void saveResponse(String organizationId, String responseId) throws SQLException {
        insertResponse(organizationId,responseId);
    }

    @Override
    public void saveResponsePage(String organizationId, String responseId, String jsonResponse) throws SQLException, NotFoundException {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            int page = getTotalPages(organizationId, responseId, conn);
            insertResponsePage(organizationId,responseId,page,jsonResponse,conn);
            conn.commit();
        }
    }

    @Override
    public void saveException(String organizationId, String responseId, String jsonResponse) throws SQLException {

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            deleteAllResponsePages(organizationId, responseId, conn);
            insertResponsePage(organizationId, responseId, 0, jsonResponse, conn);
            conn.commit();
        }
    }

    @Override
    public String getResponsePage(String organizationId, String responseId) throws SQLException, NotFoundException, NotFinishedException {
        String sql = "SELECT actualPage, maxPages, finished FROM responses WHERE organizationId = ? AND responseId = ?";

        String result = null;

        try (Connection conn = DriverManager.getConnection(dbUrl)) {

            existsOrganizationResponses(organizationId,conn);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
        }
        return result;
    }

    @Override
    public List<Dependency> getResponsePage(String organizationId, String responseId, int pageNumber) throws SQLException, NotFoundException {

        String jsonResponse;
        List<Dependency> dependencies = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl)) {

            String sql = "SELECT jsonResponse FROM responsePages WHERE organizationId = ? AND responseId = ? AND page = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, organizationId);
                ps.setString(2, responseId);
                ps.setInt(3, pageNumber);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        jsonResponse = rs.getString("jsonResponse");
                    } else throw new NotFoundException("The organization " + organizationId + " has not a response with id " + responseId + " and page " + pageNumber);
                }
            }
        }

        JSONObject jsonObject = new JSONObject(jsonResponse);
        try {
            JSONArray jsonArray = jsonObject.getJSONArray("dependencies");
            for (int i = 0; i < jsonArray.length(); ++i) {
                JSONObject aux = jsonArray.getJSONObject(i);
                String fromid = aux.getString("fromid");
                String toid = aux.getString("toid");
                String type = aux.getString("dependency_type");
                dependencies.add(new Dependency(fromid,toid,type));
            }
        } catch (JSONException e) {
            //empty
        }

        return dependencies;
    }

    @Override
    public Dependency getDependency(String fromid, String toid, String organizationId) throws SQLException, NotFoundException {

        Dependency result = new Dependency(fromid,toid);

        try (Connection conn = DriverManager.getConnection(dbUrl)) {

            String sql = "SELECT status, clusterId FROM dependencies WHERE organizationId = ? AND ((fromid = ? AND toid = ?) OR (fromid = ? AND toid = ?))";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, organizationId);
                ps.setString(2, fromid);
                ps.setString(3, toid);
                ps.setString(4, toid);
                ps.setString(5, fromid);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString(1);
                        int clusterId = rs.getInt(2);
                        result.setStatus(status);
                        result.setClusterId(clusterId);
                    } else throw new NotFoundException("The dependency between " + fromid + " and " + toid + " does not exist ");
                }
            }
        }

        return result;
    }

    public List<Dependency> getClusterDependencies(String organizationId, int clusterId) throws SQLException {

        List<Dependency> result = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl)) {

            String sql = "SELECT fromid, toid FROM dependencies WHERE organizationId = ? AND clusterId = ? AND status = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, organizationId);
                ps.setInt(2, clusterId);
                ps.setString(3, "accepted");

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String fromid = rs.getString("fromid");
                        String toid = rs.getString("toid");
                        result.add(new Dependency(fromid,toid));
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean existsDependency(String fromid, String toid, String organizationId) throws SQLException {

        boolean result = false;

        try (Connection conn = DriverManager.getConnection(dbUrl)) {

            String sql = "SELECT (count(*) > 0) FROM dependencies WHERE organizationId = ? AND ((fromid = ? AND toid = ?) OR (fromid = ? AND toid = ?))";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, organizationId);
                ps.setString(2, fromid);
                ps.setString(3, toid);
                ps.setString(4, toid);
                ps.setString(5, fromid);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) result = true;
                }
            }
        }

        return result;
    }

    @Override
    public void updateDependency(String fromid, String toid, String organizationId, String newStatus, int newCluster) throws SQLException, NotFoundException {
        String sql = "UPDATE dependencies SET status = ?, clusterId = ? WHERE organizationId = ? ((fromid = ? AND toid = ?) OR (fromid = ? AND toid = ?))";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, organizationId);
            ps.setString(2, fromid);
            ps.setString(3, toid);
            ps.setString(4, toid);
            ps.setString(5, fromid);
            ps.executeUpdate();
        }
    }

    @Override
    public void updateClusterDependencies(String organizationId, int oldClusterId, int newClusterId) throws SQLException {
        String sql = "UPDATE dependencies SET clusterId = ? WHERE organizationId = ? AND clusterId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,newClusterId);
            ps.setString(2, organizationId);
            ps.setInt(3, oldClusterId);
            ps.executeUpdate();
        }
    }

    @Override
    public void updateClusterDependencies(String organizationId, String requirementId, String status, int newClusterId) throws SQLException {
        String sql = "UPDATE dependencies SET clusterId = ? WHERE organizationId = ? AND status = ? AND (fromid = ? OR toid = ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,newClusterId);
            ps.setString(2, organizationId);
            ps.setString(3, status);
            ps.setString(4, requirementId);
            ps.setString(5, requirementId);
            ps.executeUpdate();
            conn.commit();
        }
    }

    @Override
    public void deleteReqDependencies(String reqId, String organizationId) throws SQLException {
        String sql = "DELETE FROM dependencies WHERE organizationId = ? AND (fromid = ? OR toid = ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,organizationId);
            ps.setString(2,reqId);
            ps.setString(3, reqId);
            ps.executeUpdate();
        }
    }

    @Override
    public void finishComputation(String organizationId, String responseId) throws SQLException {
        String sql = "UPDATE responses SET finished = ? WHERE organizationId = ? AND responseId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,1);
            ps.setString(2,organizationId);
            ps.setString(3,responseId);
            ps.executeUpdate();
        }
    }

    @Override
    public void clearOrganizationResponses(String organization) throws SQLException, NotFoundException {
        String sql1 = "SELECT responseId, maxPages, finished FROM responses WHERE organizationId = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            getOrganization(organization,conn);
            try (PreparedStatement ps = conn.prepareStatement(sql1)) {
                ps.setString(1, organization);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String responseId = rs.getString("responseId");
                        int maxPages = rs.getInt("maxPages");
                        int finished = rs.getInt("finished");
                        if (finished == 1) {
                            deleteAllResponsePages(organization, responseId, conn);
                            deleteResponse(organization,responseId,conn);
                            conn.commit();
                        }
                    }
                }
            }
        }
    }


    /*
    Auxiliary operations
     */

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

    private void saveDependencies(List<Dependency> dependencies, Connection conn) throws SQLException {
        for (Dependency dependency: dependencies) saveDependency(dependency, conn);
    }

    private void saveDependency(Dependency dependency, Connection conn) throws SQLException {
        String sql = "INSERT INTO dependencies(fromid,toid,status,clusterId) VALUES (?,?,?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,dependency.getFromid());
            ps.setString(2,dependency.getToid());
            ps.setString(3,dependency.getStatus());
            ps.setInt(4,dependency.getClusterId());
            ps.execute();
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

    private class Organization {
        boolean hasClusters;
        int lastClusterId;
    }

    private Organization getOrganization(String organizationId, Connection conn) throws NotFoundException, SQLException {

        Organization result = new Organization();
        try (PreparedStatement ps = conn.prepareStatement("SELECT hasClusters, lastClusterId FROM organizations WHERE id = ?")) {
            ps.setString(1, organizationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.hasClusters = rs.getBoolean(1);
                    result.lastClusterId = rs.getInt(2);
                } else throw new NotFoundException("The organization " + organizationId + " does not exist");
            }
        }
        return result;
    }

    private void existsOrganizationResponses(String organizationId, Connection conn) throws NotFoundException, SQLException {

        try (PreparedStatement ps = conn.prepareStatement("SELECT (count(*) > 0) as found FROM responses WHERE organizationId = ?")) {
            ps.setString(1, organizationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean found = rs.getBoolean(1);
                    if (!found) throw new NotFoundException("The organization " + organizationId + " has no responses");
                }
            }
        }
    }

    private void insertResponse(String organizationId, String responseId) throws SQLException {
        String sql = "INSERT INTO responses(organizationId, responseId, actualPage, maxPages, finished) VALUES (?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,organizationId);
            ps.setString(2,responseId);
            ps.setInt(3,0);
            ps.setInt(4,0);
            ps.setInt(5,0);
            ps.execute();
        }
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

    private void createOrganizationTables(String organization, Connection conn, boolean hasClusters) throws SQLException {

        String sql1 = "CREATE TABLE docs_"+organization+" (\n"
                + " id varchar PRIMARY KEY, \n"
                + " definition text \n"
                + ");";

        String sql2 = "CREATE TABLE corpus_"+organization+" (\n"
                + " definition text \n"
                + ");";

        String sql3 = "CREATE TABLE clusters_"+organization+" (\n"
                + " id integer PRIMARY KEY, \n"
                + " definition text"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
            if (hasClusters) {
                stmt.execute(sql3);
            }
        }
    }

    private void deleteOrganizationTables(String organization) throws SQLException {

        String sql1 = "DROP TABLE docs_"+organization;
        String sql2 = "DROP TABLE corpus_"+organization;
        String sql3 = "DROP TABLE IF EXISTS clusters_"+organization;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
            stmt.execute(sql3);
        }

    }

    private void setClusters(String organization, int value) throws SQLException {

        String sql = "UPDATE organizations SET hasClusters = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,value);
            ps.setString(2, organization);
            ps.execute();
        }

    }

    private void insertOrganization(String organization, boolean hasClusters, int lastClusterId) throws SQLException {

        String sql = "INSERT INTO organizations(id, hasClusters, lastClusterId) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,organization);
            int value = 0;
            if (hasClusters) value = 1;
            ps.setInt(2, value);
            ps.setInt(3, lastClusterId);
            ps.execute();
        }

    }

    private void saveDocs(String organization, Map<String, Map<String, Double>> docs, Connection conn) throws SQLException {

        Iterator it = docs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String key = (String) pair.getKey();
            Map<String, Double> words = (Map<String, Double>) pair.getValue();
            String sql = "INSERT INTO docs_"+organization+"(id, definition) VALUES (?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1,key);
                ps.setString(2,wordsConversionToJson(words).toString());
                ps.execute();
            }
            it.remove();
        }
    }

    private JSONArray wordsConversionToJson(Map<String, Double> map) {
        JSONArray result = new JSONArray();
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            JSONObject aux = new JSONObject();
            aux.put("id",pair.getKey());
            aux.put("value",pair.getValue());
            result.put(aux);
            it.remove();
        }
        return result;
    }

    private void saveCorpusFrequency(String organization, Map<String, Integer> corpusFrequency, Connection conn) throws SQLException {

        String sql = "INSERT INTO corpus_"+organization+"(definition) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,corpusFrequencyToJson(corpusFrequency));
            ps.execute();
        }
    }

    private String corpusFrequencyToJson(Map<String, Integer> corpusFrequency) {
        JSONArray result = new JSONArray();
        Iterator it = corpusFrequency.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            JSONObject aux = new JSONObject();
            aux.put("id",pair.getKey());
            aux.put("value",pair.getValue());
            result.put(aux);
            it.remove();
        }
        return result.toString();
    }

    private void saveClusters(String organization, Map<Integer, List<String>> clusters, Connection conn) throws SQLException {

        Iterator it = clusters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            int key = (int) pair.getKey();
            List<String> requirements = (List<String>) pair.getValue();
            String sql = "INSERT INTO clusters_"+organization+"(id, definition) VALUES (?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1,key);
                ps.setString(2,requirementsArrayConversionToJson(requirements).toString());
                ps.execute();
            }
            it.remove();
        }
    }

    private JSONArray requirementsArrayConversionToJson(List<String> requirements) {
        JSONArray jsonArray = new JSONArray();
        for (String requirement: requirements) {
            jsonArray.put(requirement);
        }
        return jsonArray;
    }

    private Map<String, Map<String, Double>> loadDocs(String organization, Connection conn) throws SQLException {

        Map<String, Map<String, Double>> result = new HashMap<>();

        String sql = "SELECT* FROM docs_"+organization;

        try (Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){

            while (rs.next()) {
                String key = rs.getString("id");
                String definition = rs.getString("definition");
                result.put(key,docsConversionToMap(definition));
            }
        }

        return result;
    }

    private Map<String, Double> docsConversionToMap(String rawJson) {
        JSONArray json = new JSONArray(rawJson);
        Map<String, Double> result = new HashMap<>();
        for (int i = 0; i < json.length(); ++i) {
            JSONObject aux = json.getJSONObject(i);
            String id = aux.getString("id");
            Double value = aux.getDouble("value");
            result.put(id,value);
        }
        return result;
    }

    private Map<String, Integer> loadCorpusFrequency(String organization, Connection conn) throws SQLException {

        Map<String, Integer> result;

        String sql = "SELECT* FROM corpus_"+organization;

        try (Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){

            if (rs.next()) {
                String corpus = rs.getString("definition");
                result = corpusToMap(corpus);
            } else throw new SQLException("Error loading corpus from the database");
        }

        return result;
    }

    private Map<String, Integer> corpusToMap(String corpus) {
        Map<String, Integer> result = new HashMap<>();
        JSONArray json = new JSONArray(corpus);
        for (int i = 0; i < json.length(); ++i) {
            JSONObject aux = json.getJSONObject(i);
            String id = aux.getString("id");
            Integer value = aux.getInt("value");
            result.put(id, value);
        }
        return result;
    }

    private Map<Integer, List<String>> loadClusters(String organization, Connection conn) throws SQLException {

        Map<Integer, List<String>> result = new HashMap<>();

        String sql = "SELECT* FROM clusters_"+organization;

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
}