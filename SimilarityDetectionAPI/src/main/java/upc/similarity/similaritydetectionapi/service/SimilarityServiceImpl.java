package upc.similarity.similaritydetectionapi.service;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Service;
import upc.similarity.similaritydetectionapi.AdaptersController;
import upc.similarity.similaritydetectionapi.adapter.ComponentAdapter;
import upc.similarity.similaritydetectionapi.adapter.CompareAdapter;
import upc.similarity.similaritydetectionapi.config.Control;
import upc.similarity.similaritydetectionapi.entity.Project;
import upc.similarity.similaritydetectionapi.entity.input_output.*;
import upc.similarity.similaritydetectionapi.exception.*;
import upc.similarity.similaritydetectionapi.values.Component;

import java.io.*;
import java.util.*;

@Service("similarityService")
public class SimilarityServiceImpl implements SimilarityService {

    private static Component component = Component.TfidfCompare;
    private static String thresholdNotOk = "The threshold must be a number between 0 and 1 both included";
    private Random rand = new Random();


    /*
    Main operations
     */

    @Override
    public ResultId buildModel(String url, String organization, boolean compare, Requirements input) throws InternalErrorException, BadRequestException {

        checkInput(input, 0);
        ResultId id = getId();

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ResultJson result = new ResultJson(id.getId(),"AddReqs");
                try {
                    CompareAdapter compareAdapter = new CompareAdapter();
                    compareAdapter.buildModel(id.getId(),organization,compare,input.getRequirements());
                    result.setCode(200);
                } catch (ComponentException e) {
                    result.setException(e.getStatus(),e.getError(),e.getMessage());
                }
                finally {
                    updateClient(result,url);
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public ResultId buildModelAndCompute(String url, String organization, boolean compare, double threshold, Requirements input) throws InternalErrorException, BadRequestException {

        checkInput(input, threshold);
        ResultId id = getId();

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ResultJson result = new ResultJson(id.getId(),"AddReqsAndCompute");
                try {
                    CompareAdapter compareAdapter = new CompareAdapter();
                    compareAdapter.buildModelAndCompute(id.getId(),organization,compare,threshold,input.getRequirements());
                    result.setCode(200);
                } catch (ComponentException e) {
                    result.setException(e.getStatus(),e.getError(),e.getMessage());
                }
                finally {
                    updateClient(result,url);
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public ResultId buildClustersAndComputeOrphans(String url, String organization, boolean compare, double threshold, ProjectWithDependencies input) throws InternalErrorException, BadRequestException {

        checkThreshold(threshold);
        if (!input.inputOk()) throw new BadRequestException("The provided json has not requirements or dependencies");
        ResultId id = getId();

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ResultJson result = new ResultJson(id.getId(),"AddClustersAndComputeOrphans");
                try {
                    CompareAdapter compareAdapter = new CompareAdapter();
                    compareAdapter.buildClustersAndComputeOrphans(id.getId(),organization,compare,threshold,input.getRequirements(),input.getDependencies());
                    result.setCode(200);
                } catch (ComponentException e) {
                    result.setException(e.getStatus(),e.getError(),e.getMessage());
                }
                finally {
                    updateClient(result,url);
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public ResultId buildClusters(String url, String organization, boolean compare, ProjectWithDependencies input) throws InternalErrorException, BadRequestException {

        if (!input.inputOk()) throw new BadRequestException("The provided json has not requirements or dependencies");
        ResultId id = getId();

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ResultJson result = new ResultJson(id.getId(),"AddClusters");
                try {
                    CompareAdapter compareAdapter = new CompareAdapter();
                    compareAdapter.buildClusters(id.getId(),organization,compare,input.getRequirements(),input.getDependencies());
                    result.setCode(200);
                } catch (ComponentException e) {
                    result.setException(e.getStatus(),e.getError(),e.getMessage());
                }
                finally {
                    updateClient(result,url);
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public ResultId simReqOrganization(String url, String organization, boolean compare, double threshold, Requirements input) throws InternalErrorException, BadRequestException {

        checkInput(input, threshold);
        ResultId id = getId();

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ResultJson result = new ResultJson(id.getId(),"SimReqOrganization");
                try {
                    CompareAdapter compareAdapter = new CompareAdapter();
                    compareAdapter.simReqOrganization(id.getId(),organization,compare,threshold,input.getRequirements());
                    result.setCode(200);
                } catch (ComponentException e) {
                    result.setException(e.getStatus(),e.getError(),e.getMessage());
                }
                finally {
                    updateClient(result,url);
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public String simReqReq(String organization, String req1, String req2) throws ComponentException {

        ResultId id = getId();

        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
        return componentAdapter.simReqReq(id.getId(),organization,req1,req2);
    }

    @Override
    public ResultId simReqProject(String url, String organization, double threshold, int maxNumber, List<String> req, String projectId, Projects input) throws BadRequestException, InternalErrorException, NotFoundException {

        checkThreshold(threshold);
        Project project = searchProject(projectId,input.getProjects());

        ResultId id = getId();

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ResultJson result = new ResultJson(id.getId(),"ReqProject");
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
                    componentAdapter.simReqProject(id.getId(),organization,req,threshold,project.getSpecifiedRequirements());
                    result.setCode(200);
                } catch (ComponentException e) {
                    result.setException(e.getStatus(),e.getError(),e.getMessage());
                }
                finally {
                    updateClient(result,url);
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public ResultId simProject(String url, String organization, double threshold, int maxNumber, String projectId, Projects input) throws BadRequestException, InternalErrorException, NotFoundException {

        checkThreshold(threshold);
        Project project = searchProject(projectId,input.getProjects());
        ResultId id = getId();

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ResultJson result = new ResultJson(id.getId(),"Project");
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
                    componentAdapter.simProject(id.getId(),organization,threshold,project.getSpecifiedRequirements());
                    result.setCode(200);
                } catch (ComponentException e) {
                    result.setException(e.getStatus(),e.getError(),e.getMessage());
                }
                finally {
                    updateClient(result,url);
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public String getResponsePage(String organization, String responseId) throws ComponentException {
        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
        return componentAdapter.getResponsePage(organization,responseId);
    }

    @Override
    public void deleteOrganizationResponses(String organization) throws ComponentException {
        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
        componentAdapter.deleteOrganizationResponses(organization);
    }

    @Override
    public void deleteDatabase() throws ComponentException {
        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
        componentAdapter.deleteDatabase();
    }




    /*
    Auxiliary operations
     */


    private ResultId getId() {
        return new ResultId(System.currentTimeMillis() + "_" + rand.nextInt(1000));
    }

    private Project searchProject(String project, List<Project> projects) throws NotFoundException {

        boolean found = false;
        Project projectInput = null;
        for (int i = 0; (!found) && (i < projects.size()); ++i) {
            projectInput = projects.get(i);
            if (projectOk(projectInput) && projectInput.getId().equals(project)) found = true;
        }

        if (!found) throw new NotFoundException("There is not a project with id " + project + " in the JSON provided"); //Error: project not found

        return projectInput;
    }

    private boolean projectOk(Project project) {
        return project.getId() != null;
    }

    private void updateClient(ResultJson json, String url) {
        Control control = Control.getInstance();
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(url);
        httppost.setEntity(new StringEntity(json.toJSON(), ContentType.APPLICATION_JSON));

        int httpStatus;
        try {
            HttpResponse response = httpclient.execute(httppost);
            httpStatus = response.getStatusLine().getStatusCode();
            if ((httpStatus >= 200) && (httpStatus < 300)) control.showInfoMessage("The connection with the external server was successful");
            else control.showErrorMessage("An error occurred when connecting with the external server");
        } catch (IOException e) {
            control.showErrorMessage(e.getMessage());
        }
    }

    private void checkInput(Requirements input, double threshold) throws BadRequestException {
        if (!input.inputOk()) throw new BadRequestException("The provided json has not requirements");
        checkThreshold(threshold);
    }

    private void checkThreshold(double threshold) throws BadRequestException {
        if (threshold < 0 || threshold > 1) throw new BadRequestException(thresholdNotOk);
    }


}