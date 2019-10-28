package upc.similarity.compareapi.service;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import upc.similarity.compareapi.clusters_algorithm.ClustersAlgorithm;
import upc.similarity.compareapi.clusters_algorithm.ClustersModel;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.dao.DatabaseModel;
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
import upc.similarity.compareapi.entity.exception.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service("comparerService")
public class CompareServiceImpl implements CompareService {

    private Logger logger = Logger.getInstance();
    private PreprocessPipeline preprocessPipeline = Constants.getInstance().getPreprocessPipeline();
    private SimilarityAlgorithm similarityAlgorithm = Constants.getInstance().getSimilarityAlgorithm();
    private ClustersAlgorithm clustersAlgorithm = Constants.getInstance().getClustersAlgorithm();
    private ConcurrentHashMap<String, Lock> organizationLocks = new ConcurrentHashMap<>();
    private int sleepTime = Constants.getInstance().getMaxWaitingTime();
    private DatabaseModel databaseOperations = Constants.getInstance().getDatabaseModel();
    private String syncErrorMessage = "Synchronization error";

    private static final String FORBIDDEN_ERROR_MESSAGE = "The organization already has a model created. Please use the method called DeleteOrganizationData to delete the organization's model";


    /*
    Sync methods
     */

    //is public to be accessible by tests
    public void getAccessToUpdate(String organization) throws LockedOrganizationException, InternalErrorException {
        if (!organizationLocks.containsKey(organization)) {
            Lock aux = organizationLocks.putIfAbsent(organization, new ReentrantLock(true));
            //aux not used
        }
        Lock lock = organizationLocks.get(organization);
        if (lock == null) {
            logger.showErrorMessage("Synchronization 1rst conditional");
            throw  new InternalErrorException(syncErrorMessage);
        }
        else {
            try {
                if (!lock.tryLock(sleepTime, TimeUnit.SECONDS)) { //NOSONAR
                    throw new LockedOrganizationException("There is another computation in the same organization with write or update rights that has not finished yet");
                }
            } catch (InterruptedException e) {
                Logger.getInstance().showErrorMessage(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    //is public to be accessible by tests
    public void releaseAccessToUpdate(String organization) throws InternalErrorException {
        Lock lock = organizationLocks.get(organization);
        if (lock == null) {
            logger.showErrorMessage("Synchronization 2nd conditional");
            throw new InternalErrorException(syncErrorMessage);
        }
        else {
            try {
                lock.unlock();
            } catch (IllegalMonitorStateException e) {
                logger.showErrorMessage("Synchronization 3rd conditional");
                throw new InternalErrorException(syncErrorMessage);
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
    public void buildModel(String responseId, boolean compare, String organization, List<Requirement> requirements) throws ComponentException {
        logger.showInfoMessage("BuildModel: Start computing " + organization + " " + responseId);
        try {
            databaseOperations.saveResponse(organization,responseId,"BuildModel");
            if (databaseOperations.existsOrganization(organization)) throw new ForbiddenException(FORBIDDEN_ERROR_MESSAGE);
            SimilarityModel similarityModel = generateModel(compare, deleteDuplicates(requirements));
            OrganizationModels organizationModels = new OrganizationModels(0,compare,false, similarityModel);
            getAccessToUpdate(organization);
            try {
                databaseOperations.saveOrganizationModels(organization, organizationModels, true, false);
            } finally {
                releaseAccessToUpdate(organization);
            }
            generateEmptyResponse(organization, responseId);
        } catch (ComponentException e) {
            throw treatComponentException(organization,responseId,true,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,responseId,true,e);
        }
        logger.showInfoMessage("BuildModel: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void buildModelAndCompute(String responseId, boolean compare, String organization, double threshold, List<Requirement> requirements, int maxNumDeps) throws ComponentException {
        logger.showInfoMessage("BuildModelAndCompute: Start computing " + organization + " " + responseId);
        try {
            databaseOperations.saveResponse(organization, responseId, "BuildModelAndCompute");
            if (databaseOperations.existsOrganization(organization)) throw new ForbiddenException(FORBIDDEN_ERROR_MESSAGE);
            SimilarityModel similarityModel = generateModel(compare, deleteDuplicates(requirements));
            OrganizationModels organizationModels = new OrganizationModels(0, compare, false, similarityModel);
            List<String> requirementsIds = new ArrayList<>();
            for (Requirement requirement : requirements) {
                requirementsIds.add(requirement.getId());
            }
            getAccessToUpdate(organization);
            try {
                databaseOperations.saveOrganizationModels(organization, organizationModels, true, false);
            } finally {
                releaseAccessToUpdate(organization);
            }
            project(requirementsIds, organizationModels.getSimilarityModel(), threshold, responseId, organization, maxNumDeps);
            databaseOperations.finishComputation(organization, responseId);
        } catch (ComponentException e) {
            throw treatComponentException(organization,responseId,true,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,responseId,true,e);
        }
        logger.showInfoMessage("BuildModelAndCompute: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void addRequirements(String responseId, String organization, List<Requirement> requirements) throws ComponentException {
        logger.showInfoMessage("AddRequirements: Start computing " + organization + " " + responseId);
        try {
            databaseOperations.saveResponse(organization, responseId, "AddRequirements");
            getAccessToUpdate(organization);
            try {
                OrganizationModels organizationModels = databaseOperations.getOrganizationModels(organization, false);
                if (organizationModels.hasClusters()) throw new BadRequestException("The model has clusters. Use the similarity with clusters methods instead.");
                SimilarityModel similarityModel = organizationModels.getSimilarityModel();
                List<Requirement> notDuplicatedRequirements = deleteDuplicates(requirements);
                List<Requirement> requirementsToAddOrUpdate = new ArrayList<>();
                for (Requirement requirement : notDuplicatedRequirements) {
                    if (similarityModel.containsRequirement(requirement.getId())) {
                        if (requirementUpdated(requirement, organizationModels))
                            requirementsToAddOrUpdate.add(requirement);
                    } else requirementsToAddOrUpdate.add(requirement);
                }
                addRequirementsToModel(organizationModels, requirementsToAddOrUpdate);
                databaseOperations.saveOrganizationModels(organization, organizationModels, true, false);
            } finally {
                releaseAccessToUpdate(organization);
            }
            generateEmptyResponse(organization, responseId);
        } catch (ComponentException e) {
            throw treatComponentException(organization,responseId,true,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,responseId,true,e);
        }
        logger.showInfoMessage("AddRequirements: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void deleteRequirements(String responseId, String organization, List<Requirement> requirements) throws ComponentException  {
        logger.showInfoMessage("DeleteRequirements: Start computing " + organization + " " + responseId);
        try {
            databaseOperations.saveResponse(organization, responseId, "DeleteRequirements");
            getAccessToUpdate(organization);
            try {
                OrganizationModels organizationModels = databaseOperations.getOrganizationModels(organization, false);
                if (organizationModels.hasClusters()) throw new BadRequestException("The model has clusters. Use the similarity with clusters methods instead.");
                List<Requirement> notDuplicatedRequirements = deleteDuplicates(requirements);
                SimilarityModel similarityModel = organizationModels.getSimilarityModel();
                deleteRequirementsFromModel(similarityModel, notDuplicatedRequirements);
                databaseOperations.saveOrganizationModels(organization, organizationModels, true, false);
            } finally {
                releaseAccessToUpdate(organization);
            }
            generateEmptyResponse(organization, responseId);
        } catch (ComponentException e) {
            throw treatComponentException(organization,responseId,true,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,responseId,true,e);
        }
        logger.showInfoMessage("DeleteRequirements: Finish computing " + organization + " " + responseId);
    }


    @Override
    public Dependency simReqReq(String organization, String req1, String req2) throws ComponentException {
        try {
            OrganizationModels organizationModels = databaseOperations.getOrganizationModels(organization, true);
            SimilarityModel similarityModel = organizationModels.getSimilarityModel();
            if (!similarityModel.containsRequirement(req1)) throw new NotFoundException("The requirement with id " + req1 + " is not present in the model loaded form the database");
            if (!similarityModel.containsRequirement(req2)) throw new NotFoundException("The requirement with id " + req2 + " is not present in the model loaded form the database");
            double score = similarityAlgorithm.computeSimilarity(similarityModel, req1, req2);
            return new Dependency(score, req1, req2);
        } catch (ComponentException e) {
            throw treatComponentException(organization,null,false,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,null,false,e);
        }
    }

    @Override
    public void simReqOrganization(String responseId, String organization, double threshold, List<String> requirements, int maxNumDeps) throws ComponentException {
        logger.showInfoMessage("SimReqOrganization: Start computing " + organization + " " + responseId);
        try {
            databaseOperations.saveResponse(organization, responseId, "SimReqOrganization");
            OrganizationModels organizationModels = databaseOperations.getOrganizationModels(organization, true);
            SimilarityModel similarityModel = organizationModels.getSimilarityModel();

            HashSet<String> repeatedHash = new HashSet<>();
            List<String> projectRequirements = new ArrayList<>();
            List<String> requirementsToCompare = new ArrayList<>();
            for (String requirement : requirements) {
                requirementsToCompare.add(requirement);
                repeatedHash.add(requirement);
            }

            List<String> modelRequirements = similarityModel.getRequirementsIds();
            for (String requirement : modelRequirements) {
                if (!repeatedHash.contains(requirement)) projectRequirements.add(requirement);
            }

            reqProject(requirementsToCompare, projectRequirements, similarityModel, threshold, organization, responseId, true, maxNumDeps);
            databaseOperations.finishComputation(organization, responseId);
        } catch (ComponentException e) {
            throw treatComponentException(organization,responseId,true,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,responseId,true,e);
        }
        logger.showInfoMessage("SimReqOrganization: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void simNewReqOrganization(String responseId, String organization, double threshold, List<Requirement> requirements, int maxNumDeps) throws ComponentException {
        logger.showInfoMessage("SimReqOrganization: Start computing " + organization + " " + responseId);
        try {
            databaseOperations.saveResponse(organization, responseId, "SimReqOrganization");
            getAccessToUpdate(organization);
            try {
                OrganizationModels organizationModels = databaseOperations.getOrganizationModels(organization, false);
                if (organizationModels.hasClusters()) throw  new BadRequestException("The model has clusters. Use the similarity with clusters methods instead.");
                List<Requirement> notDuplicatedRequirements = deleteDuplicates(requirements);

                SimilarityModel similarityModel = organizationModels.getSimilarityModel();
                addRequirementsToModel(organizationModels, notDuplicatedRequirements);
                HashSet<String> repeatedHash = new HashSet<>();
                for (Requirement requirement : notDuplicatedRequirements) repeatedHash.add(requirement.getId());

                List<String> projectRequirements = new ArrayList<>();
                List<String> requirementsToCompare = new ArrayList<>();
                for (Requirement requirement : notDuplicatedRequirements)
                    requirementsToCompare.add(requirement.getId());

                List<String> modelRequirements = similarityModel.getRequirementsIds();
                for (String requirement : modelRequirements) {
                    if (!repeatedHash.contains(requirement)) projectRequirements.add(requirement);
                }

                reqProject(requirementsToCompare, projectRequirements, similarityModel, threshold, organization, responseId, true, maxNumDeps);
                databaseOperations.saveOrganizationModels(organization, organizationModels, true, false);
            } finally {
                releaseAccessToUpdate(organization);
            }
            databaseOperations.finishComputation(organization, responseId);
        } catch (ComponentException e) {
            throw treatComponentException(organization,responseId,true,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,responseId,true,e);
        }
        logger.showInfoMessage("SimReqOrganization: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void simReqProject(String responseId, String organization, double threshold, ReqProject projectRequirements, int maxNumDeps) throws ComponentException {
        logger.showInfoMessage("SimReqProject: Start computing " + organization + " " + responseId);

        try {
            databaseOperations.saveResponse(organization, responseId, "SimReqProject");
            SimilarityModel similarityModel = databaseOperations.getOrganizationModels(organization, true).getSimilarityModel();

            for (String req : projectRequirements.getReqsToCompare()) {
                if (projectRequirements.getProjectReqs().contains(req)) throw new BadRequestException("The requirement with id " + req + " is already inside the project");
            }

            reqProject(projectRequirements.getReqsToCompare(), projectRequirements.getProjectReqs(), similarityModel, threshold, organization, responseId, true, maxNumDeps);
            databaseOperations.finishComputation(organization, responseId);
        } catch (ComponentException e) {
            throw treatComponentException(organization,responseId,true,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,responseId,true,e);
        }
        logger.showInfoMessage("SimReqProject: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void simProject(String responseId, String organization, double threshold, List<String> projectRequirements, int maxNumDeps) throws ComponentException {
        logger.showInfoMessage("SimProject: Start computing " + organization + " " + responseId);
        try {
            databaseOperations.saveResponse(organization, responseId, "SimProject");
            SimilarityModel similarityModel = databaseOperations.getOrganizationModels(organization, true).getSimilarityModel();
            project(projectRequirements, similarityModel, threshold, responseId, organization, maxNumDeps);
            databaseOperations.finishComputation(organization, responseId);
        } catch (ComponentException e) {
            throw treatComponentException(organization,responseId,true,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,responseId,true,e);
        }
        logger.showInfoMessage("SimProject: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void simProjectProject(String responseId, String organization, double threshold, ProjectProject projects, int maxNumDeps) throws ComponentException {
        logger.showInfoMessage("SimProjectProject: Start computing " + organization + " " + responseId);
        try {
            databaseOperations.saveResponse(organization, responseId, "SimProjectProject");
            List<String> project1NotRepeated = deleteListDuplicates(projects.getFirstProjectRequirements());
            List<String> project2NotRepeated = deleteListDuplicates(projects.getSecondProjectRequirements());
            SimilarityModel similarityModel = databaseOperations.getOrganizationModels(organization, true).getSimilarityModel();
            reqProject(project1NotRepeated, project2NotRepeated, similarityModel, threshold, organization, responseId, false, maxNumDeps);
            databaseOperations.finishComputation(organization, responseId);
        } catch (ComponentException e) {
            throw treatComponentException(organization,responseId,true,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,responseId,true,e);
        }
        logger.showInfoMessage("SimProjectProject: Finish computing " + organization + " " + responseId);
    }


    /*
    Public methods
    Similarity with clusters
     */

    @Override
    public void buildClusters(String responseId, boolean compare, double threshold, String organization, MultipartFile file) throws ComponentException {
        logger.showInfoMessage("BuildClusters: Start computing " + organization + " " + responseId);
        try {
            databaseOperations.saveResponse(organization, responseId, "BuildClusters");
            Clusters input = parseMultipartFileToClusters(file);
            if (!input.inputOk()) throw new BadRequestException("The input requirements array is empty");
            if (databaseOperations.existsOrganization(organization)) throw new ForbiddenException(FORBIDDEN_ERROR_MESSAGE);

            List<Requirement> requirements = deleteDuplicates(input.getRequirements());
            SimilarityModel similarityModel = generateModel(compare, requirements);
            ClustersModel clustersModel = clustersAlgorithm.buildModel(requirements,input.getDependencies());
            OrganizationModels organizationModels = new OrganizationModels(threshold, compare, true, similarityModel, clustersModel);

            getAccessToUpdate(organization);
            try {
                databaseOperations.saveOrganizationModels(organization, organizationModels, true, true);
                clustersAlgorithm.updateModel(organization,organizationModels);
            } finally {
                releaseAccessToUpdate(organization);
            }
            generateEmptyResponse(organization, responseId);
        } catch (ComponentException e) {
            throw treatComponentException(organization,responseId,true,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,responseId,true,e);
        }
        logger.showInfoMessage("BuildClusters: Finish computing " + organization + " " + responseId);
    }

    @Override
    public void buildClustersAndCompute(String responseId, boolean compare, String organization, double threshold, int maxNumber, MultipartFile file) throws ComponentException {
        logger.showInfoMessage("BuildClustersAndCompute: Start computing " + organization + " " + responseId);
        try {
            databaseOperations.saveResponse(organization, responseId, "BuildClustersAndCompute");
            Clusters input = parseMultipartFileToClusters(file);
            if (!input.inputOk()) throw new BadRequestException("The input requirements array is empty");
            if (databaseOperations.existsOrganization(organization)) throw new ForbiddenException(FORBIDDEN_ERROR_MESSAGE);

            List<Requirement> requirements = deleteDuplicates(input.getRequirements());
            SimilarityModel similarityModel = generateModel(compare, requirements);
            ClustersModel clustersModel = clustersAlgorithm.buildModel(requirements,input.getDependencies());
            OrganizationModels organizationModels = new OrganizationModels(threshold, compare, true, similarityModel, clustersModel);

            getAccessToUpdate(organization);
            try {
                databaseOperations.saveOrganizationModels(organization, organizationModels, true, true);
                clustersAlgorithm.updateModel(organization,organizationModels);
            } finally {
                releaseAccessToUpdate(organization);
            }

            ResponseDependencies responseDependencies = new DiskDependencies(organization,responseId);
            HashSet<String> repeated = new HashSet<>();
            for (Requirement requirement : requirements) {
                String id = requirement.getId();
                List<Dependency> dependencies = clustersAlgorithm.getReqAcceptedDependencies(organization, id);
                List<Dependency> proposedDependencies = clustersAlgorithm.getReqProposedDependencies(organization, id);
                proposedDependencies.sort(Comparator.comparing(Dependency::getDependencyScore).reversed());
                int highIndex = maxNumber;
                if (maxNumber < 0 || maxNumber > proposedDependencies.size()) highIndex = proposedDependencies.size();
                dependencies.addAll(proposedDependencies.subList(0, highIndex));
                for (Dependency dependency : dependencies) {
                    Dependency aux = new Dependency(dependency.getDependencyScore(), id, dependency.getToid(), dependency.getStatus());
                    if (!repeated.contains(aux.getFromid() + aux.getToid())) {
                        responseDependencies.addDependency(aux);
                        repeated.add(aux.getFromid() + aux.getToid());
                        repeated.add(aux.getToid() + aux.getFromid());
                    }
                }
            }
            responseDependencies.finish();
            databaseOperations.finishComputation(organization, responseId);
        } catch (ComponentException e) {
            throw treatComponentException(organization,responseId,true,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,responseId,true,e);
        }
        logger.showInfoMessage("BuildClustersAndCompute: Finish computing " + organization + " " + responseId);
    }

    @Override
    public Dependencies simReqClusters(String organization, List<String> requirements, int maxNumber) throws ComponentException {
        logger.showInfoMessage("SimReqClusters: Start computing");
        try {
            List<Dependency> result = new ArrayList<>();
            if (!databaseOperations.existsOrganization(organization)) throw new NotFoundException("The organization with id " + organization + " does not exist");
            HashSet<String> repeated = new HashSet<>();

            for (String id : requirements) {
                if (!databaseOperations.existReqInOrganizationModel(organization, id)) throw new NotFoundException("The requirement with id " + id + " is not inside the organization's model");
                List<Dependency> dependencies = clustersAlgorithm.getReqAcceptedDependencies(organization, id);
                List<Dependency> proposedDependencies = clustersAlgorithm.getReqProposedDependencies(organization, id);
                proposedDependencies.sort(Comparator.comparing(Dependency::getDependencyScore).reversed());
                int highIndex = maxNumber;
                if (maxNumber < 0 || maxNumber > proposedDependencies.size()) highIndex = proposedDependencies.size();
                dependencies.addAll(proposedDependencies.subList(0, highIndex));
                for (Dependency dependency : dependencies) {
                    Dependency aux = new Dependency(dependency.getDependencyScore(), id, dependency.getToid(), dependency.getStatus());
                    if (!repeated.contains(aux.getFromid() + aux.getToid())) {
                        result.add(aux);
                        repeated.add(aux.getFromid() + aux.getToid());
                        repeated.add(aux.getToid() + aux.getFromid());
                    }
                }
            }
            logger.showInfoMessage("SimReqClusters: Finish computing");
            return new Dependencies(result);
        } catch (ComponentException e) {
            throw treatComponentException(organization,null,false,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,null,false,e);
        }
    }

    @Override
    public void treatAcceptedAndRejectedDependencies(String organization, List<Dependency> dependencies) throws ComponentException {
        logger.showInfoMessage("TreatAcceptedAndRejectedDependencies: Start computing");
        try {
            getAccessToUpdate(organization);
            try {
                OrganizationModels organizationModels = databaseOperations.getOrganizationModels(organization, false);
                if (!organizationModels.hasClusters()) throw new BadRequestException("The model does not have clusters");

                dependencies.sort(Comparator.comparing(Dependency::computeTime));

                clustersAlgorithm.startUpdateProcess(organization,organizationModels);
                for (Dependency dependency : dependencies) {
                    if (dependency.getDependencyType() != null && dependency.getStatus() != null && dependency.getDependencyType().equals("similar")) {
                        String status = dependency.getStatus();
                        List<Dependency> aux = new ArrayList<>();
                        aux.add(dependency);
                        if (status.equals("accepted")) {
                            clustersAlgorithm.addAcceptedDependencies(organization, aux, organizationModels);
                        } else if (status.equals("rejected")) {
                            clustersAlgorithm.addRejectedDependencies(organization, aux, organizationModels);
                        }
                    }
                }
                clustersAlgorithm.finishUpdateProcess(organization,organizationModels);
            } finally {
                releaseAccessToUpdate(organization);
            }
        } catch (ComponentException e) {
            throw treatComponentException(organization,null,false,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,null,false,e);
        }
        logger.showInfoMessage("TreatAcceptedAndRejectedDependencies: Finish computing");
    }

    @Override
    public void batchProcess(String responseId, String organization, Clusters input) throws ComponentException {
        logger.showInfoMessage("BatchProcess: Start computing " + organization + " " + responseId);
        try {
            databaseOperations.saveResponse(organization, responseId, "BatchProcess");
            getAccessToUpdate(organization);
            try {
                OrganizationModels organizationModels = databaseOperations.getOrganizationModels(organization, false);

                if (!organizationModels.hasClusters()) throw new BadRequestException("The model does not have clusters");
                SimilarityModel similarityModel = organizationModels.getSimilarityModel();
                List<OrderedObject> objects = new ArrayList<>();
                Set<String> notRepeatedReqs = new HashSet<>();

                for (Requirement requirement : input.getRequirements()) {
                    String id = requirement.getId();
                    if (id != null && !notRepeatedReqs.contains(id)) {
                        notRepeatedReqs.add(id);
                        if (similarityModel.containsRequirement(id)) {
                            if (requirementUpdated(requirement, organizationModels))
                                objects.add(new OrderedObject(null, requirement, requirement.getTime()));
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

                objects.sort(Comparator.comparing(OrderedObject::getTime));

                clustersAlgorithm.startUpdateProcess(organization,organizationModels);
                for (OrderedObject orderedObject : objects) {
                    if (orderedObject.isDependency()) {
                        List<Dependency> aux = new ArrayList<>();
                        Dependency dependency = orderedObject.getDependency();
                        aux.add(dependency);
                        if (dependency.getStatus().equals("accepted")) {
                            clustersAlgorithm.addAcceptedDependencies(organization, aux, organizationModels);
                        } else {
                            clustersAlgorithm.addRejectedDependencies(organization, aux, organizationModels);
                        }
                    } else {
                        List<Requirement> aux = new ArrayList<>();
                        Requirement requirement = orderedObject.getRequirement();
                        aux.add(requirement);
                        clustersAlgorithm.addRequirementsToClusters(organization, aux, organizationModels);
                        addRequirementsToModel(organizationModels, aux);
                    }
                }
                databaseOperations.saveOrganizationModels(organization, organizationModels, true, false);
                clustersAlgorithm.finishUpdateProcess(organization,organizationModels);
            } finally {
                releaseAccessToUpdate(organization);
            }
            generateEmptyResponse(organization, responseId);
        } catch (ComponentException e) {
            throw treatComponentException(organization,responseId,true,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,responseId,true,e);
        }
        logger.showInfoMessage("BatchProcess: Finish computing " + organization + " " + responseId);
    }


    /*
    Public methods
    Auxiliary methods
     */

    @Override
    public String getResponsePage(String organization, String responseId) throws ComponentException {
        try {
            return databaseOperations.getResponsePage(organization, responseId);
        } catch (ComponentException e) {
            throw treatComponentException(organization,null,false,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,null,false,e);
        }
    }

    @Override
    public Organization getOrganizationInfo(String organization) throws ComponentException {
        try {
            return databaseOperations.getOrganizationInfo(organization);
        } catch (ComponentException e) {
            throw treatComponentException(organization,null,false,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,null,false,e);
        }
    }

    @Override
    public void deleteOrganizationResponses(String organization) throws ComponentException {
        try {
            databaseOperations.deleteOrganizationResponses(organization);
        } catch (ComponentException e) {
            throw treatComponentException(organization,null,false,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,null,false,e);
        }
    }

    @Override
    public void deleteOrganization(String organization) throws ComponentException {
        try {
            getAccessToUpdate(organization);
            databaseOperations.deleteOrganization(organization);
        } catch (ComponentException e) {
            throw treatComponentException(organization,null,false,e);
        } catch (Exception e) {
            throw treatUnexpectedException(organization,null,false,e);
        } finally {
            releaseAccessToUpdate(organization);
        }
    }

    @Override
    public void clearDatabase() throws ComponentException {
        try {
            databaseOperations.clearDatabase();
        } catch (ComponentException e) {
            throw treatComponentException(null,null,false,e);
        } catch (Exception e) {
            throw treatUnexpectedException(null,null,false,e);
        }
    }



    /*
    Private methods
     */

    private Clusters parseMultipartFileToClusters(MultipartFile file) throws BadRequestException, InternalErrorException {
        try(InputStream inputStream = new BufferedInputStream(file.getInputStream())) {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(inputStream, StandardCharsets.US_ASCII));
            return new Clusters(jsonObject);
        } catch (IOException e) {
            logger.showErrorMessage(e.getMessage());
            throw new InternalErrorException("Error while converting input json file");
        } catch (ParseException e) {
            logger.showInfoMessage(e.getMessage());
            throw new BadRequestException("The input json file is not well build");
        }
    }

    private List<String> deleteListDuplicates(List<String> inputList) {
        HashSet<String> notRepeated = new HashSet<>(inputList);
        return new ArrayList<>(notRepeated);
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

        for (String req1: reqsToCompare) {
            if (similarityModel.containsRequirement(req1)) {
                for (String req2 : projectRequirements) {
                    if (!req1.equals(req2) && similarityModel.containsRequirement(req2)) {
                        double score = similarityAlgorithm.computeSimilarity(similarityModel,req1,req2);
                        if (score >= threshold) {
                            Dependency dependency = new Dependency(score, req1, req2);
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

        for (int i = 0; i < projectRequirements.size(); ++i) {
            String req1 = projectRequirements.get(i);
            if (similarityModel.containsRequirement(req1)) {
                for (int j = i + 1; j < projectRequirements.size(); ++j) {
                    String req2 = projectRequirements.get(j);
                    if (!req2.equals(req1) && similarityModel.containsRequirement(req2)) {
                        double score = similarityAlgorithm.computeSimilarity(similarityModel,req1,req2);
                        if (score >= threshold) {
                            Dependency dependency = new Dependency(score, req1, req2);
                            responseDependencies.addDependency(dependency);
                        }
                    }
                }
            }
        }
        responseDependencies.finish();
    }

    private List<Requirement> deleteDuplicates(List<Requirement> requirements) throws ComponentException {
        HashSet<String> ids = new HashSet<>();
        List<Requirement> result = new ArrayList<>();
        for (Requirement requirement : requirements) {
            if (requirement.getId() == null) throw new BadRequestException("There is a requirement without id.");
            if (!ids.contains(requirement.getId())) {
                result.add(requirement);
                ids.add(requirement.getId());
            }
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

    private void deleteRequirementsFromModel(SimilarityModel similarityModel, List<Requirement> requirements) throws InternalErrorException {
        List<String> requirementIds = new ArrayList<>();
        for (Requirement requirement: requirements) {
            String id = requirement.getId();
            if (similarityModel.containsRequirement(id))requirementIds.add(id);
        }
        if (!requirementIds.isEmpty()) similarityAlgorithm.deleteRequirements(similarityModel,requirementIds);
    }

    private void generateEmptyResponse(String organization, String responseId) throws InternalErrorException {
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject();
            jsonObject.put("status", 200);
            databaseOperations.saveResponsePage(organization, responseId, jsonObject.toString());
            databaseOperations.finishComputation(organization, responseId);
        } catch (NotFoundException e) {
            throw new InternalErrorException("Error while saving response");
        }
    }

    private ComponentException treatComponentException(String organization, String responseId, boolean saveException, ComponentException e) throws InternalErrorException {
        if (e.getStatus() == 500) logger.showErrorMessage(e.getMessage() + " " + organization + " " + responseId);
        else logger.showInfoMessage(e.getMessage() + " " + organization + " " + responseId);
        if (saveException) databaseOperations.saveExceptionAndFinishComputation(organization, responseId, createJsonException(e.getStatus(), e.getError(), e.getMessage()));
        return e;
    }

    private InternalErrorException treatUnexpectedException(String organization, String responseId, boolean saveException, Exception e) throws InternalErrorException {
        logger.showErrorMessage(e.getMessage() + " " + organization + " " + responseId);
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        logger.showErrorMessage(errors.toString());
        if (saveException) databaseOperations.saveExceptionAndFinishComputation(organization, responseId, createJsonException(500, "Internal error", "Unexpected error. See the logs for more information"));
        return new InternalErrorException("Unexpected error: " + e.getMessage());
    }

    private String createJsonException(int status, String error, String message) {
        org.json.JSONObject result = new org.json.JSONObject();
        result.put("status",status);
        result.put("error",error);
        result.put("message",message);
        return result.toString();
    }
}
