package upc.similarity.compareapi.dao;

import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Model;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.exception.NotFoundException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface DatabaseModel {

    boolean existsOrganization(String organizationId) throws SQLException;

    void saveModel(String organizationId, Model model) throws IOException, SQLException;

    Model getModel(String organizationId, boolean withFrequency) throws SQLException, NotFoundException;

    void saveResponse(String organizationId, String responseId) throws SQLException;

    void saveResponsePage(String organizationId, String responseId, String jsonResponse) throws SQLException, NotFoundException;

    void saveException(String organizationId, String responseId, String jsonResponse) throws SQLException;

    void finishComputation(String organizationId, String responseId) throws SQLException;

    String getResponsePage(String organizationId, String responseId) throws SQLException, NotFoundException, NotFinishedException;

    //public int getTotalPages(String organizationId, String responseId) throws SQLException, NotFoundException;

    //List<Dependency> getResponsePage(String organizationId, String responseId, int pageNumber) throws SQLException, NotFoundException;

    /*void saveDependency(Dependency dependency) throws SQLException;

    Dependency getDependency(String fromid, String toid, String organizationId) throws SQLException, NotFoundException;*/

    List<Dependency> getClusterDependencies(String organizationId, int clusterId) throws SQLException;

    List<Dependency> getRejectedDependencies(String organizationId) throws SQLException;

    List<Dependency> getReqDependencies(String organizationId, String requirementId) throws SQLException;

    List<Dependency> getDependencies(String organizationId) throws SQLException;

    /*boolean existsDependency(String fromid, String toid, String organizationId) throws SQLException;

    void updateDependency(String fromid, String toid, String organizationId, String newStatus, int newCluster) throws SQLException, NotFoundException;

    void updateClusterDependencies(String organizationId, int oldClusterId, int newClusterId) throws SQLException;

    void updateClusterDependencies(String organizationId, String requirementId, String status, int newClusterId) throws SQLException;

    void deleteReqDependencies(String reqId, String organizationId) throws SQLException;*/

    void clearOrganizationResponses(String organizationId) throws SQLException, NotFoundException;

    void clearOrganization(String organizationId) throws NotFoundException, SQLException;

    void clearDatabase() throws IOException, SQLException;


}
