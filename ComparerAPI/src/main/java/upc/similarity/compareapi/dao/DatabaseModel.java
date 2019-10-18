package upc.similarity.compareapi.dao;

import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Organization;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.exception.NotFoundException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface DatabaseModel {


    /*
    Main operations
     */

    boolean existsOrganization(String organizationId) throws SQLException;

    boolean existReqInOrganizationModel(String organizationId, String requirement) throws NotFoundException, SQLException;

    Organization getOrganizationInfo(String organizationId) throws NotFoundException, SQLException;

    OrganizationModels getOrganizationModels(String organizationId, boolean withFrequency) throws NotFoundException, SQLException;

    void saveOrganizationModels(String organizationId, OrganizationModels organizationModels) throws IOException, InternalErrorException, SQLException;


    /*
    Cluster operations
     */

    Dependency getDependency(String fromid, String toid, String organizationId, boolean useAuxiliaryTable) throws SQLException, NotFoundException;

    List<Dependency> getDependencies(String organizationId) throws SQLException;

    List<Dependency> getDependenciesByStatus(String organizationId, String status, boolean useAuxiliaryTable) throws SQLException;

    List<Dependency> getReqDependencies(String organizationId, String requirementId, String status, boolean useAuxiliaryTable) throws SQLException;

    List<Dependency> getClusterDependencies(String organizationId, int clusterId, boolean useAuxiliaryTable) throws SQLException;

    void createDepsAuxiliaryTable(String organizationId) throws SQLException;

    void saveDependencyOrReplace(String organizationId, Dependency dependency, boolean useAuxiliaryTable) throws SQLException;

    void saveDependencies(String organizationId, List<Dependency> dependencies, boolean useAuxiliaryTable) throws SQLException;

    void updateDependencyStatus(String organizationId, String fromid, String toid, String newStatus, int newClusterId, boolean useAuxiliaryTable) throws SQLException;

    void updateClusterDependencies(String organizationId, int oldClusterId, int newClusterId, boolean useAuxiliaryTable) throws SQLException;

    void updateClusterDependencies(String organizationId, String requirementId, int newClusterId, boolean useAuxiliaryTable) throws SQLException;

    void updateClustersAndDependencies(String organization, OrganizationModels organizationModels, List<Dependency> dependencies, boolean useDepsAuxiliaryTable) throws SQLException;

    void deleteReqDependencies(String organizationId, String reqId, boolean useAuxiliaryTable) throws SQLException;

    void deleteProposedClusterDependencies(String organizationId, int clusterId, boolean useAuxiliaryTable) throws SQLException;


    /*
    Responses operations
     */

    String getResponsePage(String organizationId, String responseId) throws SQLException, NotFoundException, NotFinishedException;

    void saveResponse(String organizationId, String responseId, String methodName) throws SQLException, InternalErrorException;

    void saveResponsePage(String organizationId, String responseId, String jsonResponse) throws SQLException, NotFoundException, InternalErrorException;

    void finishComputation(String organizationId, String responseId) throws SQLException, InternalErrorException;

    void saveExceptionAndFinishComputation(String organizationId, String responseId, String jsonResponse) throws SQLException, InternalErrorException;

    void clearOrganizationResponses(String organizationId) throws SQLException, NotFoundException, InternalErrorException;

    void clearOldResponses(long borderTime) throws InternalErrorException, SQLException;

    /*
    Auxiliary operations
     */

    void clearOrganization(String organizationId) throws NotFoundException, SQLException, InternalErrorException, IOException;

    void clearDatabase() throws IOException, InternalErrorException, SQLException;


}
