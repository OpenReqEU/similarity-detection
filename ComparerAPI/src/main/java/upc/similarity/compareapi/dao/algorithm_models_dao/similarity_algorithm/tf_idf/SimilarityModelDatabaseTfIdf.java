package upc.similarity.compareapi.dao.algorithm_models_dao.similarity_algorithm.tf_idf;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.compareapi.dao.algorithm_models_dao.similarity_algorithm.SimilarityModelDatabase;
import upc.similarity.compareapi.entity.exception.InternalErrorException;
import upc.similarity.compareapi.algorithms.similarity_algorithm.SimilarityModel;
import upc.similarity.compareapi.algorithms.similarity_algorithm.tf_idf.SimilarityModelTfIdf;

import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SimilarityModelDatabaseTfIdf implements SimilarityModelDatabase {

    @Override
    public void createModelTables(Connection conn) throws SQLException {

        String sql1 = "CREATE TABLE docs (\n"
                + " id varchar PRIMARY KEY, \n"
                + " definition text \n"
                + ");";

        String sql2 = "CREATE TABLE corpus (\n"
                + " definition text \n"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
        }
    }

    @Override
    public void clearModelTables(Connection conn) throws SQLException {

        String sql1 = "DELETE FROM docs";
        String sql2 = "DELETE FROM corpus";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql1);
            stmt.execute(sql2);
        }
    }

    @Override
    public void saveModelInfo(SimilarityModel similarityModel, Connection conn) throws InternalErrorException, SQLException {
        try {
            SimilarityModelTfIdf similarityModelTfIdf = (SimilarityModelTfIdf) similarityModel;
            saveDocs(similarityModelTfIdf.getDocs(), conn);
            saveCorpusFrequency(similarityModelTfIdf.getCorpusFrequency(), conn);
        } catch (ClassCastException e) {
            throw new InternalErrorException("A tfIdf method received a model that is not tfIdf");
        }
    }

    @Override
    public SimilarityModel getModel(boolean readOnly, Connection conn) throws SQLException {
        Map<String,Map<String,Double>> docs = loadDocs(conn);
        Map<String,Integer> corpusFrequency = null;
        if (!readOnly) corpusFrequency = loadCorpusFrequency(conn);
        return new SimilarityModelTfIdf(docs,corpusFrequency);
    }

    @Override
    public boolean existsReqInsideModel(String requirement, Connection conn) throws SQLException {
        String sql = "SELECT id FROM docs WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requirement);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }


    /*
    Private methods
     */

    private void saveDocs(Map<String, Map<String, Double>> docs, Connection conn) throws SQLException {
        for (Map.Entry<String, Map<String, Double>> entry : docs.entrySet()) {
            String key = entry.getKey();
            Map<String, Double> words = entry.getValue();
            String sql = "INSERT INTO docs(id, definition) VALUES (?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, key);
                ps.setString(2, wordsConversionToJson(words).toString());
                ps.execute();
            }
        }
    }

    private JSONArray wordsConversionToJson(Map<String, Double> map) {
        JSONArray result = new JSONArray();
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            JSONObject aux = new JSONObject();
            aux.put("id", entry.getKey());
            aux.put("value", entry.getValue());
            result.put(aux);
        }
        return result;
    }

    private void saveCorpusFrequency(Map<String, Integer> corpusFrequency, Connection conn) throws SQLException {
        String sql = "INSERT INTO corpus(definition) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,corpusFrequencyToJson(corpusFrequency));
            ps.execute();
        }
    }

    private String corpusFrequencyToJson(Map<String, Integer> corpusFrequency) {
        JSONArray result = new JSONArray();
        for (Map.Entry<String, Integer> entry : corpusFrequency.entrySet()) {
            JSONObject aux = new JSONObject();
            aux.put("id", entry.getKey());
            aux.put("value", entry.getValue());
            result.put(aux);
        }
        return result.toString();
    }

    private Map<String, Map<String, Double>> loadDocs(Connection conn) throws SQLException {
        Map<String, Map<String, Double>> result = new HashMap<>();
        String sql = "SELECT* FROM docs";
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

    private Map<String, Integer> loadCorpusFrequency(Connection conn) throws SQLException {
        Map<String, Integer> result;
        String sql = "SELECT* FROM corpus";
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
}
