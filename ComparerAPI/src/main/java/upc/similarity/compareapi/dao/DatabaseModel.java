package upc.similarity.compareapi.dao;

import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Model;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.exception.NotFoundException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public interface DatabaseModel {


    /*
    Main operations
     */

    boolean existsOrganization(String organizationId) throws SQLException;

    Model getModel(String organizationId, boolean withFrequency) throws NotFoundException, SQLException;

    void saveModel(String organizationId, Model model, List<Dependency> dependencies) throws IOException, InternalErrorException, SQLException;


    /*
    Cluster operations
     */

    void updateClustersAndDependencies(String organization, Model model, List<Dependency> dependencies, boolean useDepsAuxiliaryTable) throws SQLException;

    void createDepsAuxiliaryTable(String organizationId) throws SQLException;

    void saveDependencyOrReplace(String organizationId, Dependency dependency, boolean useAuxiliaryTable) throws SQLException;

    void saveDependencies(String organizationId, List<Dependency> dependencies, boolean useAuxiliaryTable) throws SQLException;

    Dependency getDependency(String fromid, String toid, String organizationId, boolean useAuxiliaryTable) throws SQLException, NotFoundException;

    List<Dependency> getClusterDependencies(String organizationId, int clusterId, boolean useAuxiliaryTable) throws SQLException;

    void deleteProposedClusterDependencies(String organizationId, int clusterId, boolean useAuxiliaryTable) throws SQLException;

    List<Dependency> getRejectedDependencies(String organizationId, boolean useAuxiliaryTable) throws SQLException;

    List<Dependency> getReqDependencies(String organizationId, String requirementId, String status, boolean useAuxiliaryTable) throws SQLException;

    List<Dependency> getDependencies(String organizationId) throws SQLException;

    void updateDependencyStatus(String organizationId, String fromid, String toid, String newStatus, int newClusterId, boolean useAuxiliaryTable) throws SQLException;

    void updateClusterDependencies(String organizationId, int oldClusterId, int newClusterId, boolean useAuxiliaryTable) throws SQLException;

    void updateClusterDependencies(String organizationId, String requirementId, int newClusterId, boolean useAuxiliaryTable) throws SQLException;

    void deleteReqDependencies(String organizationId, String reqId, boolean useAuxiliaryTable) throws SQLException;

    List<Dependency> getNotInDependencies(String organizationId, Set<String> dependencies, boolean useAuxiliaryTable) throws SQLException;


    /*
    Auxiliary operations
     */

    void saveResponse(String organizationId, String responseId) throws SQLException, InternalErrorException;

    void saveResponsePage(String organizationId, String responseId, String jsonResponse) throws SQLException, NotFoundException, InternalErrorException;

    void saveException(String organizationId, String responseId, String jsonResponse) throws SQLException, InternalErrorException;

    void finishComputation(String organizationId, String responseId) throws SQLException, InternalErrorException;

    String getResponsePage(String organizationId, String responseId) throws SQLException, NotFoundException, NotFinishedException;

    void clearOrganizationResponses(String organizationId) throws SQLException, NotFoundException, InternalErrorException;

    void clearOldResponses(long borderTime) throws InternalErrorException, SQLException;

    void clearOrganization(String organizationId) throws NotFoundException, SQLException, InternalErrorException, IOException;

    void clearDatabase() throws IOException, InternalErrorException, SQLException;


}
