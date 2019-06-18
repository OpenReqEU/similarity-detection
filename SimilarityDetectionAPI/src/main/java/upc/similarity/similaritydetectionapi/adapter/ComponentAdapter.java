package upc.similarity.similaritydetectionapi.adapter;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import upc.similarity.similaritydetectionapi.entity.Requirement;
import upc.similarity.similaritydetectionapi.exception.*;

import java.io.IOException;
import java.util.List;

public abstract class ComponentAdapter {


    /*
    Main operations
     */

    public abstract String simReqReq(String filename, String organization, String req1, String req2) throws ComponentException;

    public abstract void simReqProject(String filename, String organization, List<String> req, double threshold, List<String> reqs) throws ComponentException;

    public abstract void simReqOrganization(String filename, String organization, boolean compare, double threshold, List<Requirement> requirements) throws ComponentException;

    public abstract void simProject(String filename, String organization, double threshold, List<String> reqs) throws ComponentException;

    public abstract String getResponsePage(String organization, String responseId) throws ComponentException;

    public abstract void deleteOrganizationResponses(String organization) throws ComponentException;

    public abstract void deleteDatabase() throws ComponentException;

    /*
    Auxiliary operations
     */

    protected String connectionComponentPost(String url, Object json) throws ComponentException {
        HttpPost httppost = new HttpPost(url);
        if (json != null) httppost.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));
        return connectionComponent(httppost);
    }

    protected String connectionComponentGet(String url) throws ComponentException {
        HttpGet httpget = new HttpGet(url);
        return connectionComponent(httpget);
    }

    protected String connectionComponentDelete(String url) throws ComponentException {
        HttpDelete httpDelete = new HttpDelete(url);
        return connectionComponent(httpDelete);
    }

    private String connectionComponent(HttpRequestBase httpRequest) throws ComponentException {
        HttpClient httpclient = HttpClients.createDefault();
        int httpStatus = 200;
        String jsonResponse = "";
        try {
            HttpResponse response = httpclient.execute(httpRequest);
            httpStatus = response.getStatusLine().getStatusCode();
            jsonResponse = EntityUtils.toString(response.getEntity());

        } catch (IOException e) {
            throwComponentException(e,"Error connecting with the component");
        }
        if (httpStatus != 200) checkExceptions(httpStatus,jsonResponse);

        return jsonResponse;
    }

    protected abstract void throwComponentException(Exception e, String message) throws InternalErrorException;

    protected abstract void checkExceptions(int status, String response) throws ComponentException;

    protected JSONArray listRequirementsToJson(List<Requirement> requirements) {

        JSONArray jsonRequirements = new JSONArray();

        for (Requirement req: requirements) {
            jsonRequirements.put(req.toJSON());
        }

        return jsonRequirements;
    }

}

