package upc.similarity.similaritydetectionapi.adapter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;
import upc.similarity.similaritydetectionapi.entity.Dependency;
import upc.similarity.similaritydetectionapi.entity.Requirement;
import upc.similarity.similaritydetectionapi.exception.*;

import java.io.IOException;
import java.util.List;

public class CompareAdapter extends ComponentAdapter{

    private static final String URL = "http://localhost:9405/upc/Compare/";


    /*
    Similarity without clusters
     */


    @Override
    public void buildModel(String responseId, String organization, boolean compare, List<Requirement> requirements) throws ComponentException {

        JSONArray requirementsJson = listRequirementsToJson(requirements);

        connectionComponentPost(URL + "BuildModel?compare=" + compare + "&organization=" + organization + "&responseId=" + responseId, requirementsJson);
    }

    @Override
    public void buildModelAndCompute(String responseId, String organization, boolean compare, double threshold, List<Requirement> requirements) throws ComponentException {

        JSONArray requirementsJson = listRequirementsToJson(requirements);

        connectionComponentPost(URL + "BuildModelAndCompute?responseId=" + responseId + "&compare=" + compare + "&organization=" + organization + "&threshold=" + threshold, requirementsJson);
    }

    @Override
    public void addRequirements(String responseId, String organization, List<Requirement> requirements) throws ComponentException {

        JSONArray requirementsJson = listRequirementsToJson(requirements);

        connectionComponentPost(URL + "AddRequirements?organization=" + organization + "&responseId=" + responseId, requirementsJson);
    }

    @Override
    public void deleteRequirements(String responseId, String organization, List<Requirement> requirements) throws ComponentException {

        JSONArray requirementsJson = listRequirementsToJson(requirements);

        connectionComponentPost(URL + "DeleteRequirements?organization=" + organization + "&responseId=" + responseId, requirementsJson);
    }

    @Override
    public String simReqReq(String responseId, String organization, String req1, String req2) throws ComponentException {

        return connectionComponentPost(URL + "SimReqReq?organization=" + organization + "&req1=" + req1 + "&req2=" + req2 + "&responseId=" + responseId, null);
    }

    @Override
    public void simReqOrganization(String responseId, String organization, double threshold, List<String> requirements) throws ComponentException {

        JSONArray requirementsJson = new JSONArray(requirements);

        connectionComponentPost(URL + "SimReqOrganization?responseId=" + responseId + "&organization=" + organization + "&threshold=" + threshold, requirementsJson);
    }

    @Override
    public void simNewReqOrganization(String responseId, String organization, double threshold, List<Requirement> requirements) throws ComponentException {

        JSONArray requirementsJson = listRequirementsToJson(requirements);

        connectionComponentPost(URL + "SimNewReqOrganization?responseId=" + responseId + "&organization=" + organization + "&threshold=" + threshold, requirementsJson);
    }

    @Override
    public void simReqProject(String responseId, String organization, double threshold, List<String> req, List<String> requirements) throws ComponentException {

        JSONArray requirementsToCompare = new JSONArray();
        for (String aux: req) requirementsToCompare.put(aux);
        JSONArray projectRequirements = new JSONArray();
        for (String aux: requirements) projectRequirements.put(aux);

        JSONObject jsonToSend = new JSONObject();
        jsonToSend.put("reqs_to_compare",requirementsToCompare);
        jsonToSend.put("project_reqs",projectRequirements);

        connectionComponentPost(URL + "SimReqProject?organization=" + organization + "&responseId=" + responseId + "&threshold=" + threshold, jsonToSend);
    }

    @Override
    public void simProject(String responseId, String organization, double threshold, List<String> requirements) throws ComponentException {

        JSONArray jsonToSend = new JSONArray();
        for (String aux: requirements) jsonToSend.put(aux);

        connectionComponentPost(URL + "SimProject?organization=" + organization + "&responseId=" + responseId + "&threshold=" + threshold, jsonToSend);
    }


    /*
    Similarity with clusters
     */

    @Override
    public void buildClusters(String responseId, String organization, boolean compare, double threshold, MultipartFile input) throws ComponentException {

        String content = "";
        try {
            content = new String(input.getBytes(), "UTF-8");
        } catch (IOException e) {
            throw new InternalErrorException("Error loading input file");
        }

        connectionComponentPost(URL + "BuildClusters?responseId=" + responseId + "&compare=" + compare + "&organization=" + organization + "&threshold=" + threshold, content);
    }

    @Override
    public void buildClustersAndCompute(String responseId, String organization, boolean compare, double threshold, int maxNumber, MultipartFile input) throws ComponentException {

        String content = "";
        try {
            content = new String(input.getBytes(), "UTF-8");
        } catch (IOException e) {
            throw new InternalErrorException("Error loading input file");
        }

        connectionComponentPost(URL + "BuildClustersAndCompute?responseId=" + responseId + "&compare=" + compare + "&organization=" + organization + "&threshold=" + threshold + "&maxNumber=" + maxNumber, content);
    }

    @Override
    public String simReqClusters(String organization, int maxValue, List<String> requirements) throws ComponentException {

        JSONArray requirementsJson = new JSONArray();
        for (String id: requirements) requirementsJson.put(id);

        return connectionComponentPost(URL + "SimReqClusters?maxValue=" + maxValue + "&organization=" + organization, requirementsJson);
    }

    @Override
    public void treatDependencies(String organization, List<Dependency> dependencies) throws ComponentException {

        JSONArray dependenciesJson = listDependenciesToJson(dependencies);

        connectionComponentPost(URL + "TreatAcceptedAndRejectedDependencies?organization=" + organization, dependenciesJson);
    }

    @Override
    public void batchProcess(String responseId, String organization,  List<Requirement> requirements, List<Dependency> dependencies) throws ComponentException {

        JSONArray requirementsJson = listRequirementsToJson(requirements);
        JSONArray dependenciesJson = listDependenciesToJson(dependencies);

        JSONObject jsonToSend = new JSONObject();
        jsonToSend.put("requirements", requirementsJson);
        jsonToSend.put("dependencies", dependenciesJson);

        connectionComponentPost(URL + "BatchProcess?responseId=" + responseId + "&organization=" + organization, jsonToSend);
    }


    /*
    Auxiliary methods
     */

    @Override
    public String getResponsePage(String organization, String responseId) throws ComponentException {

        return connectionComponentGet(URL + "GetResponsePage?organization=" + organization + "&responseId=" + responseId);
    }

    @Override
    public String getOrganizationInfo(String organization) throws ComponentException {

        return connectionComponentGet(URL + "GetOrganizationInfo?organization=" + organization);
    }

    @Override
    public void deleteOrganizationResponses(String organization) throws ComponentException {

        connectionComponentDelete(URL + "ClearOrganizationResponses?organization=" + organization);
    }

    @Override
    public void deleteOrganization(String organization) throws ComponentException {

        connectionComponentDelete(URL + "ClearOrganization?organization=" + organization);
    }

    @Override
    public void deleteDatabase() throws ComponentException {

        connectionComponentDelete(URL + "ClearDatabase");
    }


    /*
    Private methods
     */

    @Override
    protected void throwComponentException(Exception e, String message) throws InternalErrorException {
        throw new InternalErrorException(message);
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