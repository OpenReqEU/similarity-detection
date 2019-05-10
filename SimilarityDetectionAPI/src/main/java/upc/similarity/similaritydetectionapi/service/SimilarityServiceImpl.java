package upc.similarity.similaritydetectionapi.service;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import upc.similarity.similaritydetectionapi.AdaptersController;
import upc.similarity.similaritydetectionapi.adapter.ComponentAdapter;
import upc.similarity.similaritydetectionapi.adapter.ComparerAdapter;
import upc.similarity.similaritydetectionapi.entity.Dependency;
import upc.similarity.similaritydetectionapi.entity.Project;
import upc.similarity.similaritydetectionapi.entity.input_output.JsonProject;
import upc.similarity.similaritydetectionapi.entity.input_output.Requirements;
import upc.similarity.similaritydetectionapi.entity.input_output.Result_id;
import upc.similarity.similaritydetectionapi.entity.input_output.Result_json;
import upc.similarity.similaritydetectionapi.exception.*;
import upc.similarity.similaritydetectionapi.values.Component;

import java.io.*;
import java.util.*;

@Service("similarityService")
public class SimilarityServiceImpl implements SimilarityService {

    private static String component = "Comparer";
    private Random rand = new Random();


    /*
    Main operations
     */

    @Override
    public Result_id buildModel(String url, String organization, boolean compare, Requirements input) throws InternalErrorException, BadRequestException {

        if (!input.OK()) throw new BadRequestException("The provided json has not requirements");
        Result_id id = get_id();

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Result_json result = new Result_json(id.getId(),"AddReqs");
                try {
                    ComparerAdapter comparerAdapter = new ComparerAdapter();
                    comparerAdapter.buildModel(id.getId(),organization,compare,input.getRequirements());
                    result.setCode(200);
                } catch (ComponentException e) {
                    result.setException(e.getStatus(),e.getError(),e.getMessage());
                }
                finally {
                    update_client(result,url);
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public Result_id buildModelAndCompute(String url, String organization, boolean compare, double threshold, Requirements input) throws InternalErrorException, BadRequestException {

        if (!input.OK()) throw new BadRequestException("The provided json has not requirements");
        Result_id id = get_id();

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Result_json result = new Result_json(id.getId(),"AddReqsAndCompute");
                try {
                    ComparerAdapter comparerAdapter = new ComparerAdapter();
                    comparerAdapter.buildModelAndCompute(id.getId(),organization,compare,threshold,input.getRequirements());
                    result.setCode(200);
                } catch (ComponentException e) {
                    result.setException(e.getStatus(),e.getError(),e.getMessage());
                }
                finally {
                    update_client(result,url);
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public String simReqReq(String organization, String req1, String req2) throws ComponentException {

        Result_id id = get_id();

        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(Component.valueOf(component));
        return componentAdapter.simReqReq(id.getId(),organization,req1,req2);
    }

    @Override
    public Result_id simReqProject(String url, String organization, double threshold, int max_number, List<String> req, String project_id, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException {

        if (threshold < 0 || threshold > 1) throw new BadRequestException("Threshold must be a number between 0 and 1");
        Project project = search_project(project_id,input.getProjects());

        Result_id id = get_id();

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Result_json result = new Result_json(id.getId(),"ReqProject");
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(Component.valueOf(component));
                    componentAdapter.simReqProject(id.getId(),organization,req,threshold,project.getSpecifiedRequirements());
                    result.setCode(200);
                } catch (ComponentException e) {
                    result.setException(e.getStatus(),e.getError(),e.getMessage());
                }
                finally {
                    update_client(result,url);
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public Result_id simProject(String url, String organization, double threshold, int max_number, String project_id, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException {

        if (threshold < 0 || threshold > 1) throw new BadRequestException("Threshold must be a number between 0 and 1");
        Project project = search_project(project_id,input.getProjects());
        Result_id id = get_id();

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Result_json result = new Result_json(id.getId(),"Project");
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(Component.valueOf(component));
                    componentAdapter.simProject(id.getId(),organization,threshold,project.getSpecifiedRequirements());
                    result.setCode(200);
                } catch (ComponentException e) {
                    result.setException(e.getStatus(),e.getError(),e.getMessage());
                }
                finally {
                    update_client(result,url);
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public String getResponsePage(String organization, String responseId) throws ComponentException {
        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(Component.valueOf(component));
        return componentAdapter.getResponsePage(organization,responseId);
    }

    @Override
    public void deleteOrganizationResponses(String organization) throws ComponentException {
        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(Component.valueOf(component));
        componentAdapter.deleteOrganizationResponses(organization);
    }

    @Override
    public void deleteDatabase() throws ComponentException {
        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(Component.valueOf(component));
        componentAdapter.deleteDatabase();
    }




    //Auxiliary operations

    private String exception_to_JSON(int status, String error, String message) {
        JSONObject result = new JSONObject();
        result.put("status",status);
        result.put("error",error);
        result.put("message",message);
        return result.toString();
    }


    private Result_id get_id() {
        return new Result_id(System.currentTimeMillis() + "_" + rand.nextInt(1000));
    }


    private List<Dependency> sort_dependencies(List<Dependency> dependencies, int max_num, double threshold) {

        List<Dependency> result = new ArrayList<>();

        dependencies.sort(new Comparator<Dependency>() {
            @Override
            public int compare(Dependency o1, Dependency o2) {
                if (o1.getDependency_score() < o2.getDependency_score()) return 1;
                else if (o1.getDependency_score() > o2.getDependency_score()) return -1;
                return 0;
            }
        });

        if (max_num <= 0 || max_num > dependencies.size()) max_num = dependencies.size();

        boolean correct = true;
        for (int i = 0; correct && i < max_num; i++) {
            Dependency dependency = dependencies.get(i);
            if (dependency.getDependency_score() < threshold) correct = false;
            else result.add(dependency);
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

        if (!found) throw new NotFoundException("There is not a project with id " + project + " in the JSON provided"); //Error: project not found

        return project_input;
    }

    private boolean project_ok(Project project) {
        if (project.getId() == null) return false;
        else return true;
    }

    private void update_client(Result_json json, String url) {
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(url);
        httppost.setEntity(new StringEntity(json.toJSON(), ContentType.APPLICATION_JSON));

        int httpStatus;
        try {
            HttpResponse response = httpclient.execute(httppost);
            httpStatus = response.getStatusLine().getStatusCode();
            if ((httpStatus >= 200) && (httpStatus < 300)) System.out.println("SUCCESS");
            else System.out.println("ERROR connecting with external server");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}