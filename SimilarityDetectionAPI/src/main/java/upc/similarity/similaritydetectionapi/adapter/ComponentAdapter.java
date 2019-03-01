package upc.similarity.similaritydetectionapi.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.similaritydetectionapi.entity.Dependency;
import upc.similarity.similaritydetectionapi.entity.Requirement;
import upc.similarity.similaritydetectionapi.entity.input_output.Requirements;
import upc.similarity.similaritydetectionapi.exception.BadRequestException;
import upc.similarity.similaritydetectionapi.exception.ComponentException;
import upc.similarity.similaritydetectionapi.exception.DKProException;
import upc.similarity.similaritydetectionapi.exception.NotFoundException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ComponentAdapter {


    //main operations
    public abstract void similarity(String compare, Requirement req1, Requirement req2, String filename, List<Dependency> dependencies) throws ComponentException, BadRequestException, NotFoundException;

    public abstract void similarityReqProject(String compare, float treshold, String filename, List<Requirement> requirements, List<Requirement> project_requirements, List<Dependency> dependencies) throws ComponentException, BadRequestException, NotFoundException;

    public abstract void similarityProject(String compare, float treshold, String filename, List<Requirement> requirements, List<Dependency> dependencies) throws ComponentException, BadRequestException, NotFoundException;

    public abstract void similarityCluster(String type, String compare, float treshold, String filename, List<Requirement> requirements, List<Dependency> dependencies) throws ComponentException, BadRequestException, NotFoundException;


    //auxiliary operations
    protected void connection_component(String URL, JSONObject json) throws ComponentException, BadRequestException {

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(URL);
        httppost.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

        int httpStatus = 200;
        String json_response = "";

        //Execute and get the response.
        try {
            HttpResponse response = httpclient.execute(httppost);
            httpStatus = response.getStatusLine().getStatusCode();
            json_response = EntityUtils.toString(response.getEntity());

        } catch (IOException e) {
            throw_component_exception(e,"Error conecting with the component");
        }

        if (httpStatus != 200) check_excepcions(httpStatus,json_response);
    }

    protected abstract void throw_component_exception(Exception e, String message) throws ComponentException;

    protected abstract void check_excepcions(int status, String response) throws ComponentException, BadRequestException;

    protected List<Dependency> JSON_to_dependencies(JSONObject json) throws ComponentException {

        ObjectMapper mapper = new ObjectMapper();
        List<Dependency> dependencies = new ArrayList<>();

        try {
            JSONArray json_deps = json.getJSONArray("dependencies");
            for (int i = 0; i < json_deps.length(); ++i) {
                dependencies.add(mapper.readValue(json_deps.getJSONObject(i).toString(), Dependency.class));
            }
        } catch (Exception e) {
            throw new DKProException("Error manipulating the input json");
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

}

