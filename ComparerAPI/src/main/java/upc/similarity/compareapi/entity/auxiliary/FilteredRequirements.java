package upc.similarity.compareapi.entity.auxiliary;

import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.exception.InternalErrorException;
import upc.similarity.compareapi.preprocess.PreprocessPipeline;
import upc.similarity.compareapi.similarity_algorithm.SimilarityModel;

import java.util.*;

public class FilteredRequirements {

    private List<Requirement> newRequirements;
    private List<Requirement> updatedRequirements;
    private List<Requirement> deletedRequirements;
    private Map<String, Pair<String,Long>> reqDepsToRemove;

    public FilteredRequirements(List<Requirement> requirements, OrganizationModels organizationModels) throws InternalErrorException{
        Collection<Requirement> filteredRequirements = filterRequirements(requirements);
        newRequirements = new ArrayList<>();
        updatedRequirements = new ArrayList<>();
        deletedRequirements = new ArrayList<>();
        splitRequirements(filteredRequirements,organizationModels);
    }

    public List<Requirement> getNewRequirements() {
        return newRequirements;
    }

    public List<Requirement> getUpdatedRequirements() {
        return updatedRequirements;
    }

    public List<Requirement> getDeletedRequirements() {
        return deletedRequirements;
    }

    public Map<String, Pair<String, Long>> getReqDepsToRemove() {
        return reqDepsToRemove;
    }

    /*
    Private methods
     */

    private Collection<Requirement> filterRequirements(List<Requirement> requirements) {
        Map<String,Requirement> notRepeatedReqs = new HashMap<>();
        for (Requirement requirement: requirements) {
            String id = requirement.getId();
            if (id != null) {
                if (notRepeatedReqs.containsKey(id)) {
                    Requirement oldRequirement = notRepeatedReqs.get(id);
                    if (oldRequirement.getTime() < requirement.getTime()) notRepeatedReqs.put(id,requirement);
                } else notRepeatedReqs.put(id,requirement);
            }
        }
        return notRepeatedReqs.values();
    }

    private void splitRequirements(Collection<Requirement> requirements, OrganizationModels organizationModels) throws InternalErrorException {
        reqDepsToRemove = new HashMap<>();
        PreprocessPipeline preprocessPipeline = Constants.getInstance().getPreprocessPipeline();
        SimilarityModel similarityModel = organizationModels.getSimilarityModel();
        for (Requirement requirement: requirements) {
            String id = requirement.getId();
            String status = requirement.getStatus();
            long time = requirement.getTime();
            if (status != null && status.equals("deleted")) {
                deletedRequirements.add(requirement);
                reqDepsToRemove.put(id,new Pair<>("all",time));
            }
            else if (similarityModel.containsRequirement(id)) {
                if (requirementUpdated(requirement, preprocessPipeline, organizationModels)) {
                    updatedRequirements.add(requirement);
                    reqDepsToRemove.put(id,new Pair<>("before",time));
                }
            } else {
                newRequirements.add(requirement);
                reqDepsToRemove.put(id,new Pair<>("before",time));
            }
        }
    }

    private boolean requirementUpdated(Requirement requirement, PreprocessPipeline preprocessPipeline, OrganizationModels organizationModels) throws InternalErrorException {
        List<Requirement> requirements = new ArrayList<>();
        requirements.add(requirement);
        Map<String, List<String>> preprocessedRequirement = preprocessPipeline.preprocessRequirements(organizationModels.isCompare(),requirements);
        return organizationModels.getSimilarityModel().checkIfRequirementIsUpdated(requirement.getId(),preprocessedRequirement.get(requirement.getId()));
    }
}
