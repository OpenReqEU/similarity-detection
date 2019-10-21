package upc.similarity.compareapi.dao.algorithm_models_dao.similarity_algorithm;

import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.similarity_algorithm.SimilarityModel;

import java.sql.Connection;
import java.sql.SQLException;

public interface SimilarityModelDatabase{

    /**
     * Creates the database tables used to save the similarity algorithm data structures
     * @param conn the connection used to connect to the database (mandatory to use this one, creating a different one may create concurrency problems)
     * @throws SQLException when some sql exception is thrown
     */
    void createModelTables(Connection conn) throws InternalErrorException, SQLException;

    /**
     * Deletes the tables used to save the similarity algorithm data structures
     * @param conn the connection used to connect to the database (mandatory to use this one, creating a different one may create concurrency problems)
     * @throws SQLException when some sql exception is thrown
     */
    void clearModelTables(Connection conn) throws InternalErrorException, SQLException;

    /**
     * Saves the memory similarity model to the created database tables
     * @param similarityModel the corresponding similarity model to be saved
     * @param conn the connection used to connect to the database (mandatory to use this one, creating a different one may create concurrency problems)
     * @throws SQLException when some sql exception is thrown
     */
    void saveModelInfo(SimilarityModel similarityModel, Connection conn) throws InternalErrorException, SQLException;

    /**
     * Loads the similarity model from the database tables
     * @param readOnly this parameter shows if the method that called this operation is gonna update the model. It is used when there are some data structures
     *                 only needed when updating the model nor when computing the similarity between requirements
     * @param conn the connection used to connect to the database (mandatory to use this one, creating a different one may create concurrency problems)
     * @return the similarity model
     * @throws SQLException when some sql exception is thrown
     */
    SimilarityModel getModel(boolean readOnly, Connection conn) throws InternalErrorException, SQLException;

    /**
     * Checks if the input requirement is inside the model without the need of loading all the model from the database
     * @param requirement the id of the requirement
     * @param conn the connection used to connect to the database (mandatory to use this one, creating a different one may create concurrency problems)
     * @return true if the input requirement is inside the model, false otherwise
     * @throws SQLException when some sql exception is thrown
     */
    boolean existsReqInsideModel(String requirement, Connection conn) throws InternalErrorException, SQLException;


}
