package upc.similarity.compareapi.algorithms.similarity_algorithm.tf_idf_double;

import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.exception.InternalErrorException;
import upc.similarity.compareapi.algorithms.similarity_algorithm.SimilarityAlgorithm;
import upc.similarity.compareapi.algorithms.similarity_algorithm.SimilarityModel;
import upc.similarity.compareapi.algorithms.similarity_algorithm.tf_idf.SimilarityAlgorithmTfIdf;
import upc.similarity.compareapi.algorithms.similarity_algorithm.tf_idf.SimilarityModelTfIdf;

import java.util.List;
import java.util.Map;

public class SimilarityAlgorithmTfIdfDouble implements SimilarityAlgorithm {

    private SimilarityAlgorithmTfIdf similarityAlgorithmTfIdf;
    private CosineSimilarityTfIdfDouble cosineSimilarityTfIdfDouble;

    public SimilarityAlgorithmTfIdfDouble(double cutOffValue, boolean cutOffDummy, boolean smoothingActive, double topicThreshold, double cutOffTopics, double importanceLow) {
        this.similarityAlgorithmTfIdf = new SimilarityAlgorithmTfIdf(cutOffValue,cutOffDummy,smoothingActive);
        this.cosineSimilarityTfIdfDouble = new CosineSimilarityTfIdfDouble(topicThreshold,cutOffTopics,importanceLow);
    }

    @Override
    public SimilarityModel buildModel(Map<String, List<String>> requirements, List<Requirement> requirements_info, boolean useComponent) throws InternalErrorException {
        return similarityAlgorithmTfIdf.buildModel(requirements,requirements_info,useComponent);
    }

    @Override
    public double computeSimilarity(SimilarityModel similarityModel, String requirementIdA, String requirementIdB) throws InternalErrorException {
        try {
            SimilarityModelTfIdf modelTfIdf = (SimilarityModelTfIdf) similarityModel;
            return cosineSimilarityTfIdfDouble.compute(modelTfIdf, requirementIdA, requirementIdB);
        } catch (ClassCastException e) {
            throw new InternalErrorException("Error while computing similarity with tf_idf algorithm without a tf_idf model");
        }
    }

    @Override
    public void addRequirements(SimilarityModel similarityModel, Map<String, List<String>> requirements_tokens, List<Requirement> requirements_info) throws InternalErrorException {
        similarityAlgorithmTfIdf.addRequirements(similarityModel,requirements_tokens,requirements_info);
    }

    @Override
    public void deleteRequirements(SimilarityModel similarityModel, List<String> requirements) throws InternalErrorException {
        similarityAlgorithmTfIdf.deleteRequirements(similarityModel,requirements);
    }
}
