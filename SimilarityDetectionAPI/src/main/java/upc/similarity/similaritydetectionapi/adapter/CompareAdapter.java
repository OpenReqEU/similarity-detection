package upc.similarity.similaritydetectionapi.adapter;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.similaritydetectionapi.config.Control;
import upc.similarity.similaritydetectionapi.entity.Dependency;
import upc.similarity.similaritydetectionapi.entity.Requirement;
import upc.similarity.similaritydetectionapi.exception.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CompareAdapter extends ComponentAdapter{

    private static final String URL = "http://localhost:9405/upc/Compare/";


    /*
    Similarity without clusters
     */


    @Override
    public void buildModel(String responseId, String organization, boolean compare, boolean useComponent, List<Requirement> requirements) throws ComponentException {

        JSONArray requirementsJson = listRequirementsToJson(requirements);

        connectionComponentPost(URL + "BuildModel?compare=" + compare + "&organization=" + organization + "&responseId=" + responseId + "&useComponent=" + useComponent, requirementsJson);
    }

    @Override
    public void buildModelAndCompute(String responseId, String organization, boolean compare, boolean useComponent, double threshold, List<Requirement> requirements, int maxNumDeps) throws ComponentException {

        JSONArray requirementsJson = listRequirementsToJson(requirements);

        connectionComponentPost(URL + "BuildModelAndCompute?responseId=" + responseId + "&compare=" + compare + "&organization=" + organization + "&threshold=" + threshold + "&maxDeps=" + maxNumDeps + "&useComponent=" + useComponent, requirementsJson);
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
    public void simReqOrganization(String responseId, String organization, double threshold, List<String> requirements, int maxNumDeps) throws ComponentException {

        JSONArray requirementsJson = new JSONArray(requirements);

        connectionComponentPost(URL + "SimReqOrganization?responseId=" + responseId + "&organization=" + organization + "&threshold=" + threshold + "&maxDeps=" + maxNumDeps, requirementsJson);
    }

    @Override
    public void simNewReqOrganization(String responseId, String organization, double threshold, List<Requirement> requirements, int maxNumDeps) throws ComponentException {

        JSONArray requirementsJson = listRequirementsToJson(requirements);

        connectionComponentPost(URL + "SimNewReqOrganization?responseId=" + responseId + "&organization=" + organization + "&threshold=" + threshold + "&maxDeps=" + maxNumDeps, requirementsJson);
    }

    @Override
    public void simReqProject(String responseId, String organization, double threshold, List<String> req, List<String> requirements, int maxNumDeps) throws ComponentException {

        JSONArray requirementsToCompare = listStringToJson(req);
        JSONArray projectRequirements = listStringToJson(requirements);

        JSONObject jsonToSend = new JSONObject();
        jsonToSend.put("reqs_to_compare",requirementsToCompare);
        jsonToSend.put("project_reqs",projectRequirements);

        connectionComponentPost(URL + "SimReqProject?organization=" + organization + "&responseId=" + responseId + "&threshold=" + threshold + "&maxDeps=" + maxNumDeps, jsonToSend);
    }

    @Override
    public void simProject(String responseId, String organization, double threshold, List<String> requirements, int maxNumDeps) throws ComponentException {

        JSONArray jsonToSend = new JSONArray();
        for (String aux: requirements) jsonToSend.put(aux);

        connectionComponentPost(URL + "SimProject?organization=" + organization + "&responseId=" + responseId + "&threshold=" + threshold + "&maxDeps=" + maxNumDeps, jsonToSend);
    }

    @Override
    public void simProjectProject(String responseId, String organization, double threshold, List<String> firstProjectRequirements, List<String> secondProjectRequirements, int maxNumDeps) throws ComponentException {

        JSONArray firstList = listStringToJson(firstProjectRequirements);
        JSONArray secondList = listStringToJson(secondProjectRequirements);

        JSONObject jsonToSend = new JSONObject();
        jsonToSend.put("first_project_requirements",firstList);
        jsonToSend.put("second_project_requirements",secondList);

        connectionComponentPost(URL + "SimProjectProject?organization=" + organization + "&responseId=" + responseId + "&threshold=" + threshold + "&maxDeps=" + maxNumDeps, jsonToSend);
    }


    /*
    Similarity with clusters
     */

    @Override
    public void buildClusters(String responseId, String organization, boolean compare, boolean useComponent, double threshold, Path p) throws ComponentException {
        try {
            try {
                InputStream inputStream = new FileInputStream(p.toFile());
                connectionComponentPostMultipart(URL + "BuildClusters?responseId=" + responseId + "&compare=" + compare + "&organization=" + organization + "&threshold=" + threshold + "&useComponent=" + useComponent, inputStream);
                Files.delete(p);
            } catch (ComponentException e) {
                Files.delete(p);
                throw e;
            }
        } catch (IOException e) {
            Control.getInstance().showErrorMessage(e.getMessage());
            throw new InternalErrorException("Error while deleting multipart file");
        }
    }

    @Override
    public void buildClustersAndCompute(String responseId, String organization, boolean compare, boolean useComponent, double threshold, int maxNumber, Path p) throws ComponentException {

        try {
            InputStream inputStream = new FileInputStream(p.toFile());
            connectionComponentPostMultipart(URL + "BuildClustersAndCompute?responseId=" + responseId + "&compare=" + compare + "&organization=" + organization + "&threshold=" + threshold + "&maxNumber=" + maxNumber + "&useComponent=" + useComponent, inputStream);
            Files.delete(p);
        } catch (FileNotFoundException e) {
            Control.getInstance().showErrorMessage(e.getMessage());
            throw new InternalErrorException("Error while moving multipart file");
        } catch (IOException e) {
            throw new InternalErrorException("Error while deleting multipart file");
        }
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