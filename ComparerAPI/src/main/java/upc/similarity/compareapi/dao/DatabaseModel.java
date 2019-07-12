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

    boolean existsOrganization(String organizationId) throws SQLException;

    void saveModel(String organizationId, Model model) throws IOException, InternalErrorException, SQLException;

    void updateClustersAndDependencies(String organization, Model model, boolean useDepsAuxiliaryTable) throws SQLException;

    Model getModel(String organizationId, boolean withFrequency) throws NotFoundException, SQLException;

    void saveResponse(String organizationId, String responseId) throws SQLException;

    void saveResponsePage(String organizationId, String responseId, String jsonResponse) throws SQLException, NotFoundException;

    void saveException(String organizationId, String responseId, String jsonResponse) throws SQLException;

    void finishComputation(String organizationId, String responseId) throws SQLException;

    String getResponsePage(String organizationId, String responseId) throws SQLException, NotFoundException, NotFinishedException;

    //public int getTotalPages(String organizationId, String responseId) throws SQLException, NotFoundException;

    //List<Dependency> getResponsePage(String organizationId, String responseId, int pageNumber) throws SQLException, NotFoundException;

    void createDepsAuxiliaryTable(String organizationId) throws SQLException;

    void saveDependencyOrReplace(String organizationId, Dependency dependency, boolean useAuxiliaryTable) throws SQLException;

    void saveDependencies(String organizationId, List<Dependency> dependencies, boolean useAuxiliaryTable) throws SQLException;

    Dependency getDependency(String fromid, String toid, String organizationId, boolean useAuxiliaryTable) throws SQLException, NotFoundException;

    List<Dependency> getClusterDependencies(String organizationId, int clusterId, boolean useAuxiliaryTable) throws SQLException;

    void deleteProposedClusterDependencies(String organizationId, int clusterId, boolean useAuxiliaryTable) throws SQLException;

    List<Dependency> getRejectedDependencies(String organizationId, boolean useAuxiliaryTable) throws SQLException;

    List<Dependency> getReqDependencies(String organizationId, String requirementId, boolean useAuxiliaryTable) throws SQLException;

    List<Dependency> getDependencies(String organizationId) throws SQLException;

    //boolean existsDependency(String fromid, String toid, String organizationId) throws SQLException;

    void updateDependencyStatus(String organizationId, String fromid, String toid, String newStatus, int newClusterId, boolean useAuxiliaryTable) throws SQLException;

    void updateClusterDependencies(String organizationId, int oldClusterId, int newClusterId, boolean useAuxiliaryTable) throws SQLException;

    void updateClusterDependencies(String organizationId, String requirementId, int newClusterId, boolean useAuxiliaryTable) throws SQLException;

    void deleteReqDependencies(String organizationId, String reqId, boolean useAuxiliaryTable) throws SQLException;

    List<Dependency> getNotInDependencies(String organizationId, Set<String> dependencies, boolean useAuxiliaryTable) throws SQLException;

    void clearOrganizationResponses(String organizationId) throws SQLException, NotFoundException;

    void clearOrganization(String organizationId) throws NotFoundException, SQLException;

    void clearDatabase() throws IOException, InternalErrorException, SQLException;


}
