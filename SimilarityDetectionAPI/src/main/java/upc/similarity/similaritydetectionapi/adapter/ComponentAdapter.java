package upc.similarity.similaritydetectionapi.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.json.JSONObject;
import upc.similarity.similaritydetectionapi.entity.Dependency;
import upc.similarity.similaritydetectionapi.entity.Requirement;
import upc.similarity.similaritydetectionapi.exception.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ComponentAdapter {


    /*
    Main operations
     */

    public abstract String simReqReq(String filename, String organization, String req1, String req2) throws ComponentException;

    public abstract void simReqProject(String filename, String organization, List<String> req, double threshold, List<String> reqs) throws ComponentException;

    public abstract void simProject(String filename, String organization, double threshold, List<String> reqs) throws ComponentException;

    public abstract String getResponsePage(String organization, String responseId) throws ComponentException;

    public abstract void deleteOrganizationResponses(String organization) throws ComponentException;

    public abstract void deleteDatabase() throws ComponentException;

    /*
    Auxiliary operations
     */

    protected String connection_component_post(String URL, Object json) throws ComponentException {
        HttpPost httppost = new HttpPost(URL);
        if (json != null) httppost.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));
        return connection_component(httppost);
    }

    protected String connection_component_get(String URL) throws ComponentException {
        HttpGet httpget = new HttpGet(URL);
        return connection_component(httpget);
    }

    protected String connection_component_delete(String URL) throws ComponentException {
        HttpDelete httpDelete = new HttpDelete(URL);
        return connection_component(httpDelete);
    }

    private String connection_component(HttpRequestBase httprequest) throws ComponentException {
        HttpClient httpclient = HttpClients.createDefault();
        int httpStatus = 200;
        String json_response = "";
        try {
            HttpResponse response = httpclient.execute(httprequest);
            httpStatus = response.getStatusLine().getStatusCode();
            json_response = EntityUtils.toString(response.getEntity());

        } catch (IOException e) {
            throw_component_exception(e,"Error connecting with the component");
        }
        if (httpStatus != 200) check_exceptions(httpStatus,json_response);

        return json_response;
    }

    protected abstract void throw_component_exception(Exception e, String message) throws InternalErrorException;

    protected abstract void check_exceptions(int status, String response) throws ComponentException;

    protected List<Dependency> JSON_to_dependencies(JSONObject json) throws InternalErrorException {

        ObjectMapper mapper = new ObjectMapper();
        List<Dependency> dependencies = new ArrayList<>();

        try {
            JSONArray json_deps = json.getJSONArray("dependencies");
            for (int i = 0; i < json_deps.length(); ++i) {
                dependencies.add(mapper.readValue(json_deps.getJSONObject(i).toString(), Dependency.class));
            }
        } catch (Exception e) {
            throw new InternalErrorException("Error manipulating the input json");
        }

        return dependencies;
    }

    protected JSONArray list_requirements_to_JSON(List<Requirement> requirements) {

        JSONArray json_reqs = new JSONArray();

        for (Requirement req: requirements) {
            json_reqs.put(req.toJSON());
        }

        return json_reqs;
    }

    protected JSONArray list_dependencies_to_JSON(List<Dependency> dependencies) {

        JSONArray json_deps = new JSONArray();

        for (Dependency dep: dependencies) {
            json_deps.put(dep.toJSON());
        }

        return json_deps;
    }

    protected List<Dependency> JSON_to_list_dependencies(JSONArray array) {
        List<Dependency> result = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            result.add(new Dependency(array.getJSONObject(i)));
        }
        return result;
    }

}

