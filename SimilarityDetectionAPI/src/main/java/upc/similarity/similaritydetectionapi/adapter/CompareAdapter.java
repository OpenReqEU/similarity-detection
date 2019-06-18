package upc.similarity.similaritydetectionapi.adapter;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.similaritydetectionapi.entity.Requirement;
import upc.similarity.similaritydetectionapi.exception.*;

import java.util.List;

public class CompareAdapter extends ComponentAdapter{

    private static final String URL = "http://localhost:9405/upc/Compare/";

    public void buildModel(String filename, String organization, boolean compare, List<Requirement> requirements) throws ComponentException {

        JSONArray requirementsJson = listRequirementsToJson(requirements);

        connectionComponentPost(URL + "BuildModel?compare=" + compare + "&organization=" + organization + "&filename=" + filename, requirementsJson);
    }

    public void buildModelAndCompute(String filename, String organization, boolean compare, double threshold, List<Requirement> requirements) throws ComponentException {

        JSONArray requirementsJson = listRequirementsToJson(requirements);

        connectionComponentPost(URL + "BuildModelAndCompute?filename=" + filename + "&compare=" + compare + "&organization=" + organization + "&threshold=" + threshold, requirementsJson);
    }

    @Override
    public void simReqOrganization(String filename, String organization, boolean compare, double threshold, List<Requirement> requirements) throws ComponentException {

        JSONArray requirementsJson = listRequirementsToJson(requirements);

        connectionComponentPost(URL + "SimReqOrganization?filename=" + filename + "&compare=" + compare + "&organization=" + organization + "&threshold=" + threshold, requirementsJson);
    }

    @Override
    public String simReqReq(String filename, String organization, String req1, String req2) throws ComponentException {

        return connectionComponentPost(URL + "SimReqReq?organization=" + organization + "&req1=" + req1 + "&req2=" + req2 + "&filename=" + filename, null);
    }

    @Override
    public void simReqProject(String filename, String organization, List<String> req, double threshold, List<String> requirements) throws ComponentException {

        JSONArray requirementsToCompare = new JSONArray();
        for (String aux: req) requirementsToCompare.put(aux);
        JSONArray projectRequirements = new JSONArray();
        for (String aux: requirements) projectRequirements.put(aux);

        JSONObject jsonToSend = new JSONObject();
        jsonToSend.put("reqs_to_compare",requirementsToCompare);
        jsonToSend.put("project_reqs",projectRequirements);

        connectionComponentPost(URL + "SimReqProject?organization=" + organization + "&filename=" + filename + "&threshold=" + threshold, jsonToSend);
    }

    @Override
    public void simProject(String filename, String organization, double threshold, List<String> reqs) throws ComponentException {

        JSONArray jsonToSend = new JSONArray();
        for (String aux: reqs) jsonToSend.put(aux);

        connectionComponentPost(URL + "SimProject?organization=" + organization + "&filename=" + filename + "&threshold=" + threshold, jsonToSend);
    }

    @Override
    public String getResponsePage(String organization, String responseId) throws ComponentException {

        return connectionComponentGet(URL + "GetResponsePage?organization=" + organization + "&responseId=" + responseId);
    }

    @Override
    public void deleteOrganizationResponses(String organization) throws ComponentException {

        connectionComponentDelete(URL + "ClearOrganizationResponses?organization=" + organization);
    }

    @Override
    public void deleteDatabase() throws ComponentException {

        connectionComponentDelete(URL + "ClearDatabase");
    }

    @Override
    protected void throwComponentException(Exception e, String message) throws InternalErrorException {
        throw new InternalErrorException("Comparer Exception:" + message + ". " + e.getMessage());
    }

    protected void checkExceptions(int status, String resJSON) throws ComponentException {

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