package upc.similarity.compareapi.dao;

import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Organization;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.exception.NotFoundException;
import java.util.List;

/**
 * Must be concurrent secure. The main service can create the next situations in different cases so the class that implements this interface must deal with them:
 *      - r , r -> two methods that only read data are called at the same time (from the same organization or different ones)
 *      - r , w -> one method that writes or updates data is called at the same time with another one that only reads data (from the same organization or different ones)
 *      - w , w -> two methods that write or update data from different organizations are called at the same time (only when they are from different organizations)
 * The main service will never called at the same time two methods that write or update data from the same organization
 */
public interface DatabaseModel {


    /*
    Main operations
     */

    boolean existsOrganization(String organizationId) throws InternalErrorException;

    boolean existReqInOrganizationModel(String organizationId, String requirement) throws NotFoundException, InternalErrorException;

    Organization getOrganizationInfo(String organizationId) throws NotFoundException, InternalErrorException;

    OrganizationModels getOrganizationModels(String organizationId, boolean readOnly) throws NotFoundException, InternalErrorException;

    void saveOrganizationModels(String organizationId, OrganizationModels organizationModels) throws InternalErrorException;


    /*
    Cluster operations
     */

    Dependency getDependency(String organizationId, String fromid, String toid, boolean useAuxiliaryTable) throws NotFoundException, InternalErrorException;

    List<Dependency> getDependenciesByStatus(String organizationId, String status, boolean useAuxiliaryTable) throws InternalErrorException;

    List<Dependency> getReqDependencies(String organizationId, String requirementId, String status, boolean useAuxiliaryTable) throws InternalErrorException;

    List<Dependency> getClusterDependencies(String organizationId, int clusterId, boolean useAuxiliaryTable) throws InternalErrorException;

    void createDepsAuxiliaryTable(String organizationId) throws InternalErrorException;

    void saveDependencyOrReplace(String organizationId, Dependency dependency, boolean useAuxiliaryTable) throws InternalErrorException;

    void saveDependencies(String organizationId, List<Dependency> dependencies, boolean useAuxiliaryTable) throws InternalErrorException;

    void updateDependencyStatus(String organizationId, String fromid, String toid, String newStatus, int newClusterId, boolean useAuxiliaryTable) throws InternalErrorException;

    void updateClusterDependencies(String organizationId, int oldClusterId, int newClusterId, boolean useAuxiliaryTable) throws InternalErrorException;

    void updateClusterDependencies(String organizationId, String requirementId, int newClusterId, boolean useAuxiliaryTable) throws InternalErrorException;

    void updateClustersAndDependencies(String organization, OrganizationModels organizationModels, List<Dependency> dependencies, boolean useDepsAuxiliaryTable) throws InternalErrorException;

    void deleteReqDependencies(String organizationId, String reqId, boolean useAuxiliaryTable) throws InternalErrorException;

    void deleteProposedClusterDependencies(String organizationId, int clusterId, boolean useAuxiliaryTable) throws InternalErrorException;


    /*
    Responses operations
     */

    String getResponsePage(String organizationId, String responseId) throws NotFoundException, NotFinishedException, InternalErrorException;

    void saveResponse(String organizationId, String responseId, String methodName) throws InternalErrorException;

    void saveResponsePage(String organizationId, String responseId, String jsonResponse) throws NotFoundException, InternalErrorException;

    void finishComputation(String organizationId, String responseId) throws InternalErrorException;

    void saveExceptionAndFinishComputation(String organizationId, String responseId, String jsonResponse) throws InternalErrorException;

    void deleteOrganizationResponses(String organizationId) throws NotFoundException, InternalErrorException;

    void deleteOldResponses(long borderTime) throws InternalErrorException;


    /*
    Auxiliary operations
     */

    void deleteOrganization(String organizationId) throws NotFoundException, InternalErrorException;

    void clearDatabase() throws InternalErrorException;


    /*
    Test purpose methods
     */

    List<Dependency> getDependencies(String organizationId) throws InternalErrorException;


}
