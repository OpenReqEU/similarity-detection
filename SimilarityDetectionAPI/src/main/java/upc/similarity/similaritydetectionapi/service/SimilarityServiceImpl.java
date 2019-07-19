package upc.similarity.similaritydetectionapi.service;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import upc.similarity.similaritydetectionapi.AdaptersController;
import upc.similarity.similaritydetectionapi.adapter.ComponentAdapter;
import upc.similarity.similaritydetectionapi.config.Control;
import upc.similarity.similaritydetectionapi.entity.Project;
import upc.similarity.similaritydetectionapi.entity.input_output.*;
import upc.similarity.similaritydetectionapi.exception.*;
import upc.similarity.similaritydetectionapi.values.Component;

import java.io.*;
import java.util.*;

@Service("similarityService")
public class SimilarityServiceImpl implements SimilarityService {

    //TODO check input in all methods

    private static Component component = Component.TfIdfCompare;
    private static String thresholdNotOk = "The threshold must be a number between 0 and 1 both included";
    private Random rand = new Random();


    /*
    Main operations
     */

    @Override
    public ResultId buildModel(String url, String organization, boolean compare, double threshold, Requirements input) throws BadRequestException {

        checkInput(input);
        checkThreshold(threshold);
        ResultId id = getId();

        //New thread
        Thread thread = new Thread(() -> {
            ResultJson result = new ResultJson(id.getId(),"BuildModel");
            try {
                ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
                componentAdapter.buildModel(id.getId(),organization,compare,threshold,input.getRequirements());
                result.setCode(200);
            } catch (ComponentException e) {
                result.setException(e.getStatus(),e.getError(),e.getMessage());
            }
            finally {
                updateClient(result,url);
            }
        });

        thread.start();
        return id;
    }

    @Override
    public ResultId addRequirements(String url, String organization, Requirements input) throws BadRequestException {

        checkInput(input);
        ResultId id = getId();

        //New thread
        Thread thread = new Thread(() -> {
            ResultJson result = new ResultJson(id.getId(),"AddRequirements");
            try {
                ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
                componentAdapter.addRequirements(id.getId(),organization,input.getRequirements());
                result.setCode(200);
            } catch (ComponentException e) {
                result.setException(e.getStatus(),e.getError(),e.getMessage());
            }
            finally {
                updateClient(result,url);
            }
        });

        thread.start();
        return id;
    }

    @Override
    public ResultId deleteRequirements(String url, String organization, Requirements input) throws BadRequestException {

        checkInput(input);
        ResultId id = getId();

        //New thread
        Thread thread = new Thread(() -> {
            ResultJson result = new ResultJson(id.getId(),"DeleteRequirements");
            try {
                ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
                componentAdapter.deleteRequirements(id.getId(),organization,input.getRequirements());
                result.setCode(200);
            } catch (ComponentException e) {
                result.setException(e.getStatus(),e.getError(),e.getMessage());
            }
            finally {
                updateClient(result,url);
            }
        });

        thread.start();
        return id;
    }

    @Override
    public ResultId buildModelAndCompute(String url, String organization, boolean compare, double threshold, Requirements input) throws BadRequestException {

        checkInput(input);
        checkThreshold(threshold);
        ResultId id = getId();

        //New thread
        Thread thread = new Thread(() -> {
            ResultJson result = new ResultJson(id.getId(),"AddReqsAndCompute");
            try {
                ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
                componentAdapter.buildModelAndCompute(id.getId(),organization,compare,threshold,input.getRequirements());
                result.setCode(200);
            } catch (ComponentException e) {
                result.setException(e.getStatus(),e.getError(),e.getMessage());
            }
            finally {
                updateClient(result,url);
            }
        });

        thread.start();
        return id;
    }

    @Override
    public ResultId simReqOrganization(String url, String organization, Requirements input) throws BadRequestException {

        checkInput(input);
        ResultId id = getId();

        //New thread
        Thread thread = new Thread(() -> {
            ResultJson result = new ResultJson(id.getId(),"ReqOrganization");
            try {
                ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
                componentAdapter.simReqOrganization(id.getId(),organization,input.getRequirements());
                result.setCode(200);
            } catch (ComponentException e) {
                result.setException(e.getStatus(),e.getError(),e.getMessage());
            }
            finally {
                updateClient(result,url);
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
    public ResultId simReqProject(String url, String organization, List<String> req, String projectId, Projects input) throws NotFoundException, BadRequestException {

        checkInput(input);
        if (req.isEmpty()) throw new BadRequestException("The input parameter req is empty");
        Project project = searchProject(projectId,input.getProjects());

        ResultId id = getId();

        //New thread
        Thread thread = new Thread(() -> {
            ResultJson result = new ResultJson(id.getId(),"ReqProject");
            try {
                ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
                componentAdapter.simReqProject(id.getId(),organization,req,project.getSpecifiedRequirements());
                result.setCode(200);
            } catch (ComponentException e) {
                result.setException(e.getStatus(),e.getError(),e.getMessage());
            }
            finally {
                updateClient(result,url);
            }
        });

        thread.start();
        return id;
    }

    @Override
    public ResultId simProject(String url, String organization, String projectId, Projects input) throws NotFoundException, BadRequestException {

        checkInput(input);
        Project project = searchProject(projectId,input.getProjects());
        ResultId id = getId();

        //New thread
        Thread thread = new Thread(() -> {
            ResultJson result = new ResultJson(id.getId(),"Project");
            try {
                ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
                componentAdapter.simProject(id.getId(),organization,project.getSpecifiedRequirements());
                result.setCode(200);
            } catch (ComponentException e) {
                result.setException(e.getStatus(),e.getError(),e.getMessage());
            }
            finally {
                updateClient(result,url);
            }
        });

        thread.start();
        return id;
    }



    /*
    Cluster operations
     */

    @Override
    public ResultId buildClusters(String url, String organization, boolean compare, double threshold, MultipartFile input) throws BadRequestException {

        checkThreshold(threshold);
        ResultId id = getId();
        //New thread
        Thread thread = new Thread(() -> {
            ResultJson result = new ResultJson(id.getId(),"BuildClusters");
            try {
                ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
                componentAdapter.buildClusters(id.getId(),organization,compare,threshold,input);
                result.setCode(200);
            } catch (ComponentException e) {
                result.setException(e.getStatus(),e.getError(),e.getMessage());
            }
            finally {
                updateClient(result,url);
            }
        });

        thread.start();
        return id;
    }


    @Override
    public ResultId buildClustersAndCompute(String url, String organization, boolean compare, double threshold, MultipartFile input) throws BadRequestException {

        checkThreshold(threshold);
        ResultId id = getId();

        //New thread
        Thread thread = new Thread(() -> {
            ResultJson result = new ResultJson(id.getId(),"BuildClustersAndCompute");
            try {
                ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
                componentAdapter.buildClustersAndCompute(id.getId(),organization,compare,threshold,input);
                result.setCode(200);
            } catch (ComponentException e) {
                result.setException(e.getStatus(),e.getError(),e.getMessage());
            }
            finally {
                updateClient(result,url);
            }
        });

        thread.start();
        return id;
    }

    @Override
    public String simReqClusters(String organization, int maxNumber, List<String> input) throws ComponentException {

        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
        return componentAdapter.simReqClusters(organization,maxNumber,input);
    }

    @Override
    public void treatDependencies(String organization, Dependencies dependencies) throws ComponentException {

        checkInput(dependencies);
        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
        componentAdapter.treatDependencies(organization, dependencies.getDependencies());
    }

    @Override
    public ResultId cronMethod(String url, String organization, ProjectWithDependencies input) throws ComponentException {

        checkInput(input);
        ResultId id = getId();

        //New thread
        Thread thread = new Thread(() -> {
            ResultJson result = new ResultJson(id.getId(),"BatchProcess");
            try {
                ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
                componentAdapter.cronMethod(id.getId(), organization, input.getRequirements(), input.getDependencies());
                result.setCode(200);
            } catch (ComponentException e) {
                result.setException(e.getStatus(),e.getError(),e.getMessage());
            }
            finally {
                updateClient(result,url);
            }
        });

        thread.start();
        return id;
    }



    /*
    Auxiliary operations
     */

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
    public void clearOrganization(String organization) throws ComponentException {
        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
        componentAdapter.deleteOrganization(organization);
    }

    @Override
    public void clearDatabase() throws ComponentException {
        ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdapter(component);
        componentAdapter.deleteDatabase();
    }




    /*
    Private operations
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
        if (url != null) {
            Control control = Control.getInstance();
            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost(url);
            httppost.setEntity(new StringEntity(json.toJSON(), ContentType.APPLICATION_JSON));

            int httpStatus;
            try {
                HttpResponse response = httpclient.execute(httppost);
                httpStatus = response.getStatusLine().getStatusCode();
                if ((httpStatus >= 200) && (httpStatus < 300))
                    control.showInfoMessage("The connection with the external server was successful");
                else control.showErrorMessage("An error occurred when connecting with the external server");
            } catch (IOException e) {
                control.showErrorMessage("An error occurred when connecting with the external server. File error");
            }
        }
    }

    private void checkInput(Input input) throws BadRequestException {
        if (!input.inputOk()) throw new BadRequestException(input.checkMessage());
    }

    private void checkThreshold(double threshold) throws BadRequestException {
        if (threshold < 0 || threshold > 1) throw new BadRequestException(thresholdNotOk);
    }


}