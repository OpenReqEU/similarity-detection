package upc.similarity.compareapi.dao;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Model;
import upc.similarity.compareapi.entity.ReqClusterInfo;
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
                + " hasClusters integer"
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


        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
            stmt.execute(sql3);
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
            insertOrganization(organization,model.hasClusters());
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
                saveReqCluster(organization, model.getReqCluster(), conn);
            }
            conn.commit();
        }
    }

    @Override
    public Model getModel(String organization) throws SQLException, NotFoundException {

        Map<String, Map<String, Double>> docs = null;
        Map<String, Integer> corpusFrequency = null;
        Map<String, List<String>> clusters = null;
        Map<String, ReqClusterInfo> reqCluster = null;

        boolean hasClusters;

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            hasClusters = getOrganization(organization,conn);
            docs = loadDocs(organization,conn);
            corpusFrequency = loadCorpusFrequency(organization,conn);
            if (hasClusters) {
                clusters = loadClusters(organization, conn);
                reqCluster = loadReqCluster(organization, conn);
            }
        }

        return (hasClusters) ? new Model(docs, corpusFrequency, clusters, reqCluster) : new Model(docs, corpusFrequency);
    }

    @Override
    public void saveResponse(String organizationId, String responseId) throws SQLException {
        insertResponse(organizationId,responseId);
    }

    @Override
    public void saveResponsePage(String organizationId, String responseId, int page, String jsonResponse) throws SQLException {
        insertResponsePage(organizationId,responseId,page,jsonResponse);
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
    public int getTotalPages(String organizationId, String responseId) throws SQLException, NotFoundException {

        String sql = "SELECT maxPages FROM responses WHERE organizationId = ? AND responseId = ?";
        int totalPages;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {

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
            getOrganization(organization,conn);
            try (PreparedStatement ps = conn.prepareStatement(sql1)) {
                ps.setString(1, organization);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String responseId = rs.getString("responseId");
                        int maxPages = rs.getInt("maxPages");
                        int finished = rs.getInt("finished");
                        if (finished == 1) {
                            deleteResponsePages(organization,responseId,maxPages,conn);
                            deleteResponse(organization,responseId,conn);
                        }
                    }
                }
            }
        }
    }

    /*
    Auxiliary operations
     */

    private void deleteResponsePages(String organizationId, String responseId, int maxPages, Connection conn) throws SQLException {
        for (int i = 0; i < maxPages; ++i) {
            deleteResponsePage(organizationId,responseId,i,conn);
        }
    }

    private void deleteResponsePage(String organizationId, String responseId, int page, Connection conn) throws SQLException {
        String sql = "DELETE FROM responsePages WHERE organizationId = ? AND responseId = ? AND page = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,organizationId);
            ps.setString(2,responseId);
            ps.setInt(3,page);
            ps.executeUpdate();
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

    private boolean getOrganization(String organizationId, Connection conn) throws NotFoundException, SQLException {

        boolean hasClusters;
        try (PreparedStatement ps = conn.prepareStatement("SELECT hasClusters FROM organizations WHERE id = ?")) {
            ps.setString(1, organizationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    hasClusters = rs.getBoolean(1);
                } else throw new NotFoundException("The organization " + organizationId + " does not exist");
            }
        }
        return hasClusters;
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

    private void insertResponsePage(String organizationId, String responseId, int page, String jsonResponse) throws SQLException {
        String sql1 = "INSERT INTO responsePages(organizationId, responseId, page, jsonResponse) VALUES (?,?,?,?)";
        String sql3 = "UPDATE responses SET maxPages = ? WHERE organizationId = ? AND responseID = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl)) {

            try (PreparedStatement ps = conn.prepareStatement(sql1)) {
                ps.setString(1, organizationId);
                ps.setString(2, responseId);
                ps.setInt(3, page);
                ps.setString(4, jsonResponse);
                ps.execute();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql3)) {
                ps.setInt(1,page+1);
                ps.setString(2,organizationId);
                ps.setString(3,responseId);
                ps.executeUpdate();
            }
        }
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
                + " id varchar PRIMARY KEY, \n"
                + " definition text"
                + ");";

        String sql4 = "CREATE TABLE reqCluster_"+organization+" (\n"
                + " id varchar PRIMARY KEY, \n"
                + " definition text \n"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
            if (hasClusters) {
                stmt.execute(sql3);
                stmt.execute(sql4);
            }
        }
    }

    private void deleteOrganizationTables(String organization) throws SQLException {

        String sql1 = "DROP TABLE docs_"+organization;
        String sql2 = "DROP TABLE corpus_"+organization;
        String sql3 = "DROP TABLE IF EXISTS clusters_"+organization;
        String sql4 = "DROP TABLE IF EXISTS reqCluster_"+organization;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
            stmt.execute(sql3);
            stmt.execute(sql4);
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

    private void insertOrganization(String organization, boolean hasClusters) throws SQLException {

        String sql = "INSERT INTO organizations(id, hasClusters) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,organization);
            int value = 0;
            if (hasClusters) value = 1;
            ps.setInt(2, value);
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

    private void saveClusters(String organization, Map<String, List<String>> clusters, Connection conn) throws SQLException {

        Iterator it = clusters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String key = (String) pair.getKey();
            List<String> requirements = (List<String>) pair.getValue();
            String sql = "INSERT INTO clusters_"+organization+"(id, definition) VALUES (?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1,key);
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

    private void saveReqCluster(String organization, Map<String, ReqClusterInfo> reqCluster, Connection conn) throws SQLException {

        Iterator it = reqCluster.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String key = (String) pair.getKey();
            ReqClusterInfo reqClusterInfo = (ReqClusterInfo) pair.getValue();
            String sql = "INSERT INTO reqCluster_"+organization+"(id, definition) VALUES (?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1,key);
                ps.setString(2,reqClusterInfoConversionToJson(reqClusterInfo).toString());
                ps.execute();
            }
            it.remove();
        }
    }

    private JSONObject reqClusterInfoConversionToJson(ReqClusterInfo reqCluster) {
       JSONObject jsonObject = new JSONObject();
       jsonObject.put("cluster", reqCluster.getCluster());
       jsonObject.put("date", reqCluster.getDate());
       return jsonObject;
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

    private Map<String, List<String>> loadClusters(String organization, Connection conn) throws SQLException {

        Map<String, List<String>> result = new HashMap<>();

        String sql = "SELECT* FROM clusters_"+organization;

        try (Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){

            while (rs.next()) {
                String key = rs.getString("id");
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

    private Map<String, ReqClusterInfo> loadReqCluster(String organization, Connection conn) throws SQLException {

        Map<String, ReqClusterInfo> result = new HashMap<>();

        String sql = "SELECT* FROM reqCluster_"+organization;

        try (Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){

            while (rs.next()) {
                String key = rs.getString("id");
                String definition = rs.getString("definition");
                result.put(key,reqClusterInfoConversionToMap(definition));
            }
        }

        return result;
    }

    private ReqClusterInfo reqClusterInfoConversionToMap(String text) {

        JSONObject jsonObject = new JSONObject(text);
        String cluster = jsonObject.getString("cluster");
        long date = jsonObject.getLong("date");

        return new ReqClusterInfo(cluster, date);
    }
}
