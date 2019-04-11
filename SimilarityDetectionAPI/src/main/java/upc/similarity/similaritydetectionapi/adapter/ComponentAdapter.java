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
import upc.similarity.similaritydetectionapi.exception.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ComponentAdapter {


    //main operations
    public abstract List<Dependency> simReqReq(String organization, String req1, String req2) throws InternalErrorException, BadRequestException, NotFoundException;

    public abstract List<Dependency> simReqProject(String organization, String req, List<String> reqs) throws InternalErrorException, BadRequestException, NotFoundException;

    public abstract void similarityProject(String compare, float treshold, String filename, List<Requirement> requirements, List<Dependency> dependencies) throws ComponentException, BadRequestException, NotFoundException;

    public abstract void similarityCluster(String type, String compare, float treshold, String filename, List<Requirement> requirements, List<Dependency> dependencies) throws ComponentException, BadRequestException, NotFoundException;


    //auxiliary operations
    protected String connection_component(String URL, JSONArray json) throws InternalErrorException, BadRequestException {

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(URL);
        if (json != null) httppost.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

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

        return json_response;
    }

    protected abstract void throw_component_exception(Exception e, String message) throws InternalErrorException;

    protected abstract void check_excepcions(int status, String response) throws InternalErrorException, BadRequestException;

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

