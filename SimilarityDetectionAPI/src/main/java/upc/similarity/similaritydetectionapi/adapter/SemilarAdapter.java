package upc.similarity.similaritydetectionapi.adapter;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
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
import java.util.List;

public class SemilarAdapter extends ComponentAdapter{

    private static final String URL = "http://localhost:9405/upc/Semilar/";

    @Override
    public void similarity(String compare, Requirement req_entity1, Requirement req_entity2, String filename, List<Dependency> dependencies) throws ComponentException, BadRequestException {

        JSONObject req_json1 = req_entity1.toJSON();
        JSONObject req_json2 = req_entity2.toJSON();
        JSONArray deps_json = list_dependencies_to_JSON(dependencies);

        JSONObject json_to_send = new JSONObject();
        json_to_send.put("req1",req_json1);
        json_to_send.put("req2",req_json2);
        json_to_send.put("dependencies",deps_json);

        connection_component(URL + "PairSim?compare=" + compare + "&filename=" + filename, json_to_send);
    }

    @Override
    public void similarityReqProject(String compare, float treshold, String filename, List<Requirement> requirements, List<Requirement> project_requirements, List<Dependency> dependencies) throws ComponentException, BadRequestException {

        JSONArray reqs_to_compare_json = list_requirements_to_JSON(requirements);
        JSONArray reqs_json = list_requirements_to_JSON(project_requirements);
        JSONArray deps_json = list_dependencies_to_JSON(dependencies);

        JSONObject json_to_send = new JSONObject();
        json_to_send.put("requirements",reqs_to_compare_json);
        json_to_send.put("project_requirements",reqs_json);
        json_to_send.put("dependencies",deps_json);

        connection_component(URL + "ReqProjSim?compare=" + compare + "&threshold=" + treshold + "&filename=" + filename,json_to_send);
    }

    @Override
    public void similarityProject(String compare, float treshold, String filename, List<Requirement> requirements, List<Dependency> dependencies) throws ComponentException, BadRequestException {

        JSONArray reqs_json = list_requirements_to_JSON(requirements);
        JSONArray deps_json = list_dependencies_to_JSON(dependencies);

        JSONObject json_to_send = new JSONObject();
        json_to_send.put("requirements",reqs_json);
        json_to_send.put("dependencies",deps_json);

        connection_component(URL + "ProjSim?compare=" + compare + "&threshold=" + treshold + "&filename=" + filename,json_to_send);
    }

    @Override
    public void similarityCluster(String type, String compare, float treshold, String filename, List<Requirement> requirements, List<Dependency> dependencies) throws ComponentException, BadRequestException {

        JSONArray reqs_json = list_requirements_to_JSON(requirements);
        JSONArray deps_json = list_dependencies_to_JSON(dependencies);

        JSONObject json_to_send = new JSONObject();
        json_to_send.put("requirements",reqs_json);
        json_to_send.put("dependencies",deps_json);

        connection_component(URL + "ClusterSim?compare=" + compare + "&threshold=" + treshold + "&filename=" + filename + "&type=" + type,json_to_send);
    }

    public void processRequirements(List<Requirement> requirements) throws ComponentException, BadRequestException {
        //TODO move this part to abstract class

        JSONArray reqs_json = list_requirements_to_JSON(requirements);

        JSONObject json_to_send = new JSONObject();
        json_to_send.put("requirements",reqs_json);

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(URL + "Preprocess");
        httppost.setEntity(new StringEntity(json_to_send.toString(), ContentType.APPLICATION_JSON));

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

    public void clearDB() throws SemilarException, BadRequestException {
        //TODO move this part to abstract class

        HttpClient httpclient = HttpClients.createDefault();
        HttpDelete httpdelete = new HttpDelete(URL + "Clear");

        int httpStatus;
        String res;

        //Execute and get the response.
        try {
            HttpResponse response = httpclient.execute(httpdelete);
            res = EntityUtils.toString(response.getEntity());
            httpStatus = response.getStatusLine().getStatusCode();
        } catch (IOException e) {
            throw new SemilarException("Semilar Exception:" + e.getMessage());
        }
        check_excepcions(httpStatus,res);
    }

    @Override
    protected void throw_component_exception(Exception e, String message) throws SemilarException {
        throw new SemilarException("Semilar Exception:" + message + ". " + e.getMessage());
    }

    protected void check_excepcions(int status, String resJSON) throws SemilarException, BadRequestException {

        if (status != 200) {
            JSONObject result = new JSONObject(resJSON);
            String message = result.getString("message");
            switch (status) {
                case 412: throw new BadRequestException(message);
                case 411: throw new SemilarException("Database error: " + message);
                default: throw new SemilarException("Semilar component is not working");
            }
        }
    }
}