package upc.similarity.semilarapi.dao;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.semilarapi.entity.Model;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.NotFinishedException;

import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SQLiteDAO implements modelDAO {

    private static String db_url = "jdbc:sqlite:../models.db";

    public SQLiteDAO() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
    }

    @Override
    public void saveModel(String organization, Model model) throws SQLException {

        boolean found = true;
        try (Connection conn = DriverManager.getConnection(db_url);
             PreparedStatement ps = conn.prepareStatement("SELECT (count(*) > 0) as found FROM organizations WHERE id = ?")) {
            ps.setString(1, organization);

            try (ResultSet rs = ps.executeQuery()) {
                // Only expecting a single result
                if (rs.next()) {
                    found = rs.getBoolean(1); // "found" column
                }
            }
        }
        if (found) {
            deleteOrganizationTables(organization);
        } else {
            insertOrganization(organization);
        }
        try (Connection conn = DriverManager.getConnection(db_url)) {
            createOrganizationTables(organization, conn);
            saveDocs(organization, model.getDocs(), conn);
            saveCorpusFrequency(organization, model.getCorpusFrequency(), conn);
        }
    }

    @Override
    public Model getModel(String organization) throws SQLException, BadRequestException {

        Map<String, Map<String, Double>> docs = null;
        Map<String, Integer> corpusFrequency = null;

        try (Connection conn = DriverManager.getConnection(db_url)) {
            existsOrganization(organization,conn);
            docs = loadDocs(organization,conn);
            corpusFrequency = loadCorpusFrequency(organization,conn);
        }

        return new Model(docs,corpusFrequency);
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
    public String getResponsePage(String organizationId, String responseId) throws SQLException, BadRequestException, NotFinishedException {
        String sql = "SELECT actualPage, maxPages, finished FROM responses WHERE organizationId = ? AND responseId = ?";

        String result = null;

        try (Connection conn = DriverManager.getConnection(db_url)) {

            existsOrganization(organizationId,conn);

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
                    } else throw new BadRequestException("The organization " + organizationId + " has not a response with id " + responseId);
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
    public void finishComputation(String organizationId, String responseId) throws SQLException {
        String sql = "UPDATE responses SET finished = ? WHERE organizationId = ? AND responseId = ?";

        try (Connection conn = DriverManager.getConnection(db_url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1,1);
            ps.setString(2,organizationId);
            ps.setString(3,responseId);
            ps.executeUpdate();
        }
    }


    @Override
    public void clearDB(String organization) throws SQLException {

        try(Connection conn = DriverManager.getConnection(db_url);
            PreparedStatement ps = conn.prepareStatement("DELETE FROM models WHERE id = ?")) {
            ps.setString(1, organization);
            ps.execute();
        }

        deleteOrganizationTables(organization);
    }

    /*
    auxiliary operations
     */

    private void createDatabase() {

        String sql1 = "CREATE TABLE IF NOT EXISTS organizations (\n"
                + "	id varchar PRIMARY KEY"
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


        try (Connection conn = DriverManager.getConnection(db_url);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql1);
            stmt.execute(sql2);
            stmt.execute(sql3);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
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

    private void existsOrganization(String organizationId, Connection conn) throws BadRequestException, SQLException {

        try (PreparedStatement ps = conn.prepareStatement("SELECT (count(*) > 0) as found FROM organizations WHERE id = ?")) {
            ps.setString(1, organizationId);

            try (ResultSet rs = ps.executeQuery()) {
                // Only expecting a single result
                if (rs.next()) {
                    boolean found = rs.getBoolean(1); // "found" column
                    if (!found) throw new BadRequestException("The organization " + organizationId + " does not exist");
                }
            }
        }
    }


    private void insertResponse(String organizationId, String responseId) throws SQLException {
        String sql = "INSERT INTO responses(organizationId, responseId, actualPage, maxPages, finished) VALUES (?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(db_url);
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

        try (Connection conn = DriverManager.getConnection(db_url)) {

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

    private void createOrganizationTables(String organization, Connection conn) throws SQLException {

        String sql1 = "CREATE TABLE docs_"+organization+" (\n"
                + " id varchar PRIMARY KEY, \n"
                + " definition text \n"
                + ");";

        String sql2 = "CREATE TABLE corpus_"+organization+" (\n"
                + " definition text \n"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
        }
    }

    private void deleteOrganizationTables(String organization) throws SQLException {

        String sql1 = "DROP TABLE docs_"+organization;
        String sql2 = "DROP TABLE corpus_"+organization;

        try (Connection conn = DriverManager.getConnection(db_url);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
        }

    }

    private void insertOrganization(String organization) throws SQLException {

        String sql = "INSERT INTO organizations(id) VALUES (?)";

        try (Connection conn = DriverManager.getConnection(db_url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,organization);
            ps.execute();
        }

    }

    private void saveDocs(String organization, Map<String, Map<String, Double>> docs, Connection conn) throws SQLException {

        Iterator it = docs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String key = (String) pair.getKey();
            Map<String, Double> words = ( Map<String, Double>) pair.getValue();
            String sql = "INSERT INTO docs_"+organization+"(id, definition) VALUES (?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1,key);
                ps.setString(2,words_conversion_toJSON(words).toString());
                ps.execute();
            }
            it.remove();
        }
    }

    private JSONArray words_conversion_toJSON(Map<String, Double> map) {
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
            ps.setString(1,corpusFrequency_toJSON(corpusFrequency));
            ps.execute();
        }
    }

    private String corpusFrequency_toJSON(Map<String, Integer> corpusFrequency) {
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

    private Map<String, Map<String, Double>> loadDocs(String organization, Connection conn) throws SQLException {

        Map<String, Map<String, Double>> result = new HashMap<>();

        String sql = "SELECT* FROM docs_"+organization;

        try (Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){

            while (rs.next()) {
                String key = rs.getString("id");
                String definition = rs.getString("definition");
                result.put(key,docs_conversion_toMap(definition));
            }
        }

        return result;
    }

    private Map<String, Double> docs_conversion_toMap(String rawJson) {
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

        Map<String, Integer> result = new HashMap<>();

        String sql = "SELECT* FROM corpus_"+organization;

        try (Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){

            if (rs.next()) {
                String corpus = rs.getString("definition");
                result = corpus_toMap(corpus);
            } else throw new SQLException("Error loading corpus");
        }

        return result;
    }

    private Map<String, Integer> corpus_toMap(String corpus) {
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
}
