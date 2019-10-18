package upc.similarity.compareapi.service;

import org.json.JSONArray;
import org.springframework.stereotype.Service;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.preprocess.PreprocessPipeline;
import upc.similarity.compareapi.similarity_algorithm.SimilarityModel;
import upc.similarity.compareapi.similarity_algorithm.SimilarityAlgorithm;
import upc.similarity.compareapi.util.Logger;
import upc.similarity.compareapi.entity.*;
import upc.similarity.compareapi.entity.auxiliary.*;
import upc.similarity.compareapi.entity.input.Clusters;
import upc.similarity.compareapi.entity.input.ProjectProject;
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

    private Logger logger = Logger.getInstance();
    private SimilarityAlgorithm similarityAlgorithm = Constants.getInstance().getSimilarityAlgorithm();
    private PreprocessPipeline preprocessPipeline = Constants.getInstance().getPreprocessPipeline();
    private ConcurrentHashMap<String, Lock> organizationLocks = new ConcurrentHashMap<>();
    private int sleepTime = Constants.getInstance().getMaxWaitingTime();


    /*
    Sync methods
     */

    //is public to be accessible by tests
    public void getAccessToUpdate(String organization, String responseId) throws NotFinishedException, InternalErrorException {
        String errorMessage = "Synchronization error";
        if (!organizationLocks.containsKey(organization)) {
            Lock aux = organizationLocks.putIfAbsent(organization, new ReentrantLock(true));
            //aux not used
        }
        Lock lock = organizationLocks.get(organization);
        if (lock == null) DatabaseOperations.getInstance().saveInternalException("Synchronization 1rst conditional",organization, responseId, new InternalErrorException(errorMessage));
        else {
            try {
                if (!lock.tryLock(sleepTime, TimeUnit.SECONDS)) { //NOSONAR
                    Logger.getInstance().showInfoMessage("The " + organization + " database is locked, another thread is using it " + organization + " " + responseId);
                    DatabaseOperations.getInstance().saveNotFinishedException(organization, responseId, new NotFinishedException("There is another computation in the same organization with write or update rights that has not finished yet"));
                }
            } catch (InterruptedException e) {
                Logger.getInstance().showErrorMessage(e.getMessage());
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


    /*
    Public methods
    Similarity without clusters
     */

    @Override
    public void buildModel(String responseId, boolean compare, String organization, List<Requirement> requirements) throws ForbiddenException, BadRequestException, NotFinishedException, InternalErrorException {
        logger.showInfoMessage("BuildModel: Start computing " + organization + " " + responseId);

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId,"BuildModel");
        if (databaseOperations.existsOrganization(responseId,organization)) databaseOperations.saveForbiddenException(organization,responseId,new ForbiddenException(Constants.getInstance().getForbiddenErrorMessage()));

        SimilarityModel similarityModel = generateModel(compare, deleteDuplicates(requirements, organization, responseId));
        //threshold is never used in other without cluster methods
        OrganizationModels organizationModels = new OrganizationModels(0,compare,false, similarityModel);

        getAccessToUpdate(organization, responseId);
        try {
            databaseOperations.saveOrganizationModels(organization, responseId, organizationModels);
        } finally {
            releaseAccessToUpdate(organization, responseId);
        }
        databaseOperations.generateEmptyResponse(organization, responseId);

        logger.showInfoMessage("BuildModel: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void buildModelAndCompute(String responseId, boolean compare, String organization, double threshold, List<Requirement> requirements, int maxNumDeps) throws BadRequestException, ForbiddenException, NotFinishedException, InternalErrorException {
        logger.showInfoMessage("BuildModelAndCompute: Start computing " + organization + " " + responseId);

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId,"BuildModelAndCompute");
        if (databaseOperations.existsOrganization(responseId,organization)) databaseOperations.saveForbiddenException(organization,responseId,new ForbiddenException(Constants.getInstance().getForbiddenErrorMessage()));

        //threshold is never used in other methods
        SimilarityModel similarityModel = generateModel(compare, deleteDuplicates(requirements, organization, responseId));
        //threshold is never used in other without cluster methods
        OrganizationModels organizationModels = new OrganizationModels(0,compare,false, similarityModel);
        List<String> requirementsIds = new ArrayList<>();
        for (Requirement requirement: requirements) {
            requirementsIds.add(requirement.getId());
        }

        getAccessToUpdate(organization, responseId);
        try {
            databaseOperations.saveOrganizationModels(organization, responseId, organizationModels);
        } finally {
            releaseAccessToUpdate(organization, responseId);
        }

        project(requirementsIds, organizationModels.getSimilarityModel(), threshold, responseId, organization, maxNumDeps);

        databaseOperations.finishComputation(organization, responseId);

        logger.showInfoMessage("BuildModelAndCompute: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void addRequirements(String responseId, String organization, List<Requirement> requirements) throws BadRequestException, NotFoundException, NotFinishedException, InternalErrorException {
        logger.showInfoMessage("AddRequirements: Start computing " + organization + " " + responseId);

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId,"AddRequirements");

        getAccessToUpdate(organization, responseId);

        try {
            OrganizationModels organizationModels = databaseOperations.loadOrganizationModels(organization, responseId, false);
            if (organizationModels.hasClusters()) databaseOperations.saveBadRequestException(organization,responseId,new BadRequestException("The model has clusters. Use the similarity with clusters methods instead."));
            SimilarityModel similarityModel = organizationModels.getSimilarityModel();
            List<Requirement> notDuplicatedRequirements = deleteDuplicates(requirements, organization, responseId);
            List<Requirement> requirementsToAddOrUpdate = new ArrayList<>();
            for (Requirement requirement: notDuplicatedRequirements) {
                if (similarityModel.containsRequirement(requirement.getId())) {
                    if (requirementUpdated(requirement,organizationModels)) requirementsToAddOrUpdate.add(requirement);
                } else requirementsToAddOrUpdate.add(requirement);
            }
            addRequirementsToModel(organizationModels, requirementsToAddOrUpdate);
            databaseOperations.saveOrganizationModels(organization, responseId, organizationModels);
        } finally {
            releaseAccessToUpdate(organization, responseId);
        }

        databaseOperations.generateEmptyResponse(organization, responseId);

        logger.showInfoMessage("AddRequirements: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void deleteRequirements(String responseId, String organization, List<Requirement> requirements) throws BadRequestException, NotFoundException, NotFinishedException, InternalErrorException  {
        logger.showInfoMessage("DeleteRequirements: Start computing " + organization + " " + responseId);

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId,"DeleteRequirements");

        getAccessToUpdate(organization, responseId);

        try {
            OrganizationModels organizationModels = databaseOperations.loadOrganizationModels(organization, responseId, false);
            if (organizationModels.hasClusters()) databaseOperations.saveBadRequestException(organization,responseId,new BadRequestException("The model has clusters. Use the similarity with clusters methods instead."));
            List<Requirement> notDuplicatedRequirements = deleteDuplicates(requirements, organization, responseId);
            SimilarityModel similarityModel = organizationModels.getSimilarityModel();
            deleteRequirementsFromModel(similarityModel,notDuplicatedRequirements);
            databaseOperations.saveOrganizationModels(organization, responseId, organizationModels);
        } finally {
            releaseAccessToUpdate(organization, responseId);
        }

        databaseOperations.generateEmptyResponse(organization, responseId);

        logger.showInfoMessage("DeleteRequirements: Finish computing " + organization + " " + responseId);
    }


    @Override
    public Dependency simReqReq(String organization, String req1, String req2) throws NotFoundException, InternalErrorException {
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        Constants constants = Constants.getInstance();
        OrganizationModels organizationModels = databaseOperations.loadOrganizationModels(organization,null,true);
        SimilarityModel similarityModel = organizationModels.getSimilarityModel();
        if (!similarityModel.containsRequirement(req1)) throw new NotFoundException("The requirement with id " + req1 + " is not present in the model loaded form the database");
        if (!similarityModel.containsRequirement(req2)) throw new NotFoundException("The requirement with id " + req2 + " is not present in the model loaded form the database");
        double score = similarityAlgorithm.computeSimilarity(similarityModel,req1,req2);
        return new Dependency(score,req1,req2,constants.getStatus(),constants.getDependencyType(),constants.getComponent());
    }

    @Override
    public void simReqOrganization(String responseId, String organization, double threshold, List<String> requirements, int maxNumDeps) throws NotFoundException, InternalErrorException {
        logger.showInfoMessage("SimReqOrganization: Start computing " + organization + " " + responseId);
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId,"SimReqOrganization");

        OrganizationModels organizationModels = databaseOperations.loadOrganizationModels(organization,responseId,true);
        SimilarityModel similarityModel = organizationModels.getSimilarityModel();

        HashSet<String> repeatedHash = new HashSet<>();
        List<String> projectRequirements = new ArrayList<>();
        List<String> requirementsToCompare = new ArrayList<>();
        for (String requirement : requirements) {
            requirementsToCompare.add(requirement);
            repeatedHash.add(requirement);
        }

        List<String> modelRequirements = similarityModel.getRequirementsIds();
        for (String requirement: modelRequirements) {
            if (!repeatedHash.contains(requirement)) projectRequirements.add(requirement);
        }

        reqProject(requirementsToCompare, projectRequirements, similarityModel, threshold, organization, responseId,true, maxNumDeps);
        databaseOperations.finishComputation(organization, responseId);

        logger.showInfoMessage("SimReqOrganization: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void simNewReqOrganization(String responseId, String organization, double threshold, List<Requirement> requirements, int maxNumDeps) throws NotFoundException, NotFinishedException, BadRequestException, InternalErrorException {
        logger.showInfoMessage("SimReqOrganization: Start computing " + organization + " " + responseId);
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        databaseOperations.generateResponse(organization,responseId,"SimReqOrganization");

        getAccessToUpdate(organization, responseId);

        try {
            OrganizationModels organizationModels = databaseOperations.loadOrganizationModels(organization,responseId,false);
            if (organizationModels.hasClusters()) databaseOperations.saveBadRequestException(organization,responseId,new BadRequestException("The model has clusters. Use the similarity with clusters methods instead."));

            List<Requirement> notDuplicatedRequirements = deleteDuplicates(requirements, organization, responseId);

            SimilarityModel similarityModel = organizationModels.getSimilarityModel();
            addRequirementsToModel(organizationModels, notDuplicatedRequirements);
            HashSet<String> repeatedHash = new HashSet<>();
            for (Requirement requirement : notDuplicatedRequirements) repeatedHash.add(requirement.getId());

            List<String> projectRequirements = new ArrayList<>();
            List<String> requirementsToCompare = new ArrayList<>();
            for (Requirement requirement : notDuplicatedRequirements) requirementsToCompare.add(requirement.getId());

            List<String> modelRequirements = similarityModel.getRequirementsIds();
            for (String requirement: modelRequirements) {
                if (!repeatedHash.contains(requirement)) projectRequirements.add(requirement);
            }

            reqProject(requirementsToCompare, projectRequirements, similarityModel, threshold, organization, responseId, true, maxNumDeps);
            databaseOperations.saveOrganizationModels(organization,responseId,organizationModels);
        } finally {
            releaseAccessToUpdate(organization, responseId);
        }

        databaseOperations.finishComputation(organization, responseId);

        logger.showInfoMessage("SimReqOrganization: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void simReqProject(String responseId, String organization, double threshold, ReqProject projectRequirements, int maxNumDeps) throws NotFoundException, InternalErrorException, BadRequestException {
        logger.showInfoMessage("SimReqProject: Start computing " + organization + " " + responseId);
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();

        databaseOperations.generateResponse(organization,responseId,"SimReqProject");

        SimilarityModel similarityModel = databaseOperations.loadOrganizationModels(organization,responseId,true).getSimilarityModel();
        for (String req: projectRequirements.getReqsToCompare()) {
            if (projectRequirements.getProjectReqs().contains(req)) databaseOperations.saveBadRequestException(organization, responseId, new BadRequestException("The requirement with id " + req + " is already inside the project"));
        }

        reqProject(projectRequirements.getReqsToCompare(), projectRequirements.getProjectReqs(), similarityModel, threshold, organization, responseId, true, maxNumDeps);

        databaseOperations.finishComputation(organization, responseId);
        logger.showInfoMessage("SimReqProject: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void simProject(String responseId, String organization, double threshold, List<String> projectRequirements, int maxNumDeps) throws NotFoundException, InternalErrorException {
        logger.showInfoMessage("SimProject: Start computing " + organization + " " + responseId);
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();

        databaseOperations.generateResponse(organization,responseId,"SimProject");

        SimilarityModel similarityModel = databaseOperations.loadOrganizationModels(organization,responseId,true).getSimilarityModel();

        project(projectRequirements, similarityModel, threshold, responseId, organization, maxNumDeps);

        databaseOperations.finishComputation(organization, responseId);
        logger.showInfoMessage("SimProject: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void simProjectProject(String responseId, String organization, double threshold, ProjectProject projects, int maxNumDeps) throws NotFoundException, InternalErrorException {
        logger.showInfoMessage("SimProjectProject: Start computing " + organization + " " + responseId);
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();

        databaseOperations.generateResponse(organization,responseId,"SimProjectProject");

        List<String> project1NotRepeated = deleteListDuplicates(projects.getFirstProjectRequirements());
        List<String> project2NotRepeated = deleteListDuplicates(projects.getSecondProjectRequirements());

        SimilarityModel similarityModel = databaseOperations.loadOrganizationModels(organization,responseId,true).getSimilarityModel();

        reqProject(project1NotRepeated,project2NotRepeated,similarityModel,threshold,organization,responseId, false, maxNumDeps);

        databaseOperations.finishComputation(organization, responseId);
        logger.showInfoMessage("SimProjectProject: Finish computing " + organization + " " + responseId);
    }


    /*
    Public methods
    Similarity with clusters
     */

    @Override
    public void buildClusters(String responseId, boolean compare, double threshold, String organization, Clusters input) throws ForbiddenException, BadRequestException, NotFinishedException, InternalErrorException {
        logger.showInfoMessage("BuildClusters: Start computing " + organization + " " + responseId + " " + input.getRequirements().size() + " reqs");

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();

        if (!input.inputOk()) databaseOperations.saveBadRequestException(organization, responseId, new BadRequestException("The input requirements array is empty"));

        databaseOperations.generateResponse(organization,responseId,"BuildClusters");
        if (databaseOperations.existsOrganization(responseId,organization)) databaseOperations.saveForbiddenException(organization,responseId,new ForbiddenException(Constants.getInstance().getForbiddenErrorMessage()));

        List<Requirement> requirements = deleteDuplicates(input.getRequirements(),organization,responseId);
        SimilarityModel similarityModel = generateModel(compare, requirements);
        ClusterOperations clusterOperations = ClusterOperations.getInstance();
        ClusterAndDeps iniClusters = clusterOperations.computeIniClusters(input.getDependencies(), requirements);

        OrganizationModels organizationModels = new OrganizationModels(threshold,compare,true,similarityModel,iniClusters.getLastClusterId(),iniClusters.getClusters(),iniClusters.getDependencies());
        getAccessToUpdate(organization, responseId);
        try {
            databaseOperations.saveOrganizationModels(organization,responseId,organizationModels);
            databaseOperations.createDepsAuxiliaryTable(organization, responseId);
            clusterOperations.computeProposedDependencies(organization, responseId,  new HashSet<>(similarityModel.getRequirementsIds()), organizationModels.getClusters().keySet(), organizationModels, true);
            databaseOperations.updateModelClustersAndDependencies(organization, responseId, organizationModels, null, true);
        } finally {
            releaseAccessToUpdate(organization, responseId);
        }

        databaseOperations.generateEmptyResponse(organization, responseId);

        logger.showInfoMessage("BuildClusters: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void buildClustersAndCompute(String responseId, boolean compare, String organization, double threshold, int maxNumber, Clusters input) throws ForbiddenException, BadRequestException, NotFinishedException, InternalErrorException {
        logger.showInfoMessage("BuildClustersAndCompute: Start computing " + organization + " " + responseId + " " + input.getRequirements().size() + " reqs");

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();

        if (!input.inputOk()) databaseOperations.saveBadRequestException(organization, responseId, new BadRequestException("The input requirements array is empty"));

        databaseOperations.generateResponse(organization,responseId,"BuildClustersAndCompute");
        if (databaseOperations.existsOrganization(responseId,organization)) databaseOperations.saveForbiddenException(organization,responseId,new ForbiddenException(Constants.getInstance().getForbiddenErrorMessage()));

        List<Requirement> requirements = deleteDuplicates(input.getRequirements(),organization,responseId);

        SimilarityModel similarityModel = generateModel(compare, requirements);
        ClusterOperations clusterOperations = ClusterOperations.getInstance();
        ClusterAndDeps iniClusters = clusterOperations.computeIniClusters(input.getDependencies(), requirements);

        OrganizationModels organizationModels = new OrganizationModels(threshold,compare,true,similarityModel,iniClusters.getLastClusterId(),iniClusters.getClusters(),iniClusters.getDependencies());
        getAccessToUpdate(organization, responseId);
        try {
            databaseOperations.saveOrganizationModels(organization,responseId,organizationModels);
            databaseOperations.createDepsAuxiliaryTable(organization, responseId);
            clusterOperations.computeProposedDependencies(organization, responseId,  new HashSet<>(similarityModel.getRequirementsIds()), organizationModels.getClusters().keySet(), organizationModels, true);
            databaseOperations.updateModelClustersAndDependencies(organization, responseId, organizationModels, null, true);
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

        logger.showInfoMessage("BuildClustersAndCompute: Finish computing " + organization + " " + responseId);
    }

    @Override
    public Dependencies simReqClusters(String organization, List<String> requirements, int maxNumber) throws NotFoundException, InternalErrorException {
        logger.showInfoMessage("SimReqClusters: Start computing");
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

        logger.showInfoMessage("SimReqClusters: Finish computing");
        return new Dependencies(result);
    }

    @Override
    public void treatAcceptedAndRejectedDependencies(String organization, List<Dependency> dependencies) throws NotFoundException, BadRequestException, NotFinishedException, InternalErrorException {
        logger.showInfoMessage("TreatAcceptedAndRejectedDependencies: Start computing");

        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        getAccessToUpdate(organization, null);
        try {
            OrganizationModels organizationModels = databaseOperations.loadOrganizationModels(organization,null,false);
            if (!organizationModels.hasClusters()) throw new BadRequestException("The model does not have clusters");

            dependencies.sort(Comparator.comparing(Dependency::computeTime));
            HashSet<Integer> clustersChanged = new HashSet<>();
            Map<String,Integer> reqCluster = computeReqClusterMap(organizationModels.getClusters(), new HashSet<>(organizationModels.getSimilarityModel().getRequirementsIds()));
            ClusterOperations clusterOperations = ClusterOperations.getInstance();
            databaseOperations.createDepsAuxiliaryTable(organization, null);

            for (Dependency dependency: dependencies) {
                if (dependency.getDependencyType() != null && dependency.getStatus() != null && dependency.getDependencyType().equals("similar")) {
                    String status = dependency.getStatus();
                    List<Dependency> aux = new ArrayList<>();
                    aux.add(dependency);
                    if (status.equals("accepted")) {
                        clusterOperations.addAcceptedDependencies(organization, null, aux, organizationModels, clustersChanged, reqCluster);
                    } else if (status.equals("rejected")) {
                        clusterOperations.addRejectedDependencies(organization, null, aux, organizationModels, clustersChanged, reqCluster);
                    }
                }
            }
            clusterOperations.updateProposedDependencies(organization, null, organizationModels, clustersChanged, true);
            databaseOperations.updateModelClustersAndDependencies(organization, null, organizationModels, null, true);
        } finally {
            releaseAccessToUpdate(organization, null);
        }

        logger.showInfoMessage("TreatAcceptedAndRejectedDependencies: Finish computing");
    }

    @Override
    public void batchProcess(String responseId, String organization, Clusters input) throws BadRequestException, NotFoundException, NotFinishedException, InternalErrorException {
        logger.showInfoMessage("BatchProcess: Start computing " + organization + " " + responseId);
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        ClusterOperations clusterOperations = ClusterOperations.getInstance();

        databaseOperations.generateResponse(organization,responseId,"BatchProcess");

        getAccessToUpdate(organization, responseId);

        try {
            OrganizationModels organizationModels = databaseOperations.loadOrganizationModels(organization,responseId,false);

            if (!organizationModels.hasClusters()) databaseOperations.saveBadRequestException(organization, responseId, new BadRequestException("The model does not have clusters"));
            SimilarityModel similarityModel = organizationModels.getSimilarityModel();

            List<OrderedObject> objects = new ArrayList<>();

            Set<String> notRepeatedReqs = new HashSet<>();

            for (Requirement requirement : input.getRequirements()) {
                String id = requirement.getId();
                if (id != null && !notRepeatedReqs.contains(id)) {
                    notRepeatedReqs.add(id);
                    if (similarityModel.containsRequirement(id)) {
                        if (requirementUpdated(requirement, organizationModels)) objects.add(new OrderedObject(null, requirement, requirement.getTime()));
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
            Map<String,Integer> reqCluster = computeReqClusterMap(organizationModels.getClusters(), new HashSet<>(organizationModels.getSimilarityModel().getRequirementsIds()));

            for (OrderedObject orderedObject: objects) {
                if (orderedObject.isDependency()) {
                    List<Dependency> aux = new ArrayList<>();
                    Dependency dependency = orderedObject.getDependency();
                    aux.add(dependency);
                    if (dependency.getStatus().equals("accepted")) {
                        clusterOperations.addAcceptedDependencies(organization, responseId, aux, organizationModels, clustersChanged, reqCluster);
                    }
                    else {
                        clusterOperations.addRejectedDependencies(organization, responseId, aux, organizationModels, clustersChanged, reqCluster);
                    }
                } else {
                    List<Requirement> aux = new ArrayList<>();
                    Requirement requirement = orderedObject.getRequirement();
                    aux.add(requirement);
                    clusterOperations.addRequirementsToClusters(organization, responseId, aux, organizationModels, clustersChanged, reqCluster);
                    addRequirementsToModel(organizationModels,aux);
                }
            }
            clusterOperations.updateProposedDependencies(organization, responseId, organizationModels, clustersChanged, true);
            organizationModels.setDependencies(new ArrayList<>());
            databaseOperations.saveOrganizationModels(organization,responseId,organizationModels);
            databaseOperations.updateModelClustersAndDependencies(organization, responseId, organizationModels, null, true);

        } finally {
            releaseAccessToUpdate(organization, responseId);
        }

        databaseOperations.generateEmptyResponse(organization, responseId);
        logger.showInfoMessage("BatchProcess: Finish computing " + organization + " " + responseId);
    }


    /*
    Public methods
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
    Private methods
     */

    private List<String> deleteListDuplicates(List<String> inputList) {
        HashSet<String> notRepeated = new HashSet<>(inputList);
        return new ArrayList<>(notRepeated);
    }

    private HashMap<String, Integer> computeReqClusterMap(Map<Integer,List<String>> clusters, Set<String> requirements) {
        HashMap<String,Integer> reqCluster = new HashMap<>();
        for (String requirement: requirements) {
            reqCluster.put(requirement, -1);
        }
        for (Map.Entry<Integer, List<String>> entry : clusters.entrySet()) {
            int id = entry.getKey();
            List<String> clusterRequirements = entry.getValue();
            for (String req : clusterRequirements) {
                reqCluster.put(req, id);
            }
        }
        return reqCluster;
    }

    private boolean requirementUpdated(Requirement requirement, OrganizationModels organizationModels) throws InternalErrorException {
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(requirement);
        Map<String, List<String>> preprocessedRequirement = preprocessPipeline.preprocessRequirements(organizationModels.isCompare(),requirements);
        return organizationModels.getSimilarityModel().checkIfRequirementIsUpdated(requirement.getId(),preprocessedRequirement.get(requirement.getId()));
    }

    private void reqProject(List<String> reqsToCompare, List<String> projectRequirements, SimilarityModel similarityModel, double threshold, String organization, String responseId, boolean include, int maxNumDeps) throws InternalErrorException {
        boolean memoryDeps = maxNumDeps > 0;
        ResponseDependencies responseDependencies;
        if (memoryDeps) responseDependencies = new SizeFixedDependenciesQueue(organization,responseId,maxNumDeps,Comparator.comparing(Dependency::getDependencyScore).thenComparing(Dependency::getToid).thenComparing(Dependency::getFromid).reversed());
        else responseDependencies = new DiskDependencies(organization,responseId);
        Constants constants = Constants.getInstance();

        for (String req1: reqsToCompare) {
            if (similarityModel.containsRequirement(req1)) {
                for (String req2 : projectRequirements) {
                    if (!req1.equals(req2) && similarityModel.containsRequirement(req2)) {
                        double score = similarityAlgorithm.computeSimilarity(similarityModel,req1,req2);
                        if (score >= threshold) {
                            Dependency dependency = new Dependency(score, req1, req2, constants.getStatus(), constants.getDependencyType(), constants.getComponent());
                            responseDependencies.addDependency(dependency);
                        }
                    }
                }
                if (include) projectRequirements.add(req1);
            }
        }
        responseDependencies.finish();
    }


    private void project(List<String> projectRequirements, SimilarityModel similarityModel, double threshold, String responseId, String organization, int maxNumDeps) throws InternalErrorException {
        boolean memoryDeps = maxNumDeps > 0;
        ResponseDependencies responseDependencies;
        if (memoryDeps) responseDependencies = new SizeFixedDependenciesQueue(organization,responseId,maxNumDeps,Comparator.comparing(Dependency::getDependencyScore).thenComparing(Dependency::getToid).thenComparing(Dependency::getFromid).reversed());
        else responseDependencies = new DiskDependencies(organization,responseId);
        Constants constants = Constants.getInstance();

        for (int i = 0; i < projectRequirements.size(); ++i) {
            String req1 = projectRequirements.get(i);
            if (similarityModel.containsRequirement(req1)) {
                for (int j = i + 1; j < projectRequirements.size(); ++j) {
                    String req2 = projectRequirements.get(j);
                    if (!req2.equals(req1) && similarityModel.containsRequirement(req2)) {
                        double score = similarityAlgorithm.computeSimilarity(similarityModel,req1,req2);
                        if (score >= threshold) {
                            Dependency dependency = new Dependency(score, req1, req2, constants.getStatus(), constants.getDependencyType(), constants.getComponent());
                            responseDependencies.addDependency(dependency);
                        }
                    }
                }
            }
        }
        responseDependencies.finish();
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

    private SimilarityModel generateModel(boolean compare, List<Requirement> requirements) throws InternalErrorException {
        return similarityAlgorithm.buildModel(preprocessPipeline.preprocessRequirements(compare,requirements));
    }

    private void addRequirementsToModel(OrganizationModels organizationModels, List<Requirement> requirements) throws InternalErrorException {
        SimilarityModel similarityModel = organizationModels.getSimilarityModel();
        deleteRequirementsFromModel(similarityModel,requirements);
        similarityAlgorithm.addRequirements(similarityModel,preprocessPipeline.preprocessRequirements(organizationModels.isCompare(),requirements));
    }

    private void deleteRequirementsFromModel(SimilarityModel similarityModel, List<Requirement> requirements) {
        List<String> requirementIds = new ArrayList<>();
        for (Requirement requirement: requirements) {
            String id = requirement.getId();
            if (similarityModel.containsRequirement(id))requirementIds.add(id);
        }
        if (!requirementIds.isEmpty()) similarityAlgorithm.deleteRequirements(similarityModel,requirementIds);
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
}
