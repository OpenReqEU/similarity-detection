package upc.similarity.compareapi.similarity_algorithm;

import upc.similarity.compareapi.entity.exception.InternalErrorException;

import java.util.List;
import java.util.Map;

public interface SimilarityAlgorithm {

    /**
     * Builds a model with the input requirements to be used in the future to compare the similarity between requirements
     * @param requirements a map with each requirement id as key and the preprocessed tokens as value
     * @return a model containing the similarity algorithm representation
     */
    SimilarityModel buildModel(Map<String, List<String>> requirements) throws InternalErrorException;

    /**
     * Computes the similarity score between two requirements of the model
     * @param similarityModel the model containing all the requirements information
     * @param requirementIdA the id of the first requirement (is inside the input model)
     * @param requirementIdB the id of the second requirement (is inside the input model)
     * @return a double score that defines the similarity between the two requirements
     */
    double computeSimilarity(SimilarityModel similarityModel, String requirementIdA, String requirementIdB) throws InternalErrorException;

    /**
     * Adds the input requirements to the input model
     * @param similarityModel algorithm model
     * @param requirements requirements to add, consists of a map with each requirement id as key and the preprocessed tokens as value. The input requirements are not inside the model
     */
    void addRequirements(SimilarityModel similarityModel, Map<String, List<String>> requirements) throws InternalErrorException;

    /**
     * Deletes the input requirements from the input model
     * @param similarityModel algorithm model
     * @param requirements ids of the requirements to delete, maybe the input requirements are not inside the model
     */
    void deleteRequirements(SimilarityModel similarityModel, List<String> requirements) throws InternalErrorException;
}
