package upc.similarity.similaritydetectionapi.adapter;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.similaritydetectionapi.entity.Requirement;
import upc.similarity.similaritydetectionapi.exception.*;

import java.util.List;

public class ComparerAdapter extends ComponentAdapter{

    private static final String URL = "http://localhost:9405/upc/Comparer/";

    public void buildModel(String filename, String organization, boolean compare, List<Requirement> requirements) throws ComponentException {

        JSONArray reqs_json = list_requirements_to_JSON(requirements);

        connection_component_post(URL + "BuildModel?compare=" + compare + "&organization=" + organization + "&filename=" + filename, reqs_json);
    }

    public void buildModelAndCompute(String filename, String organization, boolean compare, double threshold, List<Requirement> requirements) throws ComponentException {

        JSONArray reqs_json = list_requirements_to_JSON(requirements);

        connection_component_post(URL + "BuildModelAndCompute?filename=" + filename + "&compare=" + compare + "&organization=" + organization + "&threshold=" + threshold, reqs_json);
    }

    @Override
    public String simReqReq(String filename, String organization, String req1, String req2) throws ComponentException {

        return connection_component_post(URL + "SimReqReq?organization=" + organization + "&req1=" + req1 + "&req2=" + req2 + "&filename=" + filename, null);
    }

    @Override
    public void simReqProject(String filename, String organization, List<String> req, double threshold, List<String> reqs) throws ComponentException {

        JSONArray reqs_to_compare = new JSONArray();
        for (String aux: req) reqs_to_compare.put(aux);
        JSONArray project_reqs = new JSONArray();
        for (String aux: reqs) project_reqs.put(aux);

        JSONObject json_to_send = new JSONObject();
        json_to_send.put("reqs_to_compare",reqs_to_compare);
        json_to_send.put("project_reqs",project_reqs);

        connection_component_post(URL + "SimReqProject?organization=" + organization + "&filename=" + filename + "&threshold=" + threshold, json_to_send);
    }

    @Override
    public void simProject(String filename, String organization, double threshold, List<String> reqs) throws ComponentException {

        JSONArray json_to_send = new JSONArray();
        for (String aux: reqs) json_to_send.put(aux);

        connection_component_post(URL + "SimProject?organization=" + organization + "&filename=" + filename + "&threshold=" + threshold, json_to_send);
    }

    @Override
    public String getResponsePage(String organization, String responseId) throws ComponentException {

        return connection_component_get(URL + "GetResponsePage?organization=" + organization + "&responseId=" + responseId);
    }

    @Override
    public void deleteOrganizationResponses(String organization) throws ComponentException {

        connection_component_delete(URL + "ClearOrganizationResponses?organization=" + organization);
    }

    @Override
    public void deleteDatabase() throws ComponentException {

        connection_component_delete(URL + "ClearDatabase");
    }

    @Override
    protected void throw_component_exception(Exception e, String message) throws InternalErrorException {
        throw new InternalErrorException("Comparer Exception:" + message + ". " + e.getMessage());
    }

    protected void check_exceptions(int status, String resJSON) throws ComponentException {

        if (status != 200) {
            JSONObject result = new JSONObject(resJSON);
            String message = result.getString("message");
            if (status == 400) throw new BadRequestException(message);
            else if (status == 423) throw new NotFinishedException(message);
            else if (status == 404) throw new NotFoundException(message);
            else throw new InternalErrorException(message);
        }
    }
}