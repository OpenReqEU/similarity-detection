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
import upc.similarity.similaritydetectionapi.entity.input_output.Requirements;
import upc.similarity.similaritydetectionapi.entity.input_output.Result_id;
import upc.similarity.similaritydetectionapi.exception.*;
import upc.similarity.similaritydetectionapi.values.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

@Service("similarityService")
public class SimilarityServiceImpl implements SimilarityService {

    private static String path = "../testing/output/";
    private static String component = "Semilar";
    private Random rand = new Random();




    //Main operations

    @Override
    public Result_id buildModel(String url, String organization, boolean compare, Requirements input) throws InternalErrorException, BadRequestException {

        if (!input.OK()) throw new BadRequestException("The provided json has not requirements");
        Result_id id = get_id();

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    SemilarAdapter semilarAdapter = new SemilarAdapter();
                    semilarAdapter.buildModel(organization,compare,input.getRequirements());
                    String result = "{\"result\":\"Success!\"}";
                    fis = new ByteArrayInputStream(result.getBytes());
                    success = "true";
                } catch (InternalErrorException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(500,"Internal error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(400,"Bad request",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"AddReqs");
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public Result_id simReqReq(String url, String organization, String req1, String req2) throws BadRequestException, InternalErrorException, NotFoundException {

        Result_id id = get_id();

        //Create file to save resulting dependencies
        File file = create_file(path+id.getId());

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
                    componentAdapter.simReqReq(id.getId(),organization,req1,req2);
                    fis = new FileInputStream(file);
                    success = "true";
                } catch (InternalErrorException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(500,"Internal error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(400,"Bad request",e.getMessage()).getBytes());
                } catch (NotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(404,"Not found",e.getMessage()).getBytes());
                } catch (FileNotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(500,"File error",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"ReqReq");
                    try {
                        delete_file(file);
                    } catch (InternalErrorException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public Result_id simReqProject(String url, String organization, double threshold, int max_number, String req, String project_id, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException {

        if (threshold < 0 || threshold > 1) throw new BadRequestException("Threshold must be a number between 0 and 1");
        Project project = search_project(project_id,input.getProjects());

        Result_id id = get_id();

        //Create file to save resulting dependencies
        File file = create_file(path+id.getId());

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
                    componentAdapter.simReqProject(id.getId(),organization,req,threshold,project.getSpecifiedRequirements());
                    fis = new FileInputStream(file);
                    success = "true";
                } catch (InternalErrorException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(500,"Internal error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(400,"Bad request",e.getMessage()).getBytes());
                } catch (NotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(404,"Not found",e.getMessage()).getBytes());
                } catch (FileNotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(500,"File error",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"ReqProject");
                    try {
                        delete_file(file);
                    } catch (InternalErrorException e) {
                        System.out.println(e.getMessage());
                    }
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

        //Create file to save resulting dependencies
        File file = create_file(path+id.getId());

        //New thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream fis = null;
                String success = "false";
                try {
                    ComponentAdapter componentAdapter = AdaptersController.getInstance().getAdpapter(Component.valueOf(component));
                    componentAdapter.simProject(id.getId(),organization,threshold,project.getSpecifiedRequirements());
                    fis = new FileInputStream(file);
                    success = "true";
                } catch (InternalErrorException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(500,"Internal error",e.getMessage()).getBytes());
                } catch (BadRequestException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(400,"Bad request",e.getMessage()).getBytes());
                } catch (NotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(404,"Not found",e.getMessage()).getBytes());
                } catch (FileNotFoundException e) {
                    fis = new ByteArrayInputStream(exception_to_JSON(500,"File error",e.getMessage()).getBytes());
                }
                finally {
                    update_client(fis,url,id.getId(),success,"Project");
                    try {
                        delete_file(file);
                    } catch (InternalErrorException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });

        thread.start();
        return id;
    }

    @Override
    public void clearDB() throws InternalErrorException, BadRequestException {

        SemilarAdapter semilarAdapter = new SemilarAdapter();
        semilarAdapter.clearDB();
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

        if (!found) throw new NotFoundException("There is not project with id \'" + project + "\' in the JSON provided"); //Error: project not found

        return project_input;
    }

    private boolean project_ok(Project project) {
        if (project.getId() == null) return false;
        else return true;
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