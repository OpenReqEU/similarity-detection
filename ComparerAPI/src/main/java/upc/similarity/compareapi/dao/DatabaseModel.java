package upc.similarity.compareapi.dao;

import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Model;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.exception.NotFoundException;

import java.sql.SQLException;
import java.util.List;

public interface DatabaseModel {

    public void saveModel(String organization, Model model) throws SQLException;

    public Model getModel(String organization, boolean withFrequency) throws SQLException, NotFoundException;

    public void saveResponse(String organizationId, String responseId) throws SQLException;

    public void saveResponsePage(String organizationId, String responseId, String jsonResponse) throws SQLException, NotFoundException;

    public void saveException(String organizationId, String responseId, String jsonResponse) throws SQLException;

    public void finishComputation(String organizationId, String responseId) throws SQLException;

    public String getResponsePage(String organizationId, String responseId) throws SQLException, NotFoundException, NotFinishedException;

    //public int getTotalPages(String organizationId, String responseId) throws SQLException, NotFoundException;

    public List<Dependency> getResponsePage(String organizationId, String responseId, int pageNumber) throws SQLException, NotFoundException;

    public Dependency getDependency(String fromid, String toid) throws SQLException, NotFoundException;

    public boolean existsDependency(String fromid, String toid) throws SQLException;

    public void updateDependency(String fromid, String toid, String newStatus, int newCluster) throws SQLException, NotFoundException;

    public void clearOrganizationResponses(String organization) throws SQLException, NotFoundException;

    public void createDatabase() throws SQLException;


}
