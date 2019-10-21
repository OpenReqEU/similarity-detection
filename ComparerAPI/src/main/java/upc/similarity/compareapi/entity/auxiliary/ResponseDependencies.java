package upc.similarity.compareapi.entity.auxiliary;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.compareapi.dao.DatabaseModel;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFoundException;

public abstract class ResponseDependencies {

    protected String organization;
    protected String responseId;
    protected String dependenciesArrayName = "dependencies";

    public ResponseDependencies(String organization, String responseId) {
        this.organization = organization;
        this.responseId = responseId;
    }

    public abstract void addDependency(Dependency elem) throws InternalErrorException;

    public abstract void finish() throws InternalErrorException;

    protected void generateResponsePage(String organizationId, String responseId, JSONArray array, String arrayName, DatabaseModel databaseModel) throws InternalErrorException {
        try {
            JSONObject json = new JSONObject();
            json.put("status", 200);
            json.put(arrayName, array);
            databaseModel.saveResponsePage(organizationId, responseId, json.toString());
        } catch (NotFoundException e) {
            throw new InternalErrorException("Error while saving response");
        }
    }
}
