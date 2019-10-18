package upc.similarity.compareapi.service;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.dao.DatabaseModel;
import upc.similarity.compareapi.dao.SQLiteDatabase;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Organization;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.exception.*;
import upc.similarity.compareapi.util.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to treat the sql exceptions
 */
public class DatabaseOperations {

    private static DatabaseOperations instance = new DatabaseOperations();
    private DatabaseModel databaseModel = getValue();
    private Logger logger = Logger.getInstance();

    //is public to be accessible by tests
    public void setDatabaseModel(DatabaseModel databaseModel) {
        this.databaseModel = databaseModel;
    }

    private DatabaseModel getValue() {
        try {
            return new SQLiteDatabase();
        }
        catch (ClassNotFoundException e) {
            logger.showErrorMessage("Error loading database controller class");
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
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(),organizationId,responseId,"Error while checking existence of an organization in the database");
        }
        return result;
    }

    public void generateResponsePage(String responseId, String organization, JSONArray array, String arrayName) throws InternalErrorException {
        String errorMessage = "Error while saving a new response page to the database";
        JSONObject json = new JSONObject();
        json.put("status",200);
        json.put(arrayName,array);
        try {
            databaseModel.saveResponsePage(organization, responseId,json.toString());
        } catch (NotFoundException | SQLException sq) {
            treatSQLException(sq.getMessage(), organization, responseId, errorMessage);
        } catch (InternalErrorException e) {
            saveInternalException(e.getMessage(), organization, responseId, new InternalErrorException(errorMessage));
        }
    }

    public void generateEmptyResponse(String organization, String responseId) throws InternalErrorException {
        String errorMessage = "Error while saving an empty response to the database";
        try {
            databaseModel.saveResponsePage(organization, responseId,new JSONObject().put("status",200).toString());
            databaseModel.finishComputation(organization,responseId);
        } catch (NotFoundException | SQLException sq) {
            treatSQLException(sq.getMessage(),organization,responseId,errorMessage);
        }catch (InternalErrorException e) {
            saveInternalException(e.getMessage(), organization, responseId, new InternalErrorException(errorMessage));
        }
    }

    public void saveBadRequestException(String organization, String responseId, BadRequestException e) throws BadRequestException, InternalErrorException {
        try {
            if (organization != null && responseId != null) {
                databaseModel.saveExceptionAndFinishComputation(organization, responseId, createJsonException(400, Constants.getInstance().getBadRequestMessage(), e.getMessage()));
            }
            throw e;
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(),organization,responseId,"Error while saving a bad request exception response to the database");
        }
    }

    public void saveForbiddenException(String organization, String responseId, ForbiddenException e) throws ForbiddenException, InternalErrorException {
        try {
            if (organization != null && responseId != null) {
                databaseModel.saveExceptionAndFinishComputation(organization, responseId, createJsonException(403, "Forbidden", e.getMessage()));
            }
            throw e;
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(),organization,responseId,"Error while saving a forbidden exception response to the database");
        }
    }

    public void saveInternalException(String console, String organization, String responseId, InternalErrorException e) throws InternalErrorException {
        if (console.contains("The main database is lock")) Logger.getInstance().showWarnMessage(console + " " + organization + " " + responseId);
        else Logger.getInstance().showErrorMessage(console + " " + organization + " " + responseId);
        try {
            if (organization != null && responseId != null) {
                databaseModel.saveExceptionAndFinishComputation(organization, responseId, createJsonException(500, Constants.getInstance().getInternalErrorMessage(), e.getMessage()));
            }
            throw e;
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(),organization,responseId,"Error while saving a internal error exception response to the database");
        }
    }

    public void saveNotFoundException(String organization, String responseId, NotFoundException e) throws NotFoundException, InternalErrorException {
        try {
            if (organization != null && responseId != null) {
                databaseModel.saveExceptionAndFinishComputation(organization, responseId, createJsonException(404, Constants.getInstance().getNotFoundMessage(), e.getMessage()));
            }
            throw e;
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(),organization,responseId,"Error while saving a not found exception response to the database");
        }
    }

    public void saveNotFinishedException(String organization, String responseId, NotFinishedException e) throws NotFinishedException, InternalErrorException {
        try {
            if (organization != null && responseId != null) {
                databaseModel.saveExceptionAndFinishComputation(organization, responseId, createJsonException(423, Constants.getInstance().getNotFinishedMessage(), e.getMessage()));
            }
            throw e;
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(),organization,responseId,"Error while saving a not finished exception response to the database");
        } catch (InternalErrorException e2) {
            Logger.getInstance().showWarnMessage("The main database is locked, another thread is using it 2 " + organization + " " + responseId);
            throw e2;
        }
    }

    public void finishComputation(String organization, String responseId) throws InternalErrorException {
        String errorMessage = "Error while finishing computation";
        try {
            databaseModel.finishComputation(organization,responseId);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organization, responseId, errorMessage);
        } catch (InternalErrorException e) {
            saveInternalException(e.getMessage(), organization, responseId, new InternalErrorException(errorMessage));
        }
    }

    public void generateResponse(String organization, String responseId, String methodName) throws InternalErrorException {
        String errorMessage = "Error while saving new response to the database";
        try {
            databaseModel.saveResponse(organization,responseId, methodName);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organization, responseId, errorMessage);
        } catch (InternalErrorException e) {
            saveInternalException(e.getMessage(), organization, responseId, new InternalErrorException(errorMessage));
        }
    }

    private String createJsonException(int status, String error, String message) {
        JSONObject result = new JSONObject();
        result.put("status",status);
        result.put("error",error);
        result.put("message",message);
        return result.toString();
    }

    public OrganizationModels loadOrganizationModels(String organization, String responseId, boolean readOnly) throws NotFoundException, InternalErrorException {
        OrganizationModels organizationModels = null;
        try {
            organizationModels =  databaseModel.getOrganizationModels(organization, readOnly);
        } catch (NotFoundException e) {
            saveNotFoundException(organization, responseId, e);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organization, responseId, "Error while loading the model from the database");
        }
        return organizationModels;
    }

    public void saveOrganizationModels(String organization, String responseId, OrganizationModels organizationModels) throws InternalErrorException {
        String errorMessage = "Error while saving the new model to the database";
        try {
            databaseModel.saveOrganizationModels(organization, organizationModels);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organization, responseId, errorMessage);
        } catch (IOException | InternalErrorException e) {
            saveInternalException(e.getMessage(), organization, responseId, new InternalErrorException(errorMessage));
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
        String erroMessage = "Error while clearing the organization responses";
        try {
            databaseModel.clearOrganizationResponses(organization);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), null, null, erroMessage);
        }catch (InternalErrorException e) {
            saveInternalException(e.getMessage(), organization, null, new InternalErrorException(erroMessage));
        }
    }

    public void clearOrganization(String organization) throws NotFoundException, InternalErrorException {
        String errorMessage = "Error while clearing the organization data";
        try {
            databaseModel.clearOrganization(organization);
        } catch (IOException | SQLException sq) {
            treatSQLException(sq.getMessage(), null, null, errorMessage);
        } catch (InternalErrorException e) {
            saveInternalException(e.getMessage(), organization, null, new InternalErrorException(errorMessage));
        }
    }

    public void clearDatabase() throws InternalErrorException {
        String errorMessage = "Error while clearing the organization responses";
        try {
            databaseModel.clearDatabase();
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), null, null, errorMessage);
        } catch (InternalErrorException | IOException e) {
            saveInternalException(e.getMessage(),null, null, new InternalErrorException(errorMessage));
        }
    }

    public void createDepsAuxiliaryTable(String organizationId, String responseId) throws InternalErrorException {
        try {
            databaseModel.createDepsAuxiliaryTable(organizationId);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while creating an auxiliary table");
        }
    }

    public Dependency getDependency(String organizationId, String responseId, String fromid, String toid, boolean useAuxiliaryTable) throws NotFoundException, InternalErrorException {
        Dependency result = null;
        try {
            result = databaseModel.getDependency(fromid, toid, organizationId, useAuxiliaryTable);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while loading a dependency from the database");
        }
        return result;
    }

    public void updateModelClustersAndDependencies(String organization, String responseId, OrganizationModels organizationModels, List<Dependency> dependencies, boolean useDepsAuxiliaryTable) throws InternalErrorException {
        String errorMessage = "Error while saving the new model to the database";
        try {
            databaseModel.updateClustersAndDependencies(organization, organizationModels, dependencies, useDepsAuxiliaryTable);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organization, responseId, errorMessage);
        }
    }

    public void updateClusterDependencies(String organizationId, String responseId, int oldClusterId, int newClusterId, boolean useAuxiliaryTable) throws InternalErrorException {
        try {
            databaseModel.updateClusterDependencies(organizationId, oldClusterId, newClusterId, useAuxiliaryTable);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while loading the cluster dependencies from the database");
        }
    }

    public void updateClusterDependencies(String organizationId, String responseId, String requirementId, int newClusterId, boolean useAuxiliaryTable) throws InternalErrorException {
        try {
            databaseModel.updateClusterDependencies(organizationId, requirementId, newClusterId, useAuxiliaryTable);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while loading the cluster dependencies from the database");
        }
    }

    public void deleteProposedClusterDependencies(String organizationId, String responseId, int clusterId, boolean useAuxiliaryTable) throws InternalErrorException {
        try {
            databaseModel.deleteProposedClusterDependencies(organizationId, clusterId, useAuxiliaryTable);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while deleting the cluster dependencies from the database");
        }
    }

    public void saveDependencyOrReplace(String organizationId, String responseId, Dependency dependency, boolean useAuxiliaryTable) throws InternalErrorException {
        try {
            databaseModel.saveDependencyOrReplace(organizationId, dependency, useAuxiliaryTable);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while saving a dependency to the database");
        }
    }

    public void saveDependencies(String organizationId, String responseId, List<Dependency> dependencies, boolean useAuxiliaryTable) throws InternalErrorException {
        try {
            databaseModel.saveDependencies(organizationId, dependencies, useAuxiliaryTable);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while saving a dependency to the database");
        }
    }

    public void updateDependencyStatus(String organizationId, String responseId, String fromid, String toid, String newStatus, int newClusterId, boolean useAuxiliaryTable) throws InternalErrorException {
        try {
            databaseModel.updateDependencyStatus(organizationId, fromid, toid, newStatus, newClusterId, useAuxiliaryTable);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while updating a dependency from the database");
        }
    }

    public List<Dependency> getClusterDependencies(String organizationId, String responseId, int clusterId, boolean useAuxiliaryTable) throws InternalErrorException {
        List<Dependency> result = new ArrayList<>();
        try {
            result = databaseModel.getClusterDependencies(organizationId,clusterId,useAuxiliaryTable);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while loading the cluster dependencies from the database");
        }
        return result;
    }

    public List<Dependency> getDependenciesByStatus(String organizationId, String responseId, String status, boolean useAuxiliaryTable) throws InternalErrorException {
        List<Dependency> result = new ArrayList<>();
        try {
            result = databaseModel.getDependenciesByStatus(organizationId, status,useAuxiliaryTable);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while loading the rejected dependencies from the database");
        }
        return result;
    }

    public List<Dependency> getReqDepedencies(String organizationId, String responseId, String requirementId, String status, boolean useAuxiliaryTable) throws InternalErrorException {
        List<Dependency> result = new ArrayList<>();
        try {
            result = databaseModel.getReqDependencies(organizationId, requirementId, status, useAuxiliaryTable);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while loading the "+requirementId+" dependencies from the database");
        }
        return result;
    }

    public boolean existReqInOrganizationModel(String organizationId, String responseId, String requirement) throws InternalErrorException, NotFoundException {
        boolean result = false;
        try {
            result = databaseModel.existReqInOrganizationModel(organizationId,requirement);
        } catch (NotFoundException e) {
            saveNotFoundException(organizationId, null, e);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, null, "Error while checking existence of a requirement");
        }
        return result;
    }

    public Organization getOrganizationInfo(String organizationId) throws NotFoundException, InternalErrorException {
        Organization organization = null;
        try {
            organization = databaseModel.getOrganizationInfo(organizationId);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), null, null, "Error while loading the organization's information from the database");
        }
        return organization;
    }


    public void deleteReqDependencies(String organizationId, String responseId, String requirementId, boolean useAuxiliaryTable) throws InternalErrorException {
        try {
            databaseModel.deleteReqDependencies(organizationId, requirementId, useAuxiliaryTable);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), organizationId, responseId, "Error while delete the deleted requirement dependencies");
        }
    }

    public void deleteOldResponses(long borderTime) throws InternalErrorException {
        String errorMessage = "Error while deleting old responses";
        try {
            databaseModel.clearOldResponses(borderTime);
        } catch (SQLException sq) {
            treatSQLException(sq.getMessage(), null, null, errorMessage);
        } catch (InternalErrorException e) {
            saveInternalException(e.getMessage(), null, null, new InternalErrorException(errorMessage));
        }
    }



    /*
    Auxiliary operations
     */

    private void treatSQLException(String sqlMessage, String organization, String responseId, String message) throws InternalErrorException {
        logger.showErrorMessage(sqlMessage + " " + organization + " " + responseId);
        try {
            if (organization != null && responseId != null) {
                databaseModel.saveExceptionAndFinishComputation(organization, responseId, createJsonException(500, Constants.getInstance().getSqlErrorMessage(), message));
            }
        } catch (SQLException sq2) {
            logger.showErrorMessage("Error while saving SQL exception: " + sq2.getMessage());
        }
        throw new InternalErrorException(message);
    }
}
