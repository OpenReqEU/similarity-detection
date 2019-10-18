package upc.similarity.compareapi.similarity_algorithm;

import upc.similarity.compareapi.entity.Requirement;

import java.util.List;

public interface SimilarityModel {

    /**
     * checks if the requirement with the input id is inside the model
     * @param requirementId the id of the requirement to search
     * @return true is the requirement is inside the model, false otherwise
     */
    boolean containsRequirement(String requirementId);

    /**
     *
     * @return an array with the ids of all the requirements of the model
     */
    List<String> getRequirementsIds();

    /**
     * Checks if the input tokens are equal to the requirement inside the model specified with the input id
     * @param requirementId the id of the requirement to check, must be inside the model (mandatory)
     * @param tokens the new tokens of the requirement
     * @return true if the requirement has been changed, false otherwise
     */
    boolean checkIfRequirementIsUpdated(String requirementId, List<String> tokens);


}
