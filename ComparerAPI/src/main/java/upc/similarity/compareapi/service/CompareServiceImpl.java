package upc.similarity.compareapi.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import upc.similarity.compareapi.dao.modelDAO;
import upc.similarity.compareapi.dao.SQLiteDAO;
import upc.similarity.compareapi.entity.*;
import upc.similarity.compareapi.entity.input.ReqProject;
import upc.similarity.compareapi.exception.BadRequestException;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.exception.NotFoundException;
import upc.similarity.compareapi.util.CosineSimilarity;
import upc.similarity.compareapi.util.Tfidf;

import java.io.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

@Service("comparerService")
public class CompareServiceImpl implements CompareService {

    private static Double cutoffParameter=10.0;
    private static String component = "Similarity-UPC";
    private static String status = "proposed";
    private static String dependency_type = "duplicates";
    private static int MAX_PAGE_DEPS = 20000;
    private modelDAO modelDAO = getValue();

    private modelDAO getValue() {
        try {
            return new SQLiteDAO();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void buildModel(String responseId, String compare, String organization, List<Requirement> requirements) throws BadRequestException, InternalErrorException {
        show_time("start");
        generateResponse(organization,responseId);
        try {
            saveModel(organization, generateModel(compare, requirements));
        } catch (BadRequestException e) {
            try {
                modelDAO.saveResponsePage(organization, responseId, 0, createJsonException(400, "Bad request", e.getMessage()));
                modelDAO.finishComputation(organization,responseId);
                throw new BadRequestException(e.getMessage());
            } catch (SQLException sq) {
                throw new InternalErrorException("Error while saving exception to the database");
            }
        }
        try {
            modelDAO.saveResponsePage(organization, responseId, 0, new JSONObject().put("status",200).toString());
            modelDAO.finishComputation(organization,responseId);
        } catch (SQLException sq) {
            throw new InternalErrorException("Error while saving result to the database");
        }
        show_time("finish");
    }

    @Override
    public void buildModelAndCompute(String responseId, String compare, String organization, double threshold, List<Requirement> reqs) throws BadRequestException, NotFoundException, InternalErrorException {
        show_time("start initialization");
        generateResponse(organization,responseId);
        try {
            Model model = generateModel(compare, reqs);
            saveModel(organization, model);
        } catch (BadRequestException e) {
            try {
                modelDAO.saveResponsePage(organization, responseId, 0, createJsonException(400, "Bad request", e.getMessage()));
                modelDAO.finishComputation(organization,responseId);
                throw new BadRequestException(e.getMessage());
            } catch (SQLException sq) {
                throw new InternalErrorException("Error while saving exception to the database");
            }
        }
        show_time("finish initialization");
        List<String> idReqs = new ArrayList<>();
        for (Requirement requirement: reqs) {
            idReqs.add(requirement.getId());
        }
        reqs = null;
        simProject(responseId,organization,threshold,idReqs,true);
    }


    @Override
    public Dependency simReqReq(String organization, String req1, String req2) throws NotFoundException, InternalErrorException {
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();
        show_time("start");
        Model model = loadModel(organization);
        if (!model.getDocs().containsKey(req1)) throw new NotFoundException("The requirement with id " + req1 + " is not present in the model loaded form the database");
        if (!model.getDocs().containsKey(req2)) throw new NotFoundException("The requirement with id " + req2 + " is not present in the model loaded form the database");
        double score = cosineSimilarity.compute(model.getDocs(),req1,req2);
        Dependency result = new Dependency(score,req1,req2,status,dependency_type,component);
        show_time("finish");
        return result;
    }

    @Override
    public void simReqProject(String responseId, String organization, double threshold, ReqProject project_reqs) throws NotFoundException, InternalErrorException, BadRequestException {
        show_time("start computing");
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();

        generateResponse(organization,responseId);

        Model model = null;
        try {
            model = loadModel(organization);
            for (String req: project_reqs.getReqs_to_compare()) {
                if (project_reqs.getProject_reqs().contains(req)) throw new BadRequestException("The requirement with id " + req + " is already inside the project");
            }
        } catch (NotFoundException e) {
            try {
                modelDAO.saveResponsePage(organization, responseId, 0, createJsonException(400, "Bad request", e.getMessage()));
                modelDAO.finishComputation(organization,responseId);
                throw new NotFoundException(e.getMessage());
            } catch (SQLException sq) {
                throw new InternalErrorException("Error while saving exception to the database");
            }
        } catch (BadRequestException e) {
            try {
                modelDAO.saveResponsePage(organization, responseId, 0, createJsonException(400, "Bad request", e.getMessage()));
                modelDAO.finishComputation(organization,responseId);
                throw new BadRequestException(e.getMessage());
            } catch (SQLException sq) {
                throw new InternalErrorException("Error while saving exception to the database");
            }
        }

        int cont = 0;
        int pages = 0;

        JSONArray array = new JSONArray();
        for (String req1: project_reqs.getReqs_to_compare()) {
            for (String req2 : project_reqs.getProject_reqs()) {
                if (!req1.equals(req2) && model.getDocs().containsKey(req2)) {
                    double score = cosineSimilarity.compute(model.getDocs(), req1, req2);
                    if (score >= threshold) {
                        Dependency dependency = new Dependency(score, req1, req2, status, dependency_type, component);
                        array.put(dependency.toJSON());
                        ++cont;
                        if (cont >= MAX_PAGE_DEPS) {
                            generateResponsePage(responseId, organization, pages, array);
                            ++pages;
                            array = new JSONArray();
                            cont = 0;
                        }
                    }
                }
            }
            project_reqs.getProject_reqs().add(req1);
        }

        if (array.length() > 0) {
            generateResponsePage(responseId, organization, pages, array);
        }

        try {
            modelDAO.finishComputation(organization,responseId);
        } catch (SQLException e) {
            throw new InternalErrorException("Error while finishing computation");
        }

        show_time("finish computing");
    }

    @Override
    public void simProject(String responseId, String organization, double threshold, List<String> project_reqs, boolean responseCreated) throws NotFoundException, InternalErrorException {
        show_time("start computing");
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();

        if (!responseCreated) generateResponse(organization,responseId);

        Model model = null;
        try {
            model = loadModel(organization);
        } catch (NotFoundException e) {
            try {
                modelDAO.saveResponsePage(organization, responseId, 0, createJsonException(400, "Bad request", e.getMessage()));
                modelDAO.finishComputation(organization,responseId);
                throw new NotFoundException(e.getMessage());
            } catch (SQLException sq) {
                throw new InternalErrorException("Error while saving exception to the database");
            }
        }

        int cont = 0;
        int pages = 0;

        JSONArray array = new JSONArray();

        for (int i = 0; i < project_reqs.size(); ++i) {
            String req1 = project_reqs.get(i);
            if (model.getDocs().containsKey(req1)) {
                for (int j = i + 1; j < project_reqs.size(); ++j) {
                    String req2 = project_reqs.get(j);
                    if (!req2.equals(req1) && model.getDocs().containsKey(req2)) {
                        double score = cosineSimilarity.compute(model.getDocs(), req1, req2);
                        if (score >= threshold) {
                            Dependency dependency = new Dependency(score, req1, req2, status, dependency_type, component);
                            array.put(dependency.toJSON());
                            ++cont;
                            if (cont >= MAX_PAGE_DEPS) {
                                generateResponsePage(responseId, organization, pages, array);
                                ++pages;
                                array = new JSONArray();
                                cont = 0;
                            }
                        }
                    }
                }
            }
        }
        if (array.length() > 0) {
            generateResponsePage(responseId, organization, pages, array);
        }

        try {
            modelDAO.finishComputation(organization,responseId);
        } catch (SQLException e) {
            throw new InternalErrorException("Error while finishing computation");
        }

        show_time("finish computing");
    }

    @Override
    public String getResponsePage(String organization, String responseId) throws NotFoundException, InternalErrorException, NotFinishedException {

        String responsePage;
        try {
            responsePage = modelDAO.getResponsePage(organization, responseId);
        } catch (SQLException e) {
            throw new InternalErrorException("Error while loading new response page");
        }
        return responsePage;
    }

    @Override
    public void clearOrganizationResponses(String organization) throws InternalErrorException, NotFoundException {
        try {
            modelDAO.clearOrganizationResponses(organization);
        } catch (SQLException e) {
            throw new InternalErrorException("Error while clearing the database");
        }
    }

    @Override
    public void clearDatabase() throws InternalErrorException {
        try {
            File file = new File("../"+SQLiteDAO.getDb_name());
            if (!file.delete()) throw new InternalErrorException("Database does not exist");
            file = new File("../"+SQLiteDAO.getDb_name());
            if (!file.createNewFile()) throw new InternalErrorException("Error while clearing the database");
            modelDAO.createDatabase();
        } catch (IOException e) {
            throw new InternalErrorException("Error while clearing the database");
        } catch (SQLException e) {
            throw new InternalErrorException("Error while clearing the database");
        }
    }



    /*
    auxiliary operations
     */

    private void generateResponsePage(String responseId, String organization, int pages, JSONArray array) throws InternalErrorException {
        JSONObject json = new JSONObject();
        if (pages == 0) json.put("status",200);
        json.put("dependencies",array);
        try {
            modelDAO.saveResponsePage(organization, responseId, pages,json.toString());
        } catch (SQLException e) {
            throw new InternalErrorException("Error while saving new response page to the database");
        }
    }

    private void generateResponse(String organization, String responseId) throws InternalErrorException {
        try {
            modelDAO.saveResponse(organization,responseId);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new InternalErrorException("Error while saving new response to the database");
        }
    }

    private String createJsonException(int status, String error, String message) {
        JSONObject result = new JSONObject();
        result.put("status",status);
        result.put("error",error);
        result.put("message",message);
        return result.toString();
    }

    private Model loadModel(String organization) throws NotFoundException, InternalErrorException {
        try {
            return modelDAO.getModel(organization);
        } catch (SQLException e) {
            throw new InternalErrorException("Error while loading the model from the database");
        }
    }

    private Model generateModel(String compare, List<Requirement> reqs) throws BadRequestException, InternalErrorException {
        Tfidf tfidf = Tfidf.getInstance();
        Map<String, Integer> corpusFrequency = new HashMap<>();
        List<String> text = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        buildCorpus(compare,reqs,text,ids);
        Map<String, Map<String, Double>> docs = tfidf.extractKeywords(text,ids,corpusFrequency);
        return new Model(docs,corpusFrequency);
    }

    private void saveModel(String organization, Model model) throws InternalErrorException {
        try {
            modelDAO.saveModel(organization, model);
        } catch (SQLException e) {
            throw new InternalErrorException("Error while saving the new model to the database");
        }
    }

    private void show_time(String text) {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int hour = now.getHour();
        int minute = now.getMinute();
        System.out.println(text + " -- " + hour + ":" + minute + "  " + month + "/" + day + "/" + year);
    }

    private void buildCorpus(String compare, List<Requirement> requirements, List<String> array_text, List<String> array_ids) throws BadRequestException {
        for (Requirement requirement: requirements) {
            if (requirement.getId() == null) throw new BadRequestException("There is a requirement without id.");
            array_ids.add(requirement.getId());
            String text = "";
            if (requirement.getName() != null) text = text.concat(clean_text(requirement.getName(),1) + ". ");
            if ((compare.equals("true")) && (requirement.getText() != null)) text = text.concat(clean_text(requirement.getText(),2));
            array_text.add(text);
        }
    }

    private String clean_text(String text, int clean) {
        text = text.replaceAll("(\\{.*?})", " code ");
        text = text.replaceAll("[.$,;\\\"/:|!?=%,()><_0-9\\-\\[\\]{}']", " ");
        String[] aux2 = text.split(" ");
        String result = "";
        for (String a : aux2) {
            if (a.length() > 1) {
                result = result.concat(" " + a);
            }
        }
        return result;
    }
}
