package upc.similarity.compareapi.dao;

import upc.similarity.compareapi.entity.Model;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.exception.NotFoundException;

import java.io.IOException;
import java.sql.SQLException;

public interface DatabaseModel {

    void saveModel(String organization, Model model) throws InternalErrorException, IOException, SQLException;

    Model getModel(String organization, boolean withFrequency) throws SQLException, NotFoundException;

    void saveResponse(String organizationId, String responseId) throws SQLException;

    void saveResponsePage(String organizationId, String responseId, String jsonResponse) throws SQLException, NotFoundException;

    void saveException(String organizationId, String responseId, String jsonResponse) throws SQLException;

    void finishComputation(String organizationId, String responseId) throws SQLException;

    String getResponsePage(String organizationId, String responseId) throws SQLException, NotFoundException, NotFinishedException;

    //public int getTotalPages(String organizationId, String responseId) throws SQLException, NotFoundException;

    //List<Dependency> getResponsePage(String organizationId, String responseId, int pageNumber) throws SQLException, NotFoundException;

    /*void saveDependency(Dependency dependency) throws SQLException;

    Dependency getDependency(String fromid, String toid, String organizationId) throws SQLException, NotFoundException;

    List<Dependency> getClusterDependencies(String organizationId, int clusterId) throws SQLException;

    boolean existsDependency(String fromid, String toid, String organizationId) throws SQLException;

    void updateDependency(String fromid, String toid, String organizationId, String newStatus, int newCluster) throws SQLException, NotFoundException;

    void updateClusterDependencies(String organizationId, int oldClusterId, int newClusterId) throws SQLException;

    void updateClusterDependencies(String organizationId, String requirementId, String status, int newClusterId) throws SQLException;

    void deleteReqDependencies(String reqId, String organizationId) throws SQLException;*/

    void clearOrganizationResponses(String organization) throws SQLException, NotFoundException;

    void clearOrganization(String organization) throws NotFoundException, InternalErrorException, SQLException;

    void clearDatabase() throws IOException, SQLException;


}
