package upc.similarity.compareapi.service;

import upc.similarity.compareapi.algorithms.similarity_algorithm.SimilarityAlgorithm;
import upc.similarity.compareapi.algorithms.similarity_algorithm.SimilarityModel;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.exception.InternalErrorException;
import upc.similarity.compareapi.util.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequirementsSimilarity {

    private SimilarityAlgorithm similarityAlgorithm;
    private Logger logger = Logger.getInstance();

    public RequirementsSimilarity(SimilarityAlgorithm similarityAlgorithm) {
        this.similarityAlgorithm = similarityAlgorithm;
    }

    public OrganizationModels buildModel(Map<String, List<String>> requirementsTokens, List<Requirement> requirementsInfo, boolean useComponent) throws InternalErrorException {
        SimilarityModel similarityModel = similarityAlgorithm.buildModel(requirementsTokens);

        //Computes each requirement component
        Map<String,String> reqComponent = new HashMap<>();
        if (useComponent) {
            for (Requirement requirement : requirementsInfo) {
                reqComponent.put(requirement.getId(), requirement.getComponent());
            }
        }

        return new OrganizationModels(similarityModel,reqComponent);
    }

    public double computeSimilarity(OrganizationModels organizationModels, String requirementIdA, String requirementIdB) throws InternalErrorException {
        long start = System.nanoTime();
        double score = similarityAlgorithm.computeSimilarity(organizationModels.getSimilarityModel(),requirementIdA,requirementIdB);
        if (organizationModels.isUseComponent()) {
            Map<String,String> reqComponent = organizationModels.getReqComponent();
            String componentA = reqComponent.get(requirementIdA);
            String componentB = reqComponent.get(requirementIdB);
            if (componentA != null && componentB != null && !componentA.equals(componentB)) score *= 0.33;
        }
        long finish = System.nanoTime();
        long timeElapsed = finish - start;
        logger.showInfoMessage("-------------------- Comparison time2: " + timeElapsed);
        return score;
    }

    public void addRequirements(OrganizationModels organizationModels, Map<String, List<String>> requirementsTokens, List<Requirement> requirementsInfo) throws InternalErrorException {
        similarityAlgorithm.addRequirements(organizationModels.getSimilarityModel(),requirementsTokens);

        //Computes reqComponent map for the new requirements
        if (organizationModels.isUseComponent()) {
            Map<String, String> reqComponent = organizationModels.getReqComponent();
            for (Requirement requirement : requirementsInfo) {
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
