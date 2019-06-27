package upc.similarity.compareapi.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.config.Control;
import upc.similarity.compareapi.entity.*;
import upc.similarity.compareapi.entity.input.Clusters;
import upc.similarity.compareapi.entity.input.ReqProject;
import upc.similarity.compareapi.exception.BadRequestException;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.exception.NotFoundException;
import upc.similarity.compareapi.util.*;
import java.util.*;

@Service("comparerService")
public class CompareServiceImpl implements CompareService {

    private Control control = Control.getInstance();

    @Override
    public void buildModel(String responseId, String compare, String organization, List<Requirement> requirements) throws BadRequestException, InternalErrorException {
        control.showInfoMessage("BuildModel: Start computing");

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId);
        databaseOperations.saveModel(organization, generateModel(compare, deleteDuplicates(requirements,organization,responseId)));
        databaseOperations.generateEmptyResponse(organization, responseId);

        control.showInfoMessage("BuildModel: Finish computing");
    }

    @Override
    public void buildModelAndCompute(String responseId, String compare, String organization, double threshold, List<Requirement> requirements) throws BadRequestException, NotFoundException, InternalErrorException {
        control.showInfoMessage("BuildModelAndCompute: Start computing");

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId);

        Model model = generateModel(compare, deleteDuplicates(requirements,organization,responseId));
        List<String> requirementsIds = new ArrayList<>();
        for (Requirement requirement: requirements) {
            requirementsIds.add(requirement.getId());
        }

        project(requirementsIds, model, threshold, responseId, organization);
        databaseOperations.saveModel(organization, model);

        databaseOperations.finishComputation(organization, responseId);

        control.showInfoMessage("BuildModelAndCompute: Finish computing");
    }

    @Override
    public void addRequirements(String responseId, String compare, String organization, List<Requirement> requirements) throws InternalErrorException, BadRequestException, NotFoundException {
        control.showInfoMessage("AddRequirements: Start computing");

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId);

        Model model = null;
        try {
            model = databaseOperations.loadModel(organization, true);
        } catch (NotFoundException e) {
            databaseOperations.saveNotFoundException(organization, responseId, e);
        }

        List<Requirement> notDuplicatedRequirements = deleteDuplicates(requirements,organization,responseId);
        addRequirementsToModel(notDuplicatedRequirements, compare, model);
        databaseOperations.saveModel(organization, model);
        databaseOperations.generateEmptyResponse(organization, responseId);

        control.showInfoMessage("AddRequirements: Finish computing");
    }

    @Override
    public void deleteRequirements(String responseId, String organization, List<Requirement> requirements) throws InternalErrorException, BadRequestException, NotFoundException {
        control.showInfoMessage("DeleteRequirements: Start computing");

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId);

        Model model = null;
        try {
            model = databaseOperations.loadModel(organization, true);
        } catch (NotFoundException e) {
            databaseOperations.saveNotFoundException(organization, responseId, e);
        }

        List<Requirement> notDuplicatedRequirements = deleteDuplicates(requirements,organization,responseId);

        if (model.hasClusters()) {
            /*Map<String, List<String>> clusters = model.getClusters();
            Map<String, ReqClusterInfo> reqCluster = model.getReqCluster();

            for (Requirement requirement: notDuplicatedRequirements) {
                String id = requirement.getId();
                deleteReqFromClusters(id, clusters, reqCluster);
            }*/
        }

        Tfidf.getInstance().deleteReqsAndRecomputeModel(notDuplicatedRequirements,model);
        databaseOperations.saveModel(organization, model);
        databaseOperations.generateEmptyResponse(organization, responseId);

        control.showInfoMessage("DeleteRequirements: Finish computing");
    }


    @Override
    public Dependency simReqReq(String organization, String req1, String req2) throws NotFoundException, InternalErrorException {
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        Constants constants = Constants.getInstance();
        Model model = databaseOperations.loadModel(organization,false);
        if (!model.getDocs().containsKey(req1)) throw new NotFoundException("The requirement with id " + req1 + " is not present in the model loaded form the database");
        if (!model.getDocs().containsKey(req2)) throw new NotFoundException("The requirement with id " + req2 + " is not present in the model loaded form the database");
        double score = cosineSimilarity.compute(model.getDocs(),req1,req2);
        return new Dependency(score,req1,req2,constants.getStatus(),constants.getDependencyType(),constants.getComponent());
    }

    @Override
    public void simReqOrganization(String responseId, String compare, String organization, double threshold, List<Requirement> requirements) throws NotFoundException, InternalErrorException, BadRequestException {
        control.showInfoMessage("SimReqOrganization: Start computing");
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId);

        Model model = null;
        try {
            model = databaseOperations.loadModel(organization,true);
        } catch (NotFoundException e) {
            databaseOperations.saveNotFoundException(organization, responseId, e);
        }

        List<Requirement> notDuplicatedRequirements = deleteDuplicates(requirements,organization,responseId);

        addRequirementsToModel(notDuplicatedRequirements, compare, model);
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

        reqProject(requirementsToCompare, projectRequirements, model, threshold, organization, responseId);
        databaseOperations.saveModel(organization, model);

        databaseOperations.finishComputation(organization, responseId);

        control.showInfoMessage("SimReqOrganization: Finish computing");
    }

    @Override
    public String simReqClusters(String responseId, String compare, String organization, double threshold, List<Requirement> requirements) throws NotFoundException, InternalErrorException, BadRequestException {
        control.showInfoMessage("SimReqClusters: Start computing");

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        Constants constants = Constants.getInstance();
        ClusterOperations clusterOperations = ClusterOperations.getInstance();

        Model model = databaseOperations.loadModel(organization,true);
        if (!model.hasClusters()) throw new BadRequestException("The model does not have clusters");
        List<Requirement> notDuplicatedRequirements = deleteDuplicates(requirements,null,null);
        addRequirementsToModel(notDuplicatedRequirements, compare, model);
        databaseOperations.saveModel(organization, model);

        List<String> requirementsToCompare = new ArrayList<>();
        for (Requirement requirement: notDuplicatedRequirements) requirementsToCompare.add(requirement.getId());
        JSONObject result = new JSONObject();
        result.put(constants.getDependenciesArrayName(), clusterOperations.reqClustersNotDb(requirementsToCompare, model.getDocs(), model.getClusters(), threshold));

        control.showInfoMessage("SimReqClusters: Finish computing");
        return result.toString();
    }

    @Override
    public void simReqProject(String responseId, String organization, double threshold, ReqProject projectRequirements) throws NotFoundException, InternalErrorException, BadRequestException {
        control.showInfoMessage("SimReqProject: Start computing");
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();

        databaseOperations.generateResponse(organization,responseId);

        Model model = null;
        try {
            model = databaseOperations.loadModel(organization,false);
            for (String req: projectRequirements.getReqsToCompare()) {
                if (projectRequirements.getProjectReqs().contains(req)) throw new BadRequestException("The requirement with id " + req + " is already inside the project");
            }
        } catch (NotFoundException e) {
            databaseOperations.saveNotFoundException(organization, responseId, e);
        } catch (BadRequestException e) {
            databaseOperations.saveBadRequestException(organization, responseId, e);
        }

        reqProject(projectRequirements.getReqsToCompare(), projectRequirements.getProjectReqs(), model, threshold, organization, responseId);

        databaseOperations.finishComputation(organization, responseId);
        control.showInfoMessage("SimReqProject: Finish computing");
    }

    @Override
    public void simProject(String responseId, String organization, double threshold, List<String> projectRequirements) throws NotFoundException, InternalErrorException {
        control.showInfoMessage("SimProject: Start computing");
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();

        databaseOperations.generateResponse(organization,responseId);

        Model model = null;
        try {
            model = databaseOperations.loadModel(organization,false);
        } catch (NotFoundException e) {
            databaseOperations.saveNotFoundException(organization, responseId, e);
        }

        project(projectRequirements, model, threshold, responseId, organization);

        databaseOperations.finishComputation(organization, responseId);
        control.showInfoMessage("SimProject: Finish computing");
    }

    @Override
    public void buildClustersAndCompute(String responseId, String compare, String organization, double threshold, Clusters input) throws BadRequestException, InternalErrorException {
        control.showInfoMessage("BuildClustersAndCompute: Start computing");

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();

        databaseOperations.generateResponse(organization,responseId);
        List<Requirement> requirements = deleteDuplicates(input.getRequirements(),organization,responseId);
        Model model = generateModel(compare, requirements);
        ClusterOperations clusterOperations = ClusterOperations.getInstance();
        ClusterAndDeps iniClusters = clusterOperations.computeIniClusters(input.getDependencies(), requirements);

        model = new Model(model.getDocs(), model.getCorpusFrequency(), iniClusters.getClusters(), iniClusters.getDependencies());
        databaseOperations.saveModel(organization, model);

        Map<String,Integer> reqCluster = iniClusters.getReqCluster();
        List<String> reqsToCompare = new ArrayList<>();
        for (Requirement requirement: requirements) {
            String id = requirement.getId();
            if (reqCluster.get(id) == -1) reqsToCompare.add(id);
        }

        clusterOperations.reqClusters(organization, responseId, reqsToCompare, model.getDocs(), model.getClusters(), threshold);

        databaseOperations.finishComputation(organization, responseId);

        control.showInfoMessage("BuildClustersAndCompute: Finish computing");
    }

    @Override
    public void buildClusters(String responseId, String compare, String organization, Clusters input) throws BadRequestException, InternalErrorException {
        control.showInfoMessage("BuildClusters: Start computing");

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();

        databaseOperations.generateResponse(organization,responseId);
        List<Requirement> requirements = deleteDuplicates(input.getRequirements(),organization,responseId);
        Model model = generateModel(compare, requirements);
        ClusterOperations clusterOperations = ClusterOperations.getInstance();
        ClusterAndDeps iniClusters = clusterOperations.computeIniClusters(input.getDependencies(), requirements);

        model = new Model(model.getDocs(), model.getCorpusFrequency(), iniClusters.getClusters(), iniClusters.getDependencies());
        databaseOperations.saveModel(organization, model);

        databaseOperations.generateEmptyResponse(organization, responseId);

        control.showInfoMessage("BuildClusters: Finish computing");
    }

    @Override
    public String getResponsePage(String organization, String responseId) throws NotFoundException, InternalErrorException, NotFinishedException {
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        return databaseOperations.getResponsePage(organization, responseId);
    }

    @Override
    public void clearOrganizationResponses(String organization) throws InternalErrorException, NotFoundException {
        DatabaseOperations.getInstance().clearOrganizationResponses(organization);
    }

    @Override
    public void clearDatabase() throws InternalErrorException {
        DatabaseOperations.getInstance().clearDatabase();
    }



    /*
    auxiliary operations
     */
    private void reqProject(List<String> reqsToCompare, List<String> projectRequirements, Model model, double threshold, String organization, String responseId) throws InternalErrorException {
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        Constants constants = Constants.getInstance();

        int cont = 0;
        long numberDependencies = 0;

        JSONArray array = new JSONArray();
        for (String req1: reqsToCompare) {
            if (model.getDocs().containsKey(req1)) {
                for (String req2 : projectRequirements) {
                    if (!req1.equals(req2) && model.getDocs().containsKey(req2)) {
                        double score = cosineSimilarity.compute(model.getDocs(), req1, req2);
                        if (score >= threshold) {
                            ++numberDependencies;
                            Dependency dependency = new Dependency(score, req1, req2, constants.getStatus(), constants.getDependencyType(), constants.getComponent());
                            array.put(dependency.toJSON());
                            ++cont;
                            if (cont >= constants.getMaxDepsForPage()) {
                                databaseOperations.generateResponsePage(responseId, organization, array, constants.getDependenciesArrayName());
                                array = new JSONArray();
                                cont = 0;
                            }
                        }
                    }
                }
                projectRequirements.add(req1);
            }
        }

        if (array.length() > 0) {
            databaseOperations.generateResponsePage(responseId, organization, array, constants.getDependenciesArrayName());
        }

        control.showInfoMessage("Number dependencies: " + numberDependencies);
    }


    private void project(List<String> projectRequirements, Model model, double threshold, String responseId, String organization) throws InternalErrorException {
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        Constants constants = Constants.getInstance();
        int cont = 0;
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
                            Dependency dependency = new Dependency(score, req1, req2, constants.getStatus(), constants.getDependencyType(), constants.getComponent());
                            array.put(dependency.toJSON());
                            ++cont;
                            if (cont >= constants.getMaxDepsForPage()) {
                                databaseOperations.generateResponsePage(responseId, organization, array, constants.getDependenciesArrayName());
                                array = new JSONArray();
                                cont = 0;
                            }
                        }
                    }
                }
            }
        }

        if (array.length() > 0) {
            databaseOperations.generateResponsePage(responseId, organization, array, constants.getDependenciesArrayName());
        }

        control.showInfoMessage("Number dependencies: " + numberDependencies);
    }

    private void addRequirementsToModel(List<Requirement> requirements, String compare, Model model) throws InternalErrorException {

        if (model.hasClusters()) {
            Map<String, List<String>> clusters = model.getClusters();
            Map<String, ReqClusterInfo> reqCluster = model.getReqCluster();

            for (Requirement requirement: requirements) {
                String id = requirement.getId();
                deleteReqFromClusters(id, clusters, reqCluster);
                List<String> aux = new ArrayList<>();
                aux.add(id);
                clusters.put(id,aux);
                reqCluster.put(id, new ReqClusterInfo(id, requirement.getCreatedAt()));
            }
        }

        int oldSize = model.getDocs().size();
        Tfidf.getInstance().deleteReqs(requirements,model);

        updateModel(compare, requirements,model,oldSize);
    }

    private List<Requirement> deleteDuplicates(List<Requirement> requirements, String organization, String responseId) throws BadRequestException, InternalErrorException {
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        HashSet<String> ids = new HashSet<>();
        List<Requirement> result = new ArrayList<>();
        try {
            for (Requirement requirement : requirements) {
                if (requirement.getId() == null) throw new BadRequestException("There is a requirement without id.");
                if (!ids.contains(requirement.getId())) {
                    result.add(requirement);
                    ids.add(requirement.getId());
                }
            }
        } catch (BadRequestException e) {
            if (organization != null && responseId != null) databaseOperations.saveBadRequestException(organization, responseId, e);
            else throw e;
        }
        return result;
    }

    private Model generateModel(String compare, List<Requirement> requirements) throws InternalErrorException {
        Tfidf tfidf = Tfidf.getInstance();
        Map<String, Integer> corpusFrequency = new HashMap<>();
        List<String> text = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        buildCorpus(compare,requirements,text,ids);
        Map<String, Map<String, Double>> docs = tfidf.extractKeywords(text,ids,corpusFrequency);
        return new Model(docs,corpusFrequency);
    }

    private void updateModel(String compare, List<Requirement> requirements, Model model, int oldSize) throws InternalErrorException {
        Tfidf tfidf = Tfidf.getInstance();
        List<String> text = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        buildCorpus(compare,requirements,text,ids);
        tfidf.addNewReqsAndRecomputeModel(text,ids,model,oldSize);
    }

    private void buildCorpus(String compare, List<Requirement> requirements, List<String> arrayText, List<String> arrayIds) {
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

    public String TestAccuracy(String compare, Clusters input) {
        return TestMethods.getInstance().TestAccuracy(compare, input);
    }

    public String extractModel(String compare, String organization, Clusters input) {
        return TestMethods.getInstance().extractModel(compare, organization, input);
    }
}
