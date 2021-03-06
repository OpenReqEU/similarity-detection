package upc.similarity.similaritydetectionapi.adapter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import upc.similarity.similaritydetectionapi.entity.Dependency;
import upc.similarity.similaritydetectionapi.entity.Requirement;
import upc.similarity.similaritydetectionapi.exception.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public abstract class ComponentAdapter {


    /*
    Similarity without clusters
     */

    public abstract void buildModel(String responseId, String organization, boolean compare, boolean useComponent, List<Requirement> requirements) throws ComponentException;

    public abstract void buildModelAndCompute(String responseId, String organization, boolean compare, boolean useComponent, double threshold, List<Requirement> requirements, int maxNumDeps) throws ComponentException;

    public abstract void addRequirements(String responseId, String organization, List<Requirement> requirements) throws ComponentException;

    public abstract void deleteRequirements(String responseId, String organization, List<Requirement> requirements) throws ComponentException;

    public abstract String simReqReq(String responseId, String organization, String req1, String req2) throws ComponentException;

    public abstract void simReqOrganization(String responseId, String organization, double threshold, List<String> requirements, int maxNumDeps) throws ComponentException;

    public abstract void simNewReqOrganization(String responseId, String organization, double threshold, List<Requirement> requirements, int maxNumDeps) throws ComponentException;

    public abstract void simReqProject(String responseId, String organization, double threshold, List<String> req, List<String> reqs, int maxNumDeps) throws ComponentException;

    public abstract void simProject(String responseId, String organization, double threshold, List<String> reqs, int maxNumDeps) throws ComponentException;

    public abstract void simProjectProject(String responseId, String organization, double threshold, List<String> firstProjectRequirements, List<String> secondProjectRequirements, int maxNumDeps) throws ComponentException;


    /*
    Similarity with clusters
     */

    public abstract void buildClusters(String responseId, String organization, boolean compare, boolean useComponent, double threshold, Path p) throws ComponentException;

    public abstract void buildClustersAndCompute(String responseId, String organization, boolean compare, boolean useComponent, double threshold, int maxNumber, Path p) throws ComponentException;

    public abstract String simReqClusters(String organization, int maxValue, List<String> requirements) throws ComponentException;

    public abstract void treatDependencies(String organization, List<Dependency> dependencies) throws ComponentException;

    public abstract void batchProcess(String responseId, String organization,  List<Requirement> requirements, List<Dependency> dependencies) throws ComponentException;


    /*
    Auxiliary methods
     */

    public abstract String getResponsePage(String organization, String responseId) throws ComponentException;

    public abstract String getOrganizationInfo(String organization) throws ComponentException;

    public abstract void deleteOrganizationResponses(String organization) throws ComponentException;

    public abstract void deleteOrganization(String organization) throws ComponentException;

    public abstract void deleteDatabase() throws ComponentException;


    /*
    Private methods
     */

    protected String connectionComponentPost(String url, Object json) throws ComponentException {
        HttpPost httppost = new HttpPost(url);
        if (json != null) httppost.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));
        return connectionComponent(httppost);
    }

    protected String connectionComponentPostMultipart(String url, InputStream inputStream) throws ComponentException {
        HttpPost httppost = new HttpPost(url);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addBinaryBody("file", inputStream, ContentType.create("application/json"), "file");
        HttpEntity entity = builder.build();
        httppost.setEntity(entity);
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

    protected JSONArray listRequirementsToJson(List<Requirement> requirements) throws InternalErrorException {
        JSONArray jsonRequirements = new JSONArray();
        for (Requirement req: requirements) {
            jsonRequirements.put(req.toJSON());
        }
        return jsonRequirements;
    }

    protected JSONArray listDependenciesToJson(List<Dependency> dependencies) throws InternalErrorException {
        JSONArray jsonDeps = new JSONArray();
        for (Dependency dep: dependencies) {
            jsonDeps.put(dep.toJSON());
        }
        return jsonDeps;
    }

    protected JSONArray listStringToJson(List<String> inputList) {
        JSONArray jsonList = new JSONArray();
        for (String entry: inputList) jsonList.put(entry);
        return jsonList;
    }

}

