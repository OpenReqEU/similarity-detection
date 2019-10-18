package upc.similarity.compareapi.similarity_algorithm;

import upc.similarity.compareapi.exception.InternalErrorException;

import java.util.List;
import java.util.Map;

public interface SimilarityAlgorithm {

    /**
     * Builds a model with the input requirements to be used in the future to compare the similarity between requirements
     * @param requirements a map with each requirement id as key and the preprocessed tokens as value
     * @return a model containing the similarity algorithm representation
     * @throws InternalErrorException
     */
    SimilarityModel buildModel(Map<String, List<String>> requirements) throws InternalErrorException;

    /**
     * @param similarityModel the model containing all the requirements information
     * @param requirementIdA the id of the first requirement
     * @param requirementIdB the id of the second requirement
     * @return a double score that defines the similarity between the two requirements
     */
    double computeSimilarity(SimilarityModel similarityModel, String requirementIdA, String requirementIdB);

    /**
     * Adds the input requirements to the input model
     * @param similarityModel algorithm model
     * @param requirements requirements to add, consists of a map with each requirement id as key and the preprocessed tokens as value. Maybe the input requirements are already inside the model
     */
    void addRequirements(SimilarityModel similarityModel, Map<String, List<String>> requirements);

    /**
     * Deletes the input requirements from the input model
     * @param similarityModel algorithm model
     * @param requirements ids of the requirements to delete, maybe the input requirements are not inside the model
     */
    void deleteRequirements(SimilarityModel similarityModel, List<String> requirements);
}
