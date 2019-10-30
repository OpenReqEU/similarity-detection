package upc.similarity.compareapi.similarity_algorithm.tf_idf_separate;

import upc.similarity.compareapi.entity.exception.InternalErrorException;
import upc.similarity.compareapi.similarity_algorithm.SimilarityAlgorithm;
import upc.similarity.compareapi.similarity_algorithm.SimilarityModel;
import upc.similarity.compareapi.similarity_algorithm.tf_idf.SimilarityAlgorithmTfIdf;
import upc.similarity.compareapi.similarity_algorithm.tf_idf.SimilarityModelTfIdf;
import upc.similarity.compareapi.similarity_algorithm.tf_idf_double.CosineSimilarityTfIdfDouble;
import upc.similarity.compareapi.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimilarityAlgorithmTfIdfSeparate implements SimilarityAlgorithm {

    private CosineSimilarityTfIdfSeparate cosineSimilarityTfIdfSeparate;
    private double cutOffValue;
    private boolean cutOffDummy;
    private boolean smoothingActive;

    public SimilarityAlgorithmTfIdfSeparate(double cutOffValue, boolean cutOffDummy, boolean smoothingActive, double cutOffTopics, double importanceLow) {
        this.cutOffValue = cutOffValue;
        this.cutOffDummy = cutOffDummy;
        this.smoothingActive = smoothingActive;
        this.cosineSimilarityTfIdfSeparate = new CosineSimilarityTfIdfSeparate(cutOffTopics,importanceLow);
    }

    @Override
    public SimilarityModelTfIdfSeparate buildModel(Map<String, List<String>> requirements) throws InternalErrorException {
        double cutOffParameter = computeCutOffParameter(requirements.size());
        boolean smoothing = (requirements.size() < 100);
        Logger.getInstance().showInfoMessage("Cutoff: " + cutOffParameter);

        Map<String,Map<String, Double>> nameTfIdfValues = new HashMap<>();
        Map<String,Map<String, Double>> textTfIdfValues = new HashMap<>();
        Map<String, Integer> frequencyValues = new HashMap<>();
        Map<String, Map<String, Integer>> tfValues = new HashMap<>();

        for(Map.Entry<String,List<String>> requirement : requirements.entrySet()) {
            tfValues.put(requirement.getKey(),(tf(requirement.getValue(),frequencyValues)));
        }

        for (Map.Entry<String,List<String>> requirement : requirements.entrySet()) {
            HashMap<String, Double> aux = new HashMap<>();
            String id = requirement.getKey();
            for (String token : requirement.getValue()) {
                double idf = idf(requirements.size(), frequencyValues.get(token), smoothing);
                int tf = tfValues.get(id).get(token);
                double tfidf = tf * idf;
                if (tfidf>=cutOffParameter) aux.put(token, tfidf);
            }
            if (id.contains("__text__")) {
                id = id.replace("__text__","");
                textTfIdfValues.put(id,aux);
            } else nameTfIdfValues.put(id,aux);
        }

        return new SimilarityModelTfIdfSeparate(nameTfIdfValues,textTfIdfValues,frequencyValues);
    }

    @Override
    public double computeSimilarity(SimilarityModel similarityModel, String requirementIdA, String requirementIdB) throws InternalErrorException {
        try {
            SimilarityModelTfIdfSeparate modelTfIdf = (SimilarityModelTfIdfSeparate) similarityModel;
            return cosineSimilarityTfIdfSeparate.compute(modelTfIdf, requirementIdA, requirementIdB);
        } catch (ClassCastException e) {
            throw new InternalErrorException("Error while computing similarity with tf_idf algorithm without a tf_idf model");
        }
    }

    @Override
    public void addRequirements(SimilarityModel similarityModel, Map<String, List<String>> requirements) throws InternalErrorException {
        //not implemented
    }

    @Override
    public void deleteRequirements(SimilarityModel similarityModel, List<String> requirements) throws InternalErrorException {
        //not implemented
    }


    /*
    Private methods
     */

    private double computeCutOffParameter(long totalSize) {
        if (cutOffDummy || totalSize < 100) return -1;
        else return cutOffValue;
    }

    private Map<String, Integer> tf(List<String> tokens, Map<String, Integer> frequencyValues) {
        Map<String, Integer> frequency = new HashMap<>();
        for (String s : tokens) {
            if (frequency.containsKey(s)) frequency.put(s, frequency.get(s) + 1);
            else {
                frequency.put(s, 1);
                if (frequencyValues.containsKey(s)) frequencyValues.put(s, frequencyValues.get(s) + 1);
                else frequencyValues.put(s, 1);
            }
        }
        return frequency;
    }
    private double idf(int size, int frequency, boolean smoothing) {
        double value = Math.log(size / (frequency + 1.0));
        if (value < 0) value = 0;
        return (smoothing && smoothingActive) ? (value + 0.1) : value;
    }


}
