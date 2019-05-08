package upc.similarity.similaritydetectionapi.adapter;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.similaritydetectionapi.entity.Requirement;
import upc.similarity.similaritydetectionapi.exception.*;

import java.util.List;

public class ComparerAdapter extends ComponentAdapter{

    private static final String URL = "http://localhost:9405/upc/Comparer/";

    public void buildModel(String organization, boolean compare, List<Requirement> requirements) throws InternalErrorException, BadRequestException {

        JSONArray reqs_json = list_requirements_to_JSON(requirements);

        try {
            connection_component(URL + "BuildModel?compare=" + compare + "&organization=" + organization, reqs_json);
        } catch (NotFinishedException e) {
            //impossible to get this exception
        }
    }

    public void buildModelAndCompute(String filename, String organization, boolean compare, double threshold, List<Requirement> requirements) throws InternalErrorException, BadRequestException {

        JSONArray reqs_json = list_requirements_to_JSON(requirements);

        try {
            connection_component(URL + "BuildModelAndCompute?filename=" + filename + "&compare=" + compare + "&organization=" + organization + "&threshold=" + threshold, reqs_json);
        } catch (NotFinishedException e) {
            //impossible to get this exception
        }
    }

    @Override
    public String simReqReq(String filename, String organization, String req1, String req2) throws InternalErrorException, BadRequestException {

        try {
            return connection_component(URL + "SimReqReq?organization=" + organization + "&req1=" + req1 + "&req2=" + req2 + "&filename=" + filename, null);
        } catch (NotFinishedException e) {
            //impossible to get this exception
            return null;
        }
    }

    @Override
    public void simReqProject(String filename, String organization, String req, double threshold, List<String> reqs) throws InternalErrorException, BadRequestException {

        JSONArray json_to_send = new JSONArray();
        for (String aux: reqs) json_to_send.put(aux);

        try {
            connection_component(URL + "SimReqProject?organization=" + organization + "&req=" + req + "&filename=" + filename + "&threshold=" + threshold, json_to_send);
        } catch (NotFinishedException e) {
            //impossible to get this exception
        }
    }

    @Override
    public void simProject(String filename, String organization, double threshold, List<String> reqs) throws InternalErrorException, BadRequestException {

        JSONArray json_to_send = new JSONArray();
        for (String aux: reqs) json_to_send.put(aux);

        try {
            connection_component(URL + "SimProject?organization=" + organization + "&filename=" + filename + "&threshold=" + threshold, json_to_send);
        } catch (NotFinishedException e) {
            //impossible to get this exception
        }
    }

    @Override
    public String getResponsePage(String organization, String responseId) throws InternalErrorException, BadRequestException, NotFinishedException {

        return connection_component(URL + "GetResponsePage?organization=" + organization + "&responseId=" + responseId,null);
    }

    /*public void clearDB() throws InternalErrorException, BadRequestException {

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
            throw new InternalErrorException("Semilar Exception:" + e.getMessage());
        }
        check_exceptions(httpStatus,res);
    }*/

    @Override
    protected void throw_component_exception(Exception e, String message) throws InternalErrorException {
        throw new InternalErrorException("Comparer Exception:" + message + ". " + e.getMessage());
    }

    protected void check_exceptions(int status, String resJSON) throws InternalErrorException, BadRequestException, NotFinishedException {

        if (status != 200) {
            JSONObject result = new JSONObject(resJSON);
            String message = result.getString("message");
            if (status == 400) throw new BadRequestException(message);
            else if (status == 423) throw new NotFinishedException(message);
            else throw new InternalErrorException(message);
        }
    }
}