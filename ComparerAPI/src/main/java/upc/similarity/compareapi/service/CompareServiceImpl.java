package upc.similarity.compareapi.service;

import org.json.JSONArray;
import org.springframework.stereotype.Service;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.config.Control;
import upc.similarity.compareapi.entity.*;
import upc.similarity.compareapi.entity.auxiliary.ClusterAndDeps;
import upc.similarity.compareapi.entity.auxiliary.OrderedObject;
import upc.similarity.compareapi.entity.input.Clusters;
import upc.similarity.compareapi.entity.input.ReqProject;
import upc.similarity.compareapi.entity.output.Dependencies;
import upc.similarity.compareapi.exception.*;
import upc.similarity.compareapi.util.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service("comparerService")
public class CompareServiceImpl implements CompareService {

    private Control control = Control.getInstance();
    private ConcurrentHashMap<String, Lock> organizationLocks = new ConcurrentHashMap<>();


    /*
    Similarity without clusters
     */

    @Override
    public void buildModel(String responseId, boolean compare, String organization, List<Requirement> requirements) throws BadRequestException, NotFinishedException, InternalErrorException {
        control.showInfoMessage("BuildModel: Start computing " + organization + " " + responseId);

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId,"BuildModel");
        getAccessToUpdate(organization, responseId);
        try {
            //threshold is never used in other methods
            databaseOperations.saveModel(organization, responseId, generateModel(compare, 0, deleteDuplicates(requirements, organization, responseId)), null);
        } finally {
            releaseAccessToUpdate(organization, responseId);
        }
        databaseOperations.generateEmptyResponse(organization, responseId);

        control.showInfoMessage("BuildModel: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void buildModelAndCompute(String responseId, boolean compare, String organization, double threshold, List<Requirement> requirements) throws BadRequestException, NotFinishedException, InternalErrorException {
        control.showInfoMessage("BuildModelAndCompute: Start computing " + organization + " " + responseId);

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId,"BuildModelAndCompute");

        //threshold is never used in other methods
        Model model = generateModel(compare, 0, deleteDuplicates(requirements,organization,responseId));
        List<String> requirementsIds = new ArrayList<>();
        for (Requirement requirement: requirements) {
            requirementsIds.add(requirement.getId());
        }

        getAccessToUpdate(organization, responseId);
        try {
            databaseOperations.saveModel(organization, responseId, model, null);
        } finally {
            releaseAccessToUpdate(organization, responseId);
        }

        project(requirementsIds, model, threshold, responseId, organization);

        databaseOperations.finishComputation(organization, responseId);

        control.showInfoMessage("BuildModelAndCompute: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void addRequirements(String responseId, String organization, List<Requirement> requirements) throws BadRequestException, NotFoundException, NotFinishedException, InternalErrorException {
        control.showInfoMessage("AddRequirements: Start computing " + organization + " " + responseId);

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId,"AddRequirements");

        getAccessToUpdate(organization, responseId);

        try {
            Model model = databaseOperations.loadModel(organization, responseId, true);
            List<Requirement> notDuplicatedRequirements = deleteDuplicates(requirements, organization, responseId);
            addRequirementsToModel(notDuplicatedRequirements, model);
            databaseOperations.saveModel(organization, responseId, model, null);
        } finally {
            releaseAccessToUpdate(organization, responseId);
        }

        databaseOperations.generateEmptyResponse(organization, responseId);

        control.showInfoMessage("AddRequirements: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void deleteRequirements(String responseId, String organization, List<Requirement> requirements) throws BadRequestException, NotFoundException, NotFinishedException, InternalErrorException  {
        control.showInfoMessage("DeleteRequirements: Start computing " + organization + " " + responseId);

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId,"DeleteRequirements");

        getAccessToUpdate(organization, responseId);

        try {
            Model model = databaseOperations.loadModel(organization, responseId, true);
            List<Requirement> notDuplicatedRequirements = deleteDuplicates(requirements, organization, responseId);
            List<String> requirementsIds = new ArrayList<>();
            for (Requirement requirement : notDuplicatedRequirements) requirementsIds.add(requirement.getId());
            Tfidf.getInstance().deleteReqsAndRecomputeModel(requirementsIds, model);
            databaseOperations.saveModel(organization, responseId, model, null);
        } finally {
            releaseAccessToUpdate(organization, responseId);
        }

        databaseOperations.generateEmptyResponse(organization, responseId);

        control.showInfoMessage("DeleteRequirements: Finish computing " + organization + " " + responseId);
    }


    @Override
    public Dependency simReqReq(String organization, String req1, String req2) throws NotFoundException, InternalErrorException {
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        Constants constants = Constants.getInstance();
        Model model = databaseOperations.loadModel(organization,null, false);
        if (!model.getDocs().containsKey(req1)) throw new NotFoundException("The requirement with id " + req1 + " is not present in the model loaded form the database");
        if (!model.getDocs().containsKey(req2)) throw new NotFoundException("The requirement with id " + req2 + " is not present in the model loaded form the database");
        double score = cosineSimilarity.compute(model.getDocs(),req1,req2);
        return new Dependency(score,req1,req2,constants.getStatus(),constants.getDependencyType(),constants.getComponent());
    }

    @Override
    public void simReqOrganization(String responseId, String organization, double threshold, List<String> requirements) throws NotFoundException, NotFinishedException, BadRequestException, InternalErrorException {
        control.showInfoMessage("SimReqOrganization: Start computing " + organization + " " + responseId);
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId,"SimReqOrganization");

        getAccessToUpdate(organization, responseId);

        try {
            Model model = databaseOperations.loadModel(organization, responseId, true);

            HashSet<String> repeatedHash = new HashSet<>();
            List<String> projectRequirements = new ArrayList<>();
            List<String> requirementsToCompare = new ArrayList<>();
            for (String requirement : requirements) {
                requirementsToCompare.add(requirement);
                repeatedHash.add(requirement);
            }

            Iterator it = model.getDocs().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                String id = (String) pair.getKey();
                if (!repeatedHash.contains(id)) projectRequirements.add(id);
            }

            reqProject(requirementsToCompare, projectRequirements, model, threshold, organization, responseId);
            databaseOperations.saveModel(organization, responseId, model, null);
        } finally {
            releaseAccessToUpdate(organization, responseId);
        }

        databaseOperations.finishComputation(organization, responseId);

        control.showInfoMessage("SimReqOrganization: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void simNewReqOrganization(String responseId, String organization, double threshold, List<Requirement> requirements) throws NotFoundException, NotFinishedException, BadRequestException, InternalErrorException {
        control.showInfoMessage("SimReqOrganization: Start computing " + organization + " " + responseId);
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId,"SimReqOrganization");

        getAccessToUpdate(organization, responseId);

        try {
            Model model = databaseOperations.loadModel(organization, responseId, true);

            List<Requirement> notDuplicatedRequirements = deleteDuplicates(requirements, organization, responseId);

            addRequirementsToModel(notDuplicatedRequirements, model);
            HashSet<String> repeatedHash = new HashSet<>();
            for (Requirement requirement : notDuplicatedRequirements) repeatedHash.add(requirement.getId());

            List<String> projectRequirements = new ArrayList<>();
            List<String> requirementsToCompare = new ArrayList<>();
            for (Requirement requirement : notDuplicatedRequirements) requirementsToCompare.add(requirement.getId());

            Iterator it = model.getDocs().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                String id = (String) pair.getKey();
                if (!repeatedHash.contains(id)) projectRequirements.add(id);
            }

            reqProject(requirementsToCompare, projectRequirements, model, threshold, organization, responseId);
            databaseOperations.saveModel(organization, responseId, model, null);
        } finally {
            releaseAccessToUpdate(organization, responseId);
        }

        databaseOperations.finishComputation(organization, responseId);

        control.showInfoMessage("SimReqOrganization: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void simReqProject(String responseId, String organization, double threshold, ReqProject projectRequirements) throws NotFoundException, InternalErrorException, BadRequestException {
        control.showInfoMessage("SimReqProject: Start computing " + organization + " " + responseId);
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();

        databaseOperations.generateResponse(organization,responseId,"SimReqProject");

        Model model = databaseOperations.loadModel(organization, responseId, false);
        for (String req: projectRequirements.getReqsToCompare()) {
            if (projectRequirements.getProjectReqs().contains(req)) databaseOperations.saveBadRequestException(organization, responseId, new BadRequestException("The requirement with id " + req + " is already inside the project"));
        }

        reqProject(projectRequirements.getReqsToCompare(), projectRequirements.getProjectReqs(), model, threshold, organization, responseId);

        databaseOperations.finishComputation(organization, responseId);
        control.showInfoMessage("SimReqProject: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void simProject(String responseId, String organization, double threshold, List<String> projectRequirements) throws NotFoundException, InternalErrorException {
        control.showInfoMessage("SimProject: Start computing " + organization + " " + responseId);
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();

        databaseOperations.generateResponse(organization,responseId,"SimProject");

        Model model = databaseOperations.loadModel(organization, responseId, false);

        project(projectRequirements, model, threshold, responseId, organization);

        databaseOperations.finishComputation(organization, responseId);
        control.showInfoMessage("SimProject: Finish computing " + organization + " " + responseId);
    }


    /*
    Similarity with clusters
     */

    @Override
    public void buildClusters(String responseId, boolean compare, double threshold, String organization, Clusters input) throws BadRequestException, NotFinishedException, InternalErrorException {
        control.showInfoMessage("BuildClusters: Start computing " + organization + " " + responseId + " " + input.getRequirements().size() + " reqs");

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();

        if (!input.inputOk()) databaseOperations.saveBadRequestException(organization, responseId, new BadRequestException("The input requirements array is empty"));

        databaseOperations.generateResponse(organization,responseId,"BuildClusters");
        List<Requirement> requirements = deleteDuplicates(input.getRequirements(),organization,responseId);
        Model model = generateModel(compare, threshold, requirements);
        ClusterOperations clusterOperations = ClusterOperations.getInstance();
        ClusterAndDeps iniClusters = clusterOperations.computeIniClusters(input.getDependencies(), requirements);

        model = new Model(model.getDocs(), model.getCorpusFrequency(), model.getThreshold(), model.isCompare(), iniClusters.getLastClusterId(), iniClusters.getClusters());
        getAccessToUpdate(organization, responseId);
        try {
            databaseOperations.saveModel(organization, responseId, model, iniClusters.getDependencies());
            databaseOperations.createDepsAuxiliaryTable(organization, null);
            clusterOperations.computeProposedDependencies(organization, responseId,  model.getDocs().keySet(), model.getClusters().keySet(), model, true);
            databaseOperations.updateModelClustersAndDependencies(organization, null, model, null, true);
        } finally {
            releaseAccessToUpdate(organization, responseId);
        }

        databaseOperations.generateEmptyResponse(organization, responseId);

        control.showInfoMessage("BuildClusters: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void buildClustersAndCompute(String responseId, boolean compare, String organization, double threshold, int maxNumber, Clusters input) throws BadRequestException, NotFinishedException, InternalErrorException {
        control.showInfoMessage("BuildClustersAndCompute: Start computing " + organization + " " + responseId + " " + input.getRequirements().size() + " reqs");

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();

        if (!input.inputOk()) databaseOperations.saveBadRequestException(organization, responseId, new BadRequestException("The input requirements array is empty"));

        databaseOperations.generateResponse(organization,responseId,"BuildClustersAndCompute");
        List<Requirement> requirements = deleteDuplicates(input.getRequirements(),organization,responseId);

        Model model = generateModel(compare, threshold, requirements);
        ClusterOperations clusterOperations = ClusterOperations.getInstance();
        ClusterAndDeps iniClusters = clusterOperations.computeIniClusters(input.getDependencies(), requirements);

        model = new Model(model.getDocs(), model.getCorpusFrequency(), model.getThreshold(), model.isCompare(), iniClusters.getLastClusterId(), iniClusters.getClusters());
        getAccessToUpdate(organization, responseId);
        try {
            databaseOperations.saveModel(organization, responseId, model, iniClusters.getDependencies());
            databaseOperations.createDepsAuxiliaryTable(organization, null);
            clusterOperations.computeProposedDependencies(organization, responseId,  model.getDocs().keySet(), model.getClusters().keySet(), model, true);
            databaseOperations.updateModelClustersAndDependencies(organization, null, model, null, true);
        } finally {
            releaseAccessToUpdate(organization, responseId);
        }

        int cont = 0;
        JSONArray array = new JSONArray();
        Constants constants = Constants.getInstance();
        HashSet<String> repeated = new HashSet<>();
        long numberDependencies = 0;
        for (Requirement requirement: requirements) {
            String id = requirement.getId();
            List<Dependency> dependencies = databaseOperations.getReqDepedencies(organization, null, id, "accepted", false);
            List<Dependency> proposedDependencies = databaseOperations.getReqDepedencies(organization, null, id, "proposed", false);
            proposedDependencies.sort(Comparator.comparing(Dependency::getDependencyScore).reversed());
            int highIndex = maxNumber;
            if (maxNumber < 0 || maxNumber > proposedDependencies.size()) highIndex = proposedDependencies.size();
            dependencies.addAll(proposedDependencies.subList(0, highIndex));
            for (Dependency dependency: dependencies) {
                Dependency aux = new Dependency(dependency.getDependencyScore(),id,dependency.getToid(),dependency.getStatus(), constants.getDependencyType(), constants.getComponent());
                if (!repeated.contains(aux.getFromid()+aux.getToid())) {
                    array.put(aux.toJSON());
                    ++numberDependencies;
                    ++cont;
                    if (cont >= constants.getMaxDepsForPage()) {
                        databaseOperations.generateResponsePage(responseId, organization, array, constants.getDependenciesArrayName());
                        array = new JSONArray();
                        cont = 0;
                    }
                    repeated.add(aux.getFromid()+aux.getToid());
                    repeated.add(aux.getToid()+aux.getFromid());
                }
            }
        }

        if (array.length() == 0 && numberDependencies == 0) databaseOperations.generateResponsePage(responseId, organization, array, constants.getDependenciesArrayName());

        if (array.length() > 0) {
            databaseOperations.generateResponsePage(responseId, organization, array, constants.getDependenciesArrayName());
        }

        databaseOperations.finishComputation(organization, responseId);

        control.showInfoMessage("BuildClustersAndCompute: Finish computing " + organization + " " + responseId);
    }

    @Override
    public Dependencies simReqClusters(String organization, List<String> requirements, int maxNumber) throws NotFoundException, InternalErrorException {
        control.showInfoMessage("SimReqClusters: Start computing");
        List<Dependency> result = new ArrayList<>();

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        if (!databaseOperations.existsOrganization(null, organization)) throw new NotFoundException("The organization with id " + organization + " does not exist");
        Constants constants = Constants.getInstance();
        HashSet<String> repeated = new HashSet<>();

        for (String id: requirements) {
            if (!databaseOperations.existReqInOrganizationModel(organization, null, id)) throw new NotFoundException("The requirement with id " + id + " is not inside the organization's model");
            List<Dependency> dependencies = databaseOperations.getReqDepedencies(organization, null, id, "accepted", false);
            List<Dependency> proposedDependencies = databaseOperations.getReqDepedencies(organization, null, id, "proposed", false);
            proposedDependencies.sort(Comparator.comparing(Dependency::getDependencyScore).reversed());
            int highIndex = maxNumber;
            if (maxNumber < 0 || maxNumber > proposedDependencies.size()) highIndex = proposedDependencies.size();
            dependencies.addAll(proposedDependencies.subList(0, highIndex));
            for (Dependency dependency : dependencies) {
                Dependency aux = new Dependency(dependency.getDependencyScore(), id, dependency.getToid(), dependency.getStatus(), constants.getDependencyType(), constants.getComponent());
                if (!repeated.contains(aux.getFromid() + aux.getToid())) {
                    result.add(aux);
                    repeated.add(aux.getFromid() + aux.getToid());
                    repeated.add(aux.getToid() + aux.getFromid());
                }
            }
        }

        control.showInfoMessage("SimReqClusters: Finish computing");
        return new Dependencies(result);
    }

    @Override
    public void treatAcceptedAndRejectedDependencies(String organization, List<Dependency> dependencies) throws NotFoundException, BadRequestException, NotFinishedException, InternalErrorException {
        control.showInfoMessage("TreatAcceptedAndRejectedDependencies: Start computing");

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        getAccessToUpdate(organization, null);
        try {
            Model model = databaseOperations.loadModel(organization, null, false);
            if (!model.hasClusters()) throw new BadRequestException("The model does not have clusters");

            dependencies.sort(Comparator.comparing(Dependency::computeTime));
            HashSet<Integer> clustersChanged = new HashSet<>();
            Map<String,Integer> reqCluster = computeReqClusterMap(model.getClusters(), model.getDocs().keySet());
            ClusterOperations clusterOperations = ClusterOperations.getInstance();
            databaseOperations.createDepsAuxiliaryTable(organization, null);

            for (Dependency dependency: dependencies) {
                if (dependency.getDependencyType() != null && dependency.getStatus() != null && dependency.getDependencyType().equals("similar")) {
                    String status = dependency.getStatus();
                    List<Dependency> aux = new ArrayList<>();
                    aux.add(dependency);
                    if (status.equals("accepted")) {
                        clusterOperations.addAcceptedDependencies(organization, null, aux, model, clustersChanged, reqCluster);
                    } else if (status.equals("rejected")) {
                        clusterOperations.addRejectedDependencies(organization, null, aux, model, clustersChanged, reqCluster);
                    }
                }
            }
            clusterOperations.updateProposedDependencies(organization, null, model, clustersChanged, true);
            databaseOperations.updateModelClustersAndDependencies(organization, null, model, null, true);
        } finally {
            releaseAccessToUpdate(organization, null);
        }

        control.showInfoMessage("TreatAcceptedAndRejectedDependencies: Finish computing");
    }

    @Override
    public void batchProcess(String responseId, String organization, Clusters input) throws BadRequestException, NotFoundException, NotFinishedException, InternalErrorException {
        control.showInfoMessage("BatchProcess: Start computing " + organization + " " + responseId);
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        ClusterOperations clusterOperations = ClusterOperations.getInstance();

        databaseOperations.generateResponse(organization,responseId,"BatchProcess");


        getAccessToUpdate(organization, responseId);

        long timeDeletedDependencies = 0;
        long timeAcceptedDependencies = 0;
        long timeAddRequirements = 0;
        long timeUpdatedProposedDependencies = 0;

        try {
            Model model = databaseOperations.loadModel(organization, responseId, true);

            if (!model.hasClusters()) databaseOperations.saveBadRequestException(organization, responseId, new BadRequestException("The model does not have clusters"));

            Map<String,Map<String,Double>> docs = model.getDocs();

            List<OrderedObject> objects = new ArrayList<>();

            Set<String> notRepeatedReqs = new HashSet<>();

            for (Requirement requirement : input.getRequirements()) {
                String id = requirement.getId();
                if (id != null && !notRepeatedReqs.contains(id)) {
                    notRepeatedReqs.add(id);
                    if (docs.containsKey(id)) {
                        if (requirementUpdated(organization, responseId, requirement, model)) objects.add(new OrderedObject(null, requirement, requirement.getTime()));
                    } else objects.add(new OrderedObject(null, requirement, requirement.getTime()));
                }
            }

            for (Dependency dependency : input.getDependencies()) {
                String status = dependency.getStatus();
                if (dependency.getDependencyType() != null && dependency.getStatus() != null
                        && dependency.getFromid() != null && dependency.getToid() != null
                        && dependency.getDependencyType().equals("similar")
                        && (status.equals("accepted") || status.equals("rejected"))) {
                    objects.add(new OrderedObject(dependency, null, dependency.computeTime()));
                }
            }

            databaseOperations.createDepsAuxiliaryTable(organization, null);
            objects.sort(Comparator.comparing(OrderedObject::getTime));
            HashSet<Integer> clustersChanged = new HashSet<>();
            Map<String,Integer> reqCluster = computeReqClusterMap(model.getClusters(), model.getDocs().keySet());

            Time time = Time.getInstance();

            for (OrderedObject orderedObject: objects) {
                if (orderedObject.isDependency()) {
                    List<Dependency> aux = new ArrayList<>();
                    Dependency dependency = orderedObject.getDependency();
                    aux.add(dependency);
                    if (dependency.getStatus().equals("accepted")) {
                        long auxTime = time.getCurrentMillis();
                        clusterOperations.addAcceptedDependencies(organization, responseId, aux, model, clustersChanged, reqCluster);
                        auxTime = time.getCurrentMillis() - auxTime;
                        timeAcceptedDependencies += auxTime;
                    }
                    else {
                        long auxTime = time.getCurrentMillis();
                        clusterOperations.addRejectedDependencies(organization, responseId, aux, model, clustersChanged, reqCluster);
                        auxTime = time.getCurrentMillis() - auxTime;
                        timeDeletedDependencies += auxTime;
                    }
                } else {
                    List<Requirement> aux = new ArrayList<>();
                    Requirement requirement = orderedObject.getRequirement();
                    aux.add(requirement);
                    long auxTime = time.getCurrentMillis();
                    clusterOperations.addRequirementsToClusters(organization, responseId, aux, model, clustersChanged, reqCluster);
                    addRequirementsToModel(aux, model);
                    auxTime = time.getCurrentMillis() - auxTime;
                    timeAddRequirements += auxTime;
                }
            }

            long auxTime = time.getCurrentMillis();
            clusterOperations.updateProposedDependencies(organization, responseId, model, clustersChanged, true);
            auxTime = time.getCurrentMillis() - auxTime;
            timeUpdatedProposedDependencies += auxTime;

            databaseOperations.updateModelClustersAndDependencies(organization, responseId, model, null, true);

        } finally {
            releaseAccessToUpdate(organization, responseId);
        }

        databaseOperations.generateEmptyResponse(organization, responseId);
        control.showInfoMessage("BatchProcess: Finish computing " + organization + " " + responseId + " " + timeAcceptedDependencies + " " + timeDeletedDependencies + " " + timeAddRequirements + " " + timeUpdatedProposedDependencies);
    }


    /*
    Auxiliary methods
     */

    @Override
    public String getResponsePage(String organization, String responseId) throws NotFoundException, InternalErrorException, NotFinishedException {
        return DatabaseOperations.getInstance().getResponsePage(organization, responseId);
    }

    @Override
    public Organization getOrganizationInfo(String organization) throws NotFoundException, InternalErrorException {
        return DatabaseOperations.getInstance().getOrganizationInfo(organization);
    }

    @Override
    public void clearOrganizationResponses(String organization) throws InternalErrorException, NotFoundException {
        DatabaseOperations.getInstance().clearOrganizationResponses(organization);
    }

    @Override
    public void clearOrganization(String organization) throws NotFoundException, NotFinishedException, InternalErrorException {
        getAccessToUpdate(organization, null);
        try {
            DatabaseOperations.getInstance().clearOrganization(organization);
        } finally {
            releaseAccessToUpdate(organization, null);
        }
    }

    @Override
    public void clearDatabase() throws InternalErrorException {
        DatabaseOperations.getInstance().clearDatabase();
    }


    /*
    Test methods
     */

    @Override
    public void testAccuracy(boolean compare, int dimensions, Clusters input) {
        TestMethods.getInstance().testSvd(compare,dimensions,input);
    }

    @Override
    public String extractModel(boolean compare, String organization, Clusters input) {
        return TestMethods.getInstance().extractModel(compare, organization, input);
    }


    /*
    Private methods
     */

    private HashMap<String, Integer> computeReqClusterMap(Map<Integer,List<String>> clusters, Set<String> requirements) {
        HashMap<String,Integer> reqCluster = new HashMap<>();
        for (String requirement: requirements) {
            reqCluster.put(requirement, -1);
        }
        Iterator it = clusters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            int id = (int) pair.getKey();
            List<String> clusterRequirements = (List<String>) pair.getValue();
            for (String req: clusterRequirements) {
                reqCluster.put(req, id);
            }
        }
        return reqCluster;
    }

    private boolean requirementUpdated(String organization, String responseId, Requirement requirement, Model model) throws InternalErrorException {
        String requirementText = buildRequirement(model.isCompare(), requirement);
        Map<String,Double> oldRequirement = model.getDocs().get(requirement.getId());
        Map<String,Double> newRequirement = Tfidf.getInstance().computeTfIdf(organization, responseId, requirementText, model);
        return !oldRequirement.equals(newRequirement);
    }

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

        if (array.length() == 0 && numberDependencies == 0) databaseOperations.generateResponsePage(responseId, organization, array, constants.getDependenciesArrayName());

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

        if (array.length() == 0 && numberDependencies == 0) databaseOperations.generateResponsePage(responseId, organization, array, constants.getDependenciesArrayName());

        if (array.length() > 0) {
            databaseOperations.generateResponsePage(responseId, organization, array, constants.getDependenciesArrayName());
        }

        control.showInfoMessage("Number dependencies: " + numberDependencies);
    }

    private void addRequirementsToModel(List<Requirement> requirements, Model model) throws InternalErrorException {

        int oldSize = model.getDocs().size();
        List<String> requirementsIds = new ArrayList<>();
        for (Requirement requirement: requirements) requirementsIds.add(requirement.getId());
        Tfidf.getInstance().deleteReqs(requirementsIds,model);

        updateModel(model.isCompare(), requirements, model, oldSize);
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

    private Model generateModel(boolean compare, double threshold, List<Requirement> requirements) throws InternalErrorException {
        Tfidf tfidf = Tfidf.getInstance();
        Map<String, Integer> corpusFrequency = new HashMap<>();
        List<String> text = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        buildCorpus(compare,requirements,text,ids);
        Map<String, Map<String, Double>> docs = tfidf.extractKeywords(text,ids,corpusFrequency);
        return new Model(docs,corpusFrequency,threshold,compare);
    }

    private void updateModel(boolean compare, List<Requirement> requirements, Model model, int oldSize) throws InternalErrorException {
        Tfidf tfidf = Tfidf.getInstance();
        List<String> text = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        buildCorpus(compare,requirements,text,ids);
        tfidf.addNewReqsAndRecomputeModel(text,ids,model,oldSize);
    }

    private void buildCorpus(boolean compare, List<Requirement> requirements, List<String> arrayText, List<String> arrayIds) {
        for (Requirement requirement: requirements) {
            arrayIds.add(requirement.getId());
            String text = buildRequirement(compare, requirement);
            arrayText.add(text);
        }
    }

    private String buildRequirement(boolean compare, Requirement requirement) {
        String text = "";
        if (requirement.getName() != null) text = text.concat(cleanText(requirement.getName()) + ". ");
        if (compare && (requirement.getText() != null)) text = text.concat(cleanText(requirement.getText()));
        return text;
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

    //is public to be accessible by tests
    public void getAccessToUpdate(String organization, String responseId) throws NotFinishedException, InternalErrorException {
        String errorMessage = "Synchronization error";
        int sleepTime = Constants.getInstance().getSleepTime();
        if (!organizationLocks.containsKey(organization)) {
            Lock aux = organizationLocks.putIfAbsent(organization, new ReentrantLock(true));
            //aux not used
        }
        Lock lock = organizationLocks.get(organization);
        if (lock == null) DatabaseOperations.getInstance().saveInternalException("Synchronization 1rst conditional",organization, responseId, new InternalErrorException(errorMessage));
        else {
            try {
                if (!lock.tryLock(sleepTime, TimeUnit.SECONDS)) { //NOSONAR
                    Control.getInstance().showInfoMessage("The " + organization + " database is locked, another thread is using it " + organization + " " + responseId);
                    DatabaseOperations.getInstance().saveNotFinishedException(organization, responseId, new NotFinishedException("There is another computation in the same organization with write or update rights that has not finished yet"));
                }
            } catch (InterruptedException e) {
                Control.getInstance().showErrorMessage(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    //is public to be accessible by tests
    public void releaseAccessToUpdate(String organization, String responseId) throws InternalErrorException {
        Lock lock = organizationLocks.get(organization);
        if (lock == null) DatabaseOperations.getInstance().saveInternalException("Synchronization 2nd conditional",organization, responseId, new InternalErrorException("Synchronization error"));
        else {
            try {
                lock.unlock();
            } catch (IllegalMonitorStateException e) {
                DatabaseOperations.getInstance().saveInternalException("Synchronization 3rd conditional: " + e.getMessage(),organization, responseId, new InternalErrorException("Synchronization error"));
            }
        }
    }

    public void removeOrganizationLock(String organization) {
        organizationLocks.remove(organization);
    }

    public ConcurrentMap<String, Lock> getConcurrentMap() {
        return organizationLocks;
    }
}
