package upc.similarity.compareapi.dao.algorithm_models_dao.similarity_algorithm;

import upc.similarity.compareapi.similarity_algorithm.SimilarityModel;

import java.sql.Connection;
import java.sql.SQLException;

public interface SimilarityModelDatabase{

    void createModelTables(Connection conn) throws SQLException;

    void clearModelTables(Connection conn) throws SQLException;

    void saveModelInfo(SimilarityModel similarityModel, Connection conn) throws SQLException;

    SimilarityModel getModel(boolean readOnly, Connection conn) throws SQLException;

    boolean existsReqInsideModel(String requirement, Connection conn) throws SQLException;


}
