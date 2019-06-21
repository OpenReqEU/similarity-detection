package upc.similarity.compareapi.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import upc.similarity.compareapi.config.Control;
import upc.similarity.compareapi.dao.DatabaseModel;
import upc.similarity.compareapi.dao.SQLiteDatabase;
import upc.similarity.compareapi.entity.*;
import upc.similarity.compareapi.entity.input.Clusters;
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
    private static String internalErrorMessage = "Internal error";
    private static String dependenciesArrayName = "dependencies";
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
        Model model = null;
        try {
            model = generateModel(compare, deleteDuplicates(requirements));
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }
        List<String> requirementsIds = new ArrayList<>();
        for (Requirement requirement: requirements) {
            requirementsIds.add(requirement.getId());
        }

        project(requirementsIds, model, threshold, responseId, organization);

        saveModel(organization, model);

        finishComputation(organization, responseId);
        control.showInfoMessage("BuildModelAndCompute: Finish computing");
    }

    @Override
    public void addRequirements(String responseId, String compare, String organization, List<Requirement> requirements) throws InternalErrorException, BadRequestException, NotFoundException {
        control.showInfoMessage("AddRequirements: Start computing");
        generateResponse(organization,responseId);

        Model model = null;
        try {
            model = loadModel(organization);
        } catch (NotFoundException e) {
            saveNotFoundException(organization, responseId, e);
        }

        List<Requirement> notDuplicatedRequirements = null;
        try {
            notDuplicatedRequirements = deleteDuplicates(requirements);
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }

        addRequirementsToModel(notDuplicatedRequirements, organization, responseId, compare, model);

        saveModel(organization, model);

        try {
            databaseModel.saveResponsePage(organization, responseId, 0, new JSONObject().put("status",200).toString());
            databaseModel.finishComputation(organization,responseId);
        } catch (SQLException sq) {
            treatSQLException(sq);
        }

        control.showInfoMessage("AddRequirements: Finish computing");
    }

    @Override
    public void deleteRequirements(String responseId, String organization, List<Requirement> requirements) throws InternalErrorException, BadRequestException, NotFoundException {
        control.showInfoMessage("DeleteRequirements: Start computing");
        generateResponse(organization,responseId);

        Model model = null;
        try {
            model = loadModel(organization);
        } catch (NotFoundException e) {
            saveNotFoundException(organization, responseId, e);
        }

        List<Requirement> notDuplicatedRequirements = null;
        try {
            notDuplicatedRequirements = deleteDuplicates(requirements);
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }

        Tfidf.getInstance().deleteReqsAndRecomputeModel(notDuplicatedRequirements,model);

        saveModel(organization, model);

        try {
            databaseModel.saveResponsePage(organization, responseId, 0, new JSONObject().put("status",200).toString());
            databaseModel.finishComputation(organization,responseId);
        } catch (SQLException sq) {
            treatSQLException(sq);
        }

        control.showInfoMessage("DeleteRequirements: Finish computing");
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

        List<Requirement> notDuplicatedRequirements = null;
        try {
            notDuplicatedRequirements = deleteDuplicates(requirements);
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }

        addRequirementsToModel(notDuplicatedRequirements, organization, responseId, compare, model);
        HashSet<String> repeatedHash = new HashSet<>();
        for (Requirement requirement: notDuplicatedRequirements) repeatedHash.add(requirement.getId());

        List<String> projectRequirements = new ArrayList<>();
        List<String> requirementsToCompare = new ArrayList<>();
        for (Requirement requirement: notDuplicatedRequirements) requirementsToCompare.add(requirement.getId());

        Iterator it = model.getDocs().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String id = (String) pair.getKey();
            if (!repeatedHash.contains(id)) projectRequirements.add(id);
        }

        reqProject(requirementsToCompare, projectRequirements, model, threshold, organization, responseId, true);
        saveModel(organization, model);

        finishComputation(organization, responseId);

        control.showInfoMessage("SimReqOrganization: Finish computing");
    }

    @Override
    public void simReqClusters(String responseId, String compare, String organization, double threshold, List<Requirement> requirements) throws NotFoundException, InternalErrorException, BadRequestException {
        control.showInfoMessage("SimReqClusters: Start computing");
        generateResponse(organization,responseId);

        Model model = null;
        try {
            model = loadModel(organization);
        } catch (NotFoundException e) {
            saveNotFoundException(organization, responseId, e);
        }

        if (!model.hasClusters()) saveBadRequestException(organization, responseId, new BadRequestException("The model does not have clusters"));

        List<Requirement> notDuplicatedRequirements = null;
        try {
            notDuplicatedRequirements = deleteDuplicates(requirements);
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }

        HashSet<String> repeatedHash = new HashSet<>();
        for (Requirement requirement: notDuplicatedRequirements) repeatedHash.add(requirement.getId());

        List<String> projectRequirements = new ArrayList<>();
        List<String> requirementsToCompare = new ArrayList<>();
        for (Requirement requirement: notDuplicatedRequirements) requirementsToCompare.add(requirement.getId());

        Iterator it = model.getClusters().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String master = (String) pair.getKey();
            if (!repeatedHash.contains(master)) projectRequirements.add(master);
        }

        addRequirementsToModel(notDuplicatedRequirements, organization, responseId, compare, model);

        reqProject(requirementsToCompare, projectRequirements, model, threshold, organization, responseId, true);

        computeClusterDependencies(organization, responseId, model.getReqCluster(), model.getClusters());

        saveModel(organization, model);

        finishComputation(organization, responseId);

        control.showInfoMessage("SimReqClusters: Finish computing");
    }

    private void addRequirementsToModel(List<Requirement> requirements, String organization, String responseId, String compare, Model model) throws BadRequestException, InternalErrorException {

        if (model.hasClusters()) {
            Map<String, List<String>> clusters = model.getClusters();
            Map<String, ReqClusterInfo> reqCluster = model.getReqCluster();

            for (Requirement requirement: requirements) {
                String id = requirement.getId();
                if (reqCluster.containsKey(id)) {
                    String master = reqCluster.get(id).getCluster();
                    List<String> clusterRequirements = clusters.get(master);
                    clusterRequirements.remove(id);
                    clusters.remove(master);
                    if (master.equals(id) && clusterRequirements.size() > 0) {
                        master = findMaster(clusterRequirements, reqCluster);
                    }
                    if (clusterRequirements.size() > 0) {
                        clusters.put(master,clusterRequirements);
                        for (String req: clusterRequirements) {
                            ReqClusterInfo aux = reqCluster.get(req);
                            reqCluster.put(req, new ReqClusterInfo(master, aux.getDate()));
                        }
                    }
                    reqCluster.remove(id);
                }
                List<String> aux = new ArrayList<>();
                aux.add(id);
                clusters.put(id,aux);
                reqCluster.put(id, new ReqClusterInfo(id, requirement.getCreated_at()));
            }
        }

        int oldSize = model.getDocs().size();
        Tfidf.getInstance().deleteReqs(requirements,model);

        try {
            updateModel(compare, requirements,model,oldSize);
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }
    }

    private String findMaster(List<String> requirements, Map<String, ReqClusterInfo> reqCluster) {
        String master = requirements.get(0);
        for (String requirement: requirements) {
            master = chooseMaster(reqCluster, master, requirement);
        }
        return master;
    }

    @Override
    public void simReqProject(String responseId, String organization, double threshold, ReqProject projectRequirements) throws NotFoundException, InternalErrorException, BadRequestException {
        control.showInfoMessage("SimReqProject: Start computing");

        generateResponse(organization,responseId);

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

        reqProject(projectRequirements.getReqsToCompare(), projectRequirements.getProjectReqs(), model, threshold, organization, responseId, true);

        finishComputation(organization, responseId);
        control.showInfoMessage("SimReqProject: Finish computing");
    }

    private void reqProject(List<String> reqsToCompare, List<String> projectRequirements, Model model, double threshold, String organization, String responseId, boolean includeReqs) throws InternalErrorException {
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();

        int cont = 0;
        int pages = 0;
        long numberDependencies = 0;

        JSONArray array = new JSONArray();
        for (String req1: reqsToCompare) {
            if (model.getDocs().containsKey(req1)) {
                for (String req2 : projectRequirements) {
                    if (!req1.equals(req2) && model.getDocs().containsKey(req2)) {
                        double score = cosineSimilarity.compute(model.getDocs(), req1, req2);
                        if (score >= threshold) {
                            ++numberDependencies;
                            Dependency dependency = new Dependency(score, req1, req2, status, dependencyType, component);
                            array.put(dependency.toJSON());
                            ++cont;
                            if (cont >= maxDepsForPage) {
                                generateResponsePage(responseId, organization, pages, array, dependenciesArrayName);
                                ++pages;
                                array = new JSONArray();
                                cont = 0;
                            }
                        }
                    }
                }
                if (includeReqs) projectRequirements.add(req1);
            }
        }

        if (array.length() > 0) {
            generateResponsePage(responseId, organization, pages, array, dependenciesArrayName);
        }

        control.showInfoMessage("Number dependencies: " + numberDependencies);
    }

    @Override
    public void simProject(String responseId, String organization, double threshold, List<String> projectRequirements) throws NotFoundException, InternalErrorException {
        control.showInfoMessage("SimProject: Start computing");

        generateResponse(organization,responseId);

        Model model = null;
        try {
            model = loadModel(organization);
        } catch (NotFoundException e) {
            saveNotFoundException(organization, responseId, e);
        }

        project(projectRequirements, model, threshold, responseId, organization);

        finishComputation(organization, responseId);
        control.showInfoMessage("SimProject: Finish computing");
    }

    private void project(List<String> projectRequirements, Model model, double threshold, String responseId, String organization) throws InternalErrorException {
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();
        int cont = 0;
        int pages = 0;
        long numberDependencies = 0;

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
                            ++numberDependencies;
                            Dependency dependency = new Dependency(score, req1, req2, status, dependencyType, component);
                            array.put(dependency.toJSON());
                            ++cont;
                            if (cont >= maxDepsForPage) {
                                generateResponsePage(responseId, organization, pages, array, dependenciesArrayName);
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
            generateResponsePage(responseId, organization, pages, array, dependenciesArrayName);
        }

        control.showInfoMessage("Number dependencies: " + numberDependencies);
    }

    @Override
    public void buildClustersAndCompute(String responseId, String compare, String organization, double threshold, Clusters input) throws BadRequestException, InternalErrorException {
        control.showInfoMessage("BuildClustersAndCompute: Start computing");
        generateResponse(organization,responseId);

        List<Requirement> requirements = null;
        try {
            requirements = deleteDuplicates(input.getRequirements());
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }

        Model model = null;
        try {
            model = generateModel(compare, requirements);
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }

        Model iniClusters = computeIniClusters(input.getDependencies(), requirements);
        Map<String, List<String>> clusters = iniClusters.getClusters();
        Map<String, ReqClusterInfo> reqCluster = iniClusters.getReqCluster();

        List<String> projectRequirements = new ArrayList<>();
        List<String> requirementsToCompare = new ArrayList<>();

        Iterator it = clusters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String master = (String) pair.getKey();
            List<String> reqs = (List<String>) pair.getValue();
            if (reqs.size() > 1) projectRequirements.add(master);
            else requirementsToCompare.add(master);
        }

        model.setClusters(clusters);
        model.setReqCluster(reqCluster);

        reqProject(requirementsToCompare,projectRequirements,model,threshold, organization, responseId, true);

        computeClusterDependencies(organization, responseId, reqCluster, clusters);

        saveModel(organization, model);

        finishComputation(organization, responseId);

        control.showInfoMessage("BuildClustersAndCompute: Finish computing");
    }

    private void computeClusterDependencies(String organization, String responseId, Map<String, ReqClusterInfo> reqCluster, Map<String, List<String>> clusters) throws InternalErrorException {
        try {
            int totalPages = databaseModel.getTotalPages(organization, responseId);
            for (int i = 0; i < totalPages; ++i) {
                List<Dependency> dependencies = databaseModel.getResponsePage(organization, responseId, i);
                computeDependencies(dependencies, reqCluster, clusters);
            }
        } catch (NotFoundException e) {
            String message = "Error loading responses from the database";
            control.showErrorMessage(e.getMessage());
            try {
                databaseModel.saveResponsePage(organization, responseId, 0, createJsonException(500, internalErrorMessage, message)); //TODO throws PK error
                databaseModel.finishComputation(organization, responseId);
                throw new InternalErrorException(message);
            } catch (SQLException sq) {
                treatSQLException(sq);
            }
        } catch (SQLException sq) {
            treatSQLException(sq);
        }
    }

    @Override
    public void buildClusters(String responseId, String compare, String organization, Clusters input) throws BadRequestException, InternalErrorException {
        control.showInfoMessage("BuildClusters: Start computing");
        generateResponse(organization,responseId);

        List<Requirement> requirements = null;
        try {
            requirements = deleteDuplicates(input.getRequirements());
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }

        Model model = null;
        try {
            model = generateModel(compare, requirements);
        } catch (BadRequestException e) {
            saveBadRequestException(organization, responseId, e);
        }

        Model iniClusters = computeIniClusters(input.getDependencies(), requirements);

        JSONArray array = new JSONArray();
        int cont = 0;
        int pages = 0;
        Iterator it = iniClusters.getClusters().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String master = (String) pair.getKey();
            JSONObject aux = new JSONObject();
            aux.put("id", master);
            array.put(aux);
            ++cont;
            if (cont >= maxDepsForPage) {
                generateResponsePage(responseId, organization, pages, array, "requirements");
                ++pages;
                array = new JSONArray();
                cont = 0;
            }
        }

        if (array.length() > 0) {
            generateResponsePage(responseId, organization, pages, array, "requirements");
        }

        model = new Model(model.getDocs(), model.getCorpusFrequency(), iniClusters.getClusters(), iniClusters.getReqCluster());
        saveModel(organization, model);

        finishComputation(organization, responseId);

        control.showInfoMessage("BuildClusters: Finish computing");
    }

    private Model computeIniClusters(List<Dependency> dependencies, List<Requirement> requirements) {

        HashMap<String,ReqClusterInfo> reqCluster = new HashMap<>();
        HashMap<String,List<String>> clusters = new HashMap<>();

        for (Requirement requirement: requirements) {
            String id = requirement.getId();
            List<String> aux = new ArrayList<>();
            aux.add(id);
            clusters.put(id,aux);
            reqCluster.put(id,new ReqClusterInfo(id, requirement.getCreated_at()));
        }

        computeDependencies(dependencies, reqCluster, clusters);

        Model model = new Model();
        model.setClusters(clusters);
        model.setReqCluster(reqCluster);

        return model;
    }

    private void computeDependencies(List<Dependency> dependencies, Map<String,ReqClusterInfo> reqCluster, Map<String,List<String>> clusters) {
        for (Dependency dependency: dependencies) {
            if (validDependency(dependency)) {
                String fromid = dependency.getFromid();
                String toid = dependency.getToid();
                if (reqCluster.containsKey(fromid) && reqCluster.containsKey(toid)) {
                    mergeClusters(clusters, reqCluster, fromid, toid);
                }
            }
        }
    }

    private boolean validDependency(Dependency dependency) {
        String type = dependency.getDependencyType();
        return (type != null && (type.equals("similar") || type.equals("duplicates")));
    }

    private void mergeClusters(Map<String,List<String>> clusters, Map<String,ReqClusterInfo> reqCluster, String req1, String req2) {
        String masterReq1 = reqCluster.get(req1).getCluster();
        String masterReq2 = reqCluster.get(req2).getCluster();
        String master = chooseMaster(reqCluster, masterReq1, masterReq2);
        if (!masterReq1.equals(masterReq2)) {
            List<String> aux1 = clusters.get(masterReq1);
            List<String> aux2 = clusters.get(masterReq2);
            List<String> reqsArray = aux2;
            if (master.equals(masterReq2)) reqsArray = aux1;
            for (String req: reqsArray) {
                ReqClusterInfo aux = reqCluster.get(req);
                reqCluster.put(req, new ReqClusterInfo(master, aux.getDate()));
            }
            aux1.addAll(aux2);
            clusters.put(master, aux1);
            if (master.equals(masterReq1)) clusters.remove(masterReq2);
            else clusters.remove(masterReq1);
        }
    }

    private String chooseMaster(Map<String,ReqClusterInfo> reqCluster, String req1, String req2) {

        long req1Date = reqCluster.get(req1).getDate();
        long req2Date = reqCluster.get(req2).getDate();

        if (req1Date != 0 && req2Date != 0) return (req1Date <= req2Date) ? req1 : req2;
        else return (req2Date != 0) ? req2 : req1;
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
            Path path = Paths.get(SQLiteDatabase.getDbName());
            Files.delete(path);
            File file = new File(SQLiteDatabase.getDbName());
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

    private void generateResponsePage(String responseId, String organization, int pages, JSONArray array, String arrayName) throws InternalErrorException {
        JSONObject json = new JSONObject();
        if (pages == 0) json.put("status",200);
        json.put(arrayName,array);
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

    private void updateModel(String compare, List<Requirement> requirements, Model model, int oldSize) throws BadRequestException, InternalErrorException {
        Tfidf tfidf = Tfidf.getInstance();
        List<String> text = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        buildCorpus(compare,requirements,text,ids);
        tfidf.addNewReqsAndRecomputeModel(text,ids,model,oldSize);
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
