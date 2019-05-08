package upc.similarity.semilarapi.dao;

import upc.similarity.semilarapi.entity.Model;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.NotFinishedException;

import java.sql.SQLException;

public interface modelDAO {

    public void saveModel(String organization, Model model) throws SQLException;

    public Model getModel(String organization) throws SQLException, BadRequestException;

    public void saveResponse(String organizationId, String responseId) throws SQLException;

    public void saveResponsePage(String organizationId, String responseId, int page, String jsonResponse) throws SQLException;

    public void finishComputation(String organizationId, String responseId) throws SQLException;

    public String getResponsePage(String organizationId, String responseId) throws SQLException, BadRequestException, NotFinishedException;

    public void clearDB(String organization) throws SQLException;

}
