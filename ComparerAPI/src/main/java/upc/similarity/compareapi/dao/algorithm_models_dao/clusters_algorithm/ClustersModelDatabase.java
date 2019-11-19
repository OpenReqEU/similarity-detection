package upc.similarity.compareapi.dao.algorithm_models_dao.clusters_algorithm;

import upc.similarity.compareapi.algorithms.clusters_algorithm.ClustersModel;
import upc.similarity.compareapi.entity.exception.InternalErrorException;

import java.sql.Connection;
import java.sql.SQLException;

public interface ClustersModelDatabase {

    /**
     * Creates the database tables used to save the clusters algorithm data structures
     * @param conn the connection used to connect to the database (mandatory to use this one, creating a different one may create concurrency problems)
     * @throws SQLException when some sql exception is thrown
     */
    void createModelTables(Connection conn) throws InternalErrorException, SQLException;

    /**
     * Deletes the tables used to save the clusters algorithm data structures
     * @param conn the connection used to connect to the database (mandatory to use this one, creating a different one may create concurrency problems)
     * @throws SQLException when some sql exception is thrown
     */
    void clearModelTables(Connection conn) throws InternalErrorException, SQLException;

    /**
     * Saves the memory clusters model to the created database tables
     * @param clustersModel the corresponding similarity model to be saved
     * @param conn the connection used to connect to the database (mandatory to use this one, creating a different one may create concurrency problems)
     * @throws SQLException when some sql exception is thrown
     */
    void saveModelInfo(ClustersModel clustersModel, Connection conn) throws InternalErrorException, SQLException;

    /**
     * Loads the clusters model from the database tables
     * @param conn the connection used to connect to the database (mandatory to use this one, creating a different one may create concurrency problems)
     * @return the clusters model
     * @throws SQLException when some sql exception is thrown
     */
    ClustersModel getModel(Connection conn) throws InternalErrorException, SQLException;
}
