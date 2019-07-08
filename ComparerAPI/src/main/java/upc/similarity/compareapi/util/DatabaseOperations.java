package upc.similarity.compareapi.util;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.config.Control;
import upc.similarity.compareapi.dao.DatabaseModel;
import upc.similarity.compareapi.dao.SQLiteDatabase;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Model;
import upc.similarity.compareapi.exception.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseOperations {
    //TODO catch right connectionErrorExceptions

    private static DatabaseOperations instance = new DatabaseOperations();
    private DatabaseModel databaseModel = getValue();
    private Control control = Control.getInstance();

    private DatabaseModel getValue() {
        try {
            return new SQLiteDatabase();
        }
        catch (ClassNotFoundException e) {
            control.showErrorMessage("Error loading database controller class");
        }
        return null;
    }

    private DatabaseOperations(){}

    public static DatabaseOperations getInstance() {
        return instance;
    }

    public boolean existsOrganization(String responseId, String organizationId) throws InternalErrorException {
        boolean result = false;
        try {
            result = databaseModel.existsOrganization(organizationId);
        } catch ( SQLException sq) {
            treatSQLException(sq.getMessage(),organizationId,responseId,"Error while checking existence of an organization in the database");
        }
        return result;
    }

    public void generateResponsePage(String responseId, String organization, JSONArray array, String arrayName) throws InternalErrorException {
        JSONObject json = new JSONObject();
        json.put("status",200);
        json.put(arrayName,array);
        try {
            databaseModel.saveResponsePage(organization, responseId,json.toString());
        } catch (NotFoundException | SQLException sq) {
            treatSQLException(sq.getMessage(), organization, responseId, "Error while saving a new response page to the database");
        }
    }

    public void generateEmptyResponse(String organization, String responseId) throws InternalErrorException {
        try {
            databaseModel.saveResponsePage(organization, responseId,new JSONObject().put("status",200).toString());
            databaseModel.finishComputation(organization,responseId);
        } catch (NotFoundException | SQLException sq) {
            treatSQLException(sq.getMessage(),organization,responseId,"Error while saving an empty response to the database");
        }
    }

    public void saveBadRequestException(String organization, String responseId, BadRequestException e) throws BadRequestException, InternalErrorException {
        try {
            if (organization != null && responseId != null) {
                databaseModel.saveException(organization, responseId, createJsonException(400, Constants.getInstance().getBadRequestMessage(), e.getMessage()));
                databaseModel.finishComputation(organization, responseId);
            }
            throw e;
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(),organization,responseId,"Error while saving a bad request exception response to the database");
        }
    }

    public void saveInternalException(String organization, String responseId, InternalErrorException e) throws InternalErrorException {
        Control.getInstance().showErrorMessage(e.getMessage());
        try {
            if (organization != null && responseId != null) {
                databaseModel.saveException(organization, responseId, createJsonException(500, Constants.getInstance().getInternalErrorMessage(), e.getMessage()));
                databaseModel.finishComputation(organization, responseId);
            }
            throw e;
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(),organization,responseId,"Error while saving a internal error exception response to the database");
        }
    }

    public void saveNotFoundException(String organization, String responseId, NotFoundException e) throws NotFoundException, InternalErrorException {
        try {
            if (organization != null && responseId != null) {
                databaseModel.saveException(organization, responseId, createJsonException(404, Constants.getInstance().getNotFoundMessage(), e.getMessage()));
                databaseModel.finishComputation(organization, responseId);
            }
            throw e;
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(),organization,responseId,"Error while saving a not found exception response to the database");
        }
    }

    public void finishComputation(String organization, String responseId) throws InternalErrorException {
        try {
            databaseModel.finishComputation(organization,responseId);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organization, responseId, "Error while finishing computation");
        }
    }

    public void generateResponse(String organization, String responseId) throws InternalErrorException {
        try {
            databaseModel.saveResponse(organization,responseId);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organization, responseId, "Error while saving new response to the database");
        }
    }

    private String createJsonException(int status, String error, String message) {
        JSONObject result = new JSONObject();
        result.put("status",status);
        result.put("error",error);
        result.put("message",message);
        return result.toString();
    }

    public Model loadModel(String organization, String responseId, boolean withFrequency) throws NotFoundException, InternalErrorException {
        Model model = null;
        try {
            model =  databaseModel.getModel(organization, withFrequency);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organization, responseId, "Error while loading the model from the database");
        }
        return model;
    }

    public void saveModel(String organization, String responseId, Model model) throws InternalErrorException {
        String errorMessage = "Error while saving the new model to the database";
        try {
            databaseModel.saveModel(organization, model);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organization, responseId, errorMessage);
        } catch (IOException e) {
            saveInternalException(organization, responseId, new InternalErrorException(errorMessage));
        }
    }

    public String getResponsePage(String organization, String responseId) throws NotFoundException, NotFinishedException, InternalErrorException {
        String response = "{}";
        try {
            response = databaseModel.getResponsePage(organization, responseId);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), null, null, "Error while loading new response page");
        }
        return response;
    }

    public void clearOrganizationResponses(String organization) throws NotFoundException, InternalErrorException {
        try {
            databaseModel.clearOrganizationResponses(organization);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), null, null, "Error while clearing the organization responses");
        }
    }

    public void clearOrganization(String organization) throws NotFoundException, InternalErrorException {
        try {
            databaseModel.clearOrganization(organization);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), null, null, "Error while clearing the organization data");
        }
    }

    public void clearDatabase() throws InternalErrorException {
        String errorMessage = "Error while clearing the organization responses";
        try {
            databaseModel.clearDatabase();
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), null, null, errorMessage);
        } catch (IOException e) {
            saveInternalException(null, null, new InternalErrorException(errorMessage));
        }
    }

    public List<Dependency> getClusterDependencies(String organizationId, String responseId, int clusterId) throws InternalErrorException {
        List<Dependency> result = new ArrayList<>();
        try {
            result = databaseModel.getClusterDependencies(organizationId,clusterId);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while loading the cluster dependencies from the database");
        }
        return result;
    }

    public List<Dependency> getRejectedDependencies(String organizationId, String responseId) throws InternalErrorException {
        List<Dependency> result = new ArrayList<>();
        try {
            result = databaseModel.getRejectedDependencies(organizationId);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while loading the rejected dependencies from the database");
        }
        return result;
    }

    public List<Dependency> getReqDepedencies(String organizationId, String responseId, String requirementId) throws InternalErrorException {
        List<Dependency> result = new ArrayList<>();
        try {
            result = databaseModel.getReqDependencies(organizationId, requirementId);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while loading the "+requirementId+" dependencies from the database");
        }
        return result;
    }

    /*
    public void deleteReqDependencies(String organizationId, String responseId, String requirementId) throws InternalErrorException {
        try {
            databaseModel.deleteReqDependencies(requirementId, organizationId);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while delete the deleted requirement dependencies");
        }
    }

    public Dependency getDependency(String organizationId, String responseId, String fromid, String toid) throws InternalErrorException, NotFoundException {
        Dependency result = null;
        try {
            result = databaseModel.getDependency(fromid, toid, organizationId);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while loading a dependency from the database");
        }
        return result;
    }

    public void saveDependency(String organizationId, String responseId, Dependency dependency) throws InternalErrorException {
        try {
            databaseModel.saveDependency(dependency);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while saving a dependency to the database");
        }
    }

    public void updateClusterDependencies(String organizationId, String responseId, int oldClusterId, int newClusterId) throws InternalErrorException {
        try {
            databaseModel.updateClusterDependencies(organizationId, oldClusterId, newClusterId);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while updating a dependency in the database");
        }
    }

    public void updateClusterDependencies(String organizationId, String responseId, String requirementId, String status, int newClusterId) throws InternalErrorException {
        try {
            databaseModel.updateClusterDependencies(organizationId, requirementId, status, newClusterId);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while updating a dependency in the database");
        }
    }

    public void updateDependency(String organizationId, String responseId, String fromid, String toid, String status, int clusterId) throws NotFoundException, InternalErrorException {
        try {
            databaseModel.updateDependency(fromid, toid, organizationId, status, clusterId);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while updating a dependency to the database");
        }
    }*/

    /*
    Auxiliary operations
     */

    private void treatSQLException(String sqlMessage, String organization, String responseId, String message) throws InternalErrorException {
        control.showErrorMessage(sqlMessage);
        try {
            if (organization != null && responseId != null) {
                databaseModel.saveException(organization, responseId, createJsonException(500, Constants.getInstance().getSqlErrorMessage(), message));
                databaseModel.finishComputation(organization, responseId);
            }
        } catch (SQLException sq2) {
            //empty
        }
        throw new InternalErrorException(message);
    }
}
