package upc.similarity.compareapi.service;

import upc.similarity.compareapi.algorithms.similarity_algorithm.SimilarityAlgorithm;
import upc.similarity.compareapi.algorithms.similarity_algorithm.SimilarityModel;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.exception.InternalErrorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequirementsSimilarity {

    private SimilarityAlgorithm similarityAlgorithm;

    public RequirementsSimilarity(SimilarityAlgorithm similarityAlgorithm) {
        this.similarityAlgorithm = similarityAlgorithm;
    }

    public OrganizationModels buildModel(Map<String, List<String>> requirements_tokens, List<Requirement> requirements_info, boolean useComponent) throws InternalErrorException {
        SimilarityModel similarityModel = similarityAlgorithm.buildModel(requirements_tokens);

        //Computes each requirement component
        Map<String,String> reqComponent = new HashMap<>();
        if (useComponent) {
            for (Requirement requirement : requirements_info) {
                reqComponent.put(requirement.getId(), requirement.getComponent());
            }
        }

        return new OrganizationModels(similarityModel,reqComponent);
    }

    public double computeSimilarity(OrganizationModels organizationModels, String requirementIdA, String requirementIdB) throws InternalErrorException {
        double score = similarityAlgorithm.computeSimilarity(organizationModels.getSimilarityModel(),requirementIdA,requirementIdB);
        if (organizationModels.isUseComponent()) {
            Map<String,String> reqComponent = organizationModels.getReqComponent();
            String componentA = reqComponent.get(requirementIdA);
            String componentB = reqComponent.get(requirementIdB);
            if (componentA != null && componentB != null && !componentA.equals(componentB)) score *= 0.33;
        }
        return score;
    }

    public void addRequirements(OrganizationModels organizationModels, Map<String, List<String>> requirements_tokens, List<Requirement> requirements_info) throws InternalErrorException {
        similarityAlgorithm.addRequirements(organizationModels.getSimilarityModel(),requirements_tokens);

        //Computes reqComponent map for the new requirements
        if (organizationModels.isUseComponent()) {
            Map<String, String> reqComponent = organizationModels.getReqComponent();
            for (Requirement requirement : requirements_info) {
                reqComponent.put(requirement.getId(), requirement.getComponent());
            }
        }
    }

    public void deleteRequirements(OrganizationModels organizationModels, List<String> requirements) throws InternalErrorException {
        similarityAlgorithm.deleteRequirements(organizationModels.getSimilarityModel(),requirements);

        //Updates reqComponent structure
        if (organizationModels.isUseComponent()) organizationModels.getReqComponent().keySet().removeAll(requirements);
    }
}
