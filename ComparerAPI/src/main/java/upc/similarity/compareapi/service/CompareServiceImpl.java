package upc.similarity.compareapi.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import upc.similarity.compareapi.config.Control;
import upc.similarity.compareapi.dao.DatabaseModel;
import upc.similarity.compareapi.dao.SQLiteDatabase;
import upc.similarity.compareapi.entity.*;
import upc.similarity.compareapi.entity.input.ReqProject;
import upc.similarity.compareapi.exception.BadRequestException;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.exception.NotFoundException;
import upc.similarity.compareapi.util.CosineSimilarity;
import upc.similarity.compareapi.util.Tfidf;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

@Service("comparerService")
public class CompareServiceImpl implements CompareService {

    private static String component = "Similarity-UPC";
    private static String status = "proposed";
    private static String dependencyType = "duplicates";
    private static int maxDepsForPage = 20000;
    private static String badRequestMessage = "Bad request";
    private static String notFoundMessage = "Not found";
    private static String sqlErrorMessage = "Database error";
    private DatabaseModel databaseModel = getValue();
    private Control control = Control.getInstance();

    private DatabaseModel getValue() {
        try {
            return new SQLiteDatabase();
        }
        catch (ClassNotFoundException e) {
            control.showErrorMessage("Error loading database controller class");
        }
        return null;
    }

    @Override
    public void buildModel(String responseId, String compare, String organization, List<Requirement> requirements) throws BadRequestException, InternalErrorException {
        control.showInfoMessage("BuildModel: Start computing");
        generateResponse(organization,responseId);
        try {
            saveModel(organization, generateModel(compare, deleteDuplicates(requirements)));
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }
        try {
            databaseModel.saveResponsePage(organization, responseId, 0, new JSONObject().put("status",200).toString());
            databaseModel.finishComputation(organization,responseId);
        } catch (SQLException sq) {
            treatSQLException(sq);
        }
        control.showInfoMessage("BuildModel: Finish computing");
    }

    @Override
    public void buildModelAndCompute(String responseId, String compare, String organization, double threshold, List<Requirement> requirements) throws BadRequestException, NotFoundException, InternalErrorException {
        control.showInfoMessage("BuildModelAndCompute: Start computing");
        generateResponse(organization,responseId);
        try {
            Model model = generateModel(compare, deleteDuplicates(requirements));
            saveModel(organization, model);
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }
        List<String> requirementsIds = new ArrayList<>();
        for (Requirement requirement: requirements) {
            requirementsIds.add(requirement.getId());
        }
        simProject(responseId,organization,threshold,requirementsIds,true);
        control.showInfoMessage("BuildModelAndCompute: Finish computing");
    }


    @Override
    public Dependency simReqReq(String organization, String req1, String req2) throws NotFoundException, InternalErrorException {
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();
        Model model = loadModel(organization);
        if (!model.getDocs().containsKey(req1)) throw new NotFoundException("The requirement with id " + req1 + " is not present in the model loaded form the database");
        if (!model.getDocs().containsKey(req2)) throw new NotFoundException("The requirement with id " + req2 + " is not present in the model loaded form the database");
        double score = cosineSimilarity.compute(model.getDocs(),req1,req2);
        return new Dependency(score,req1,req2,status,dependencyType,component);
    }

    @Override
    public void simReqOrganization(String responseId, String compare, String organization, double threshold, List<Requirement> requirements) throws NotFoundException, InternalErrorException, BadRequestException {
        control.showInfoMessage("SimReqOrganization: Start computing");
        generateResponse(organization,responseId);

        Model model = null;
        try {
            model = loadModel(organization);
        } catch (NotFoundException e) {
            saveNotFoundException(organization, responseId, e);
        }

        List<String> projectRequirements = new ArrayList<>(model.getDocs().keySet());
        List<Requirement> requirementsNotDuplicated = null;
        try {
            requirementsNotDuplicated = deleteDuplicates(requirements);
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }
        List<String> requirementsToCompare = new ArrayList<>();

        Map<String, Integer> corpusFrequency = model.getCorpusFrequency();
        Map<String, Integer> oldCorpusFrequency = new HashMap<>();
        Iterator it = corpusFrequency.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String word = (String) pair.getKey();
            int value = (int) pair.getValue();
            oldCorpusFrequency.put(word, value);
        }

        Map<String, Map<String, Double>> docs = model.getDocs();
        for (Requirement requirement: requirementsNotDuplicated) {
            String id = requirement.getId();
            requirementsToCompare.add(id);
            if (docs.containsKey(id)) { //problem: if the requirement had this word before applying cutoff parameter
                projectRequirements.remove(id);
                Map<String, Double> words = docs.get(id);
                for (String word: words.keySet()) {
                    int value = corpusFrequency.get(word);
                    if (value == 1) corpusFrequency.remove(word);
                    else corpusFrequency.put(word, value-1);
                }
                docs.remove(id);
            }
        }

        try {
            updateModel(compare, requirementsNotDuplicated,model,oldCorpusFrequency);
            saveModel(organization, model);
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }
        simReqProject(responseId,organization , threshold, new ReqProject(requirementsToCompare, projectRequirements), true);

        control.showInfoMessage("SimReqOrganization: Finish computing");
    }

    @Override
    public void simReqProject(String responseId, String organization, double threshold, ReqProject projectRequirements, boolean responseCreated) throws NotFoundException, InternalErrorException, BadRequestException {
        control.showInfoMessage("SimReqProject: Start computing");
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();

        if (!responseCreated) generateResponse(organization,responseId);

        Model model = null;
        try {
            model = loadModel(organization);
            for (String req: projectRequirements.getReqsToCompare()) {
                if (projectRequirements.getProjectReqs().contains(req)) throw new BadRequestException("The requirement with id " + req + " is already inside the project");
            }
        } catch (NotFoundException e) {
            saveNotFoundException(organization, responseId, e);
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }

        int cont = 0;
        int pages = 0;

        JSONArray array = new JSONArray();
        for (String req1: projectRequirements.getReqsToCompare()) {
            if (model.getDocs().containsKey(req1)) {
                for (String req2 : projectRequirements.getProjectReqs()) {
                    if (!req1.equals(req2) && model.getDocs().containsKey(req2)) {
                        double score = cosineSimilarity.compute(model.getDocs(), req1, req2);
                        if (score >= threshold) {
                            Dependency dependency = new Dependency(score, req1, req2, status, dependencyType, component);
                            array.put(dependency.toJSON());
                            ++cont;
                            if (cont >= maxDepsForPage) {
                                generateResponsePage(responseId, organization, pages, array);
                                ++pages;
                                array = new JSONArray();
                                cont = 0;
                            }
                        }
                    }
                }
                projectRequirements.getProjectReqs().add(req1);
            }
        }

        if (array.length() > 0) {
            generateResponsePage(responseId, organization, pages, array);
        }

        finishComputation(organization, responseId);
        control.showInfoMessage("SimReqProject: Finish computing");
    }

    @Override
    public void simProject(String responseId, String organization, double threshold, List<String> projectRequirements, boolean responseCreated) throws NotFoundException, InternalErrorException {
        control.showInfoMessage("SimProject: Start computing");
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();

        if (!responseCreated) generateResponse(organization,responseId);

        Model model = null;
        try {
            model = loadModel(organization);
        } catch (NotFoundException e) {
            saveNotFoundException(organization, responseId, e);
        }

        int cont = 0;
        int pages = 0;
        long number_dependencies = 0;

        control.showInfoMessage("Number requirements: " + projectRequirements.size());

        JSONArray array = new JSONArray();

        for (int i = 0; i < projectRequirements.size(); ++i) {
            String req1 = projectRequirements.get(i);
            if (model.getDocs().containsKey(req1)) {
                for (int j = i + 1; j < projectRequirements.size(); ++j) {
                    String req2 = projectRequirements.get(j);
                    if (!req2.equals(req1) && model.getDocs().containsKey(req2)) {
                        double score = cosineSimilarity.compute(model.getDocs(), req1, req2);
                        if (score >= threshold) {
                            ++number_dependencies;
                            Dependency dependency = new Dependency(score, req1, req2, status, dependencyType, component);
                            array.put(dependency.toJSON());
                            ++cont;
                            if (cont >= maxDepsForPage) {
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

        control.showInfoMessage("Number dependencies: " + number_dependencies);

        finishComputation(organization, responseId);
        control.showInfoMessage("SimProject: Finish computing");
    }

    @Override
    public String getResponsePage(String organization, String responseId) throws NotFoundException, InternalErrorException, NotFinishedException {

        String responsePage;
        try {
            responsePage = databaseModel.getResponsePage(organization, responseId);
        } catch (SQLException sq) {
            control.showErrorMessage(sq.getMessage());
            throw new InternalErrorException("Error while loading new response page");
        }
        return responsePage;
    }

    @Override
    public void clearOrganizationResponses(String organization) throws InternalErrorException, NotFoundException {
        try {
            databaseModel.clearOrganizationResponses(organization);
        } catch (SQLException sq) {
            control.showErrorMessage(sq.getMessage());
            throw new InternalErrorException("Error while clearing the organization responses");
        }
    }

    @Override
    public void clearDatabase() throws InternalErrorException {
        try {
            Path path = Paths.get("../"+ SQLiteDatabase.getDbName());
            Files.delete(path);
            File file = new File("../"+ SQLiteDatabase.getDbName());
            if (!file.createNewFile()) throw new InternalErrorException("Error while clearing the database. Error while creating new database file.");
            databaseModel.createDatabase();
        } catch (IOException e) {
            control.showErrorMessage(e.getMessage());
            throw new InternalErrorException(e.getMessage());
        } catch (SQLException sq) {
            control.showErrorMessage(sq.getMessage());
            throw new InternalErrorException("Error while clearing the database");
        }
    }



    /*
    auxiliary operations
     */

    private List<Requirement> deleteDuplicates(List<Requirement> requirements) throws BadRequestException {
        HashSet<String> ids = new HashSet<>();
        List<Requirement> result = new ArrayList<>();
        for (Requirement requirement: requirements) {
            if (requirement.getId() == null) throw new BadRequestException("There is a requirement without id.");
            if (!ids.contains(requirement.getId())) {
                result.add(requirement);
                ids.add(requirement.getId());
            }
        }
        return result;
    }

    private void writeToFile(String fileName, String text) {
        try (FileWriter fw = new FileWriter(fileName, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(text);
                bw.newLine();
        } catch (IOException e) {
            control.showErrorMessage(e.getMessage());
        }
    }

    private void saveBadRequestException(String organization, String responseId, BadRequestException e) throws BadRequestException, InternalErrorException {
        try {
            databaseModel.saveResponsePage(organization, responseId, 0, createJsonException(400, badRequestMessage, e.getMessage()));
            databaseModel.finishComputation(organization, responseId);
            throw e;
        } catch (SQLException sq) {
            treatSQLException(sq);
        }
    }

    private void saveNotFoundException(String organization, String responseId, NotFoundException e) throws NotFoundException, InternalErrorException {
        try {
            databaseModel.saveResponsePage(organization, responseId, 0, createJsonException(404, notFoundMessage, e.getMessage()));
            databaseModel.finishComputation(organization, responseId);
            throw e;
        } catch (SQLException sq) {
            treatSQLException(sq);
        }
    }

    private void treatSQLException(SQLException sq) throws InternalErrorException {
        control.showErrorMessage(sq.getMessage());
        throw new InternalErrorException(sqlErrorMessage);
    }

    private void finishComputation(String organization, String responseId) throws InternalErrorException {
        try {
            databaseModel.finishComputation(organization,responseId);
        } catch (SQLException sq) {
            control.showErrorMessage(sq.getMessage());
            throw new InternalErrorException("Error while finishing computation");
        }
    }

    private void generateResponsePage(String responseId, String organization, int pages, JSONArray array) throws InternalErrorException {
        JSONObject json = new JSONObject();
        if (pages == 0) json.put("status",200);
        json.put("dependencies",array);
        try {
            databaseModel.saveResponsePage(organization, responseId, pages,json.toString());
        } catch (SQLException sq) {
            control.showErrorMessage(sq.getMessage());
            throw new InternalErrorException("Error while saving new response page to the database");
        }
    }

    private void generateResponse(String organization, String responseId) throws InternalErrorException {
        try {
            databaseModel.saveResponse(organization,responseId);
        } catch (SQLException sq) {
            control.showErrorMessage(sq.getMessage());
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
            return databaseModel.getModel(organization);
        } catch (SQLException sq) {
            control.showErrorMessage(sq.getMessage());
            throw new InternalErrorException("Error while loading the model from the database");
        }
    }

    private Model generateModel(String compare, List<Requirement> requirements) throws BadRequestException, InternalErrorException {
        Tfidf tfidf = Tfidf.getInstance();
        Map<String, Integer> corpusFrequency = new HashMap<>();
        List<String> text = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        buildCorpus(compare,requirements,text,ids);
        Map<String, Map<String, Double>> docs = tfidf.extractKeywords(text,ids,corpusFrequency);
        return new Model(docs,corpusFrequency);
    }

    private void updateModel(String compare, List<Requirement> requirements, Model model, Map<String, Integer> oldCorpusFrequency) throws BadRequestException, InternalErrorException {
        Tfidf tfidf = Tfidf.getInstance();
        List<String> text = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        buildCorpus(compare,requirements,text,ids);
        tfidf.addNewReqs(text,ids,model,oldCorpusFrequency);
    }

    private void saveModel(String organization, Model model) throws InternalErrorException {
        try {
            databaseModel.saveModel(organization, model);
        } catch (SQLException sq) {
            control.showErrorMessage(sq.getMessage());
            throw new InternalErrorException("Error while saving the new model to the database");
        }
    }

    private void buildCorpus(String compare, List<Requirement> requirements, List<String> arrayText, List<String> arrayIds) throws BadRequestException {
        for (Requirement requirement: requirements) {
            arrayIds.add(requirement.getId());
            String text = "";
            if (requirement.getName() != null) text = text.concat(cleanText(requirement.getName()) + ". ");
            if ((compare.equals("true")) && (requirement.getText() != null)) text = text.concat(cleanText(requirement.getText()));
            arrayText.add(text);
        }
    }

    private String cleanText(String text) {
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
