package upc.similarity.similaritydetectionapi.service;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import upc.similarity.similaritydetectionapi.AdaptersController;
import upc.similarity.similaritydetectionapi.adapter.ComponentAdapter;
import upc.similarity.similaritydetectionapi.adapter.SemilarAdapter;
import upc.similarity.similaritydetectionapi.entity.Dependency;
import upc.similarity.similaritydetectionapi.entity.Project;
import upc.similarity.similaritydetectionapi.entity.input_output.JsonProject;
import upc.similarity.similaritydetectionapi.entity.Requirement;
import upc.similarity.similaritydetectionapi.entity.input_output.Requirements;
import upc.similarity.similaritydetectionapi.exception.*;
import upc.similarity.similaritydetectionapi.values.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

@Service("similarityService")
public class SimilarityServiceImpl implements SimilarityService {

    private static String path = "../testing/output/";
    private static String component = "Semilar";




    //Main operations

    @Override
    public List<Dependency> simReqReq(String organization, String req1, String req2) throws BadRequestException, InternalErrorException, NotFoundException {

        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
        return componentAdapter.simReqReq(organization,req1,req2);
    }

    @Override
    public List<Dependency> simReqProj(String organization, String req, String project_id, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException {

        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
        Project project = search_project(project_id,input.getProjects());
        return componentAdapter.simReqProject(organization,req,project.getSpecifiedRequirements());
    }

    @Override
    public List<Dependency> simProject(String organization, String project_id, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException {

        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
        Project project = search_project(project_id,input.getProjects());
        return componentAdapter.simProject(organization,project.getSpecifiedRequirements());
    }

    @Override
    public void buildModel(String organization, boolean compare, Requirements input) throws InternalErrorException, BadRequestException {

        if (!input.OK()) throw new BadRequestException("The provided json has not requirements");
        SemilarAdapter semilarAdapter = new SemilarAdapter();
        semilarAdapter.buildModel(organization,compare,input.getRequirements());
    }

    @Override
    public void clearDB() throws InternalErrorException, BadRequestException {

        SemilarAdapter semilarAdapter = new SemilarAdapter();
        semilarAdapter.clearDB();
    }





    //Auxiliary operations

    private List<Requirement> search_requirements(List<String> req, List<Requirement> requirements) throws NotFoundException {

        List<Requirement> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (String id: req) ids.add(id);

        for (int i = 0; (!ids.isEmpty()) && (i < requirements.size()); ++i) {
            Requirement req_aux = requirements.get(i);
            if (req_ok(req_aux) && ids.contains(req_aux.getId())) {
                result.add(req_aux);
                ids.remove(req_aux.getId());
            }
        }

        if (!ids.isEmpty()) {
            for (String id: ids) {
                throw new NotFoundException("There is not requirement with id \'" + id + "\' in the JSON provided"); //Error: req not found
            }
        }

        return result;
    }

    private Project search_project(String project, List<Project> projects) throws NotFoundException {

        boolean found = false;
        Project project_input = null;
        for (int i = 0; (!found) && (i < projects.size()); ++i) {
            project_input = projects.get(i);
            if (project_ok(project_input) && project_input.getId().equals(project)) found = true;
        }

        if (!found) throw new NotFoundException("There is not project with id \'" + project + "\' in the JSON provided"); //Error: project not found

        return project_input;
    }

    private List<Requirement> search_project_requirements(Project project, List<Requirement> requirements) {

        List<Requirement> result = new ArrayList<>();
        List<String> requirements_id = project.getSpecifiedRequirements();
        Set<String> ids = new HashSet<>();
        for (String id: requirements_id) ids.add(id);

        for (int i = 0; (!ids.isEmpty()) && (i < requirements.size()); i++) {
            Requirement requirement_aux = requirements.get(i);
            String id_aux = requirement_aux.getId();
            if (id_aux != null && ids.contains(id_aux)) {
                result.add(requirement_aux);
                ids.remove(id_aux);
            }
        }

        return result;
    }

    private boolean validCompare(String compare) {

        if (compare.equals("true") || compare.equals("false")) return true;
        else return false;

        /*String[] parts = compare.split("-");
        boolean correcto = true;
        for (int i = 0; (correcto) && (i < parts.length); ++i) {
            String aux = parts[i];
            if (!aux.equals("")) {
                if (!aux.equals("Name") && !aux.equals("Text") && !aux.equals("Comments")) correcto = false;
            }
        }
        return correcto;*/
    }

    private boolean project_ok(Project project) {
        if (project.getId() == null) return false;
        else return true;
    }

    private boolean req_ok(Requirement req) {
        if (req.getId() == null) return false;
        else return true;
    }

    private String exception_to_JSON(int status, String error, String message) {
        JSONObject result = new JSONObject();
        result.put("status",status);
        result.put("error",error);
        result.put("message",message);
        return result.toString();
    }

    private void update_client(InputStream targetStream, String url, String id, String success, String operation) {
        String finish = "";
        try {
            //prepare post
            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost(url);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addPart("result", new InputStreamBody(targetStream, "filename"));
            JSONObject json_to_send = new JSONObject();
            json_to_send.put("id",id);
            json_to_send.put("success",success);
            json_to_send.put("operation",operation);
            builder.addPart("info", new StringBody(json_to_send.toString(), ContentType.APPLICATION_JSON));
            HttpEntity entity = builder.build();
            httppost.setEntity(entity);
            //execute post
            HttpResponse response = httpclient.execute(httppost);
            int statusCode = response.getStatusLine().getStatusCode();
            if ((statusCode >= 200) && (statusCode < 300)) finish = "SUCCESS";
            else finish = "ERROR in external server";
        } catch (IOException e) {
            finish = "ERROR connecting with external server";
        }
        finally {
            LocalDateTime now = LocalDateTime.now();
            int year = now.getYear();
            int month = now.getMonthValue();
            int day = now.getDayOfMonth();
            int hour = now.getHour();
            int minute = now.getMinute();
            System.out.println(finish + " -- " + hour + ":" + minute + "  " + month + "/" + day + "/" + year);
        }
    }

    private File create_file(String filename) throws InternalErrorException {

        File file;
        try {
            file = new File(filename);
            if (!file.createNewFile()) throw new InternalErrorException("Error while creating new file");
        } catch (IOException e) {
            throw new InternalErrorException(e.getMessage());
        }
        return file;
    }

    private void delete_file(File file) throws InternalErrorException {

        if (!file.delete()) throw new InternalErrorException("Error deleting file with name: " + file.getName());
    }


}