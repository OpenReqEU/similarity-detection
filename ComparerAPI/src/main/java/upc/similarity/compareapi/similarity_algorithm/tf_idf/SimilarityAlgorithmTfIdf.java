package upc.similarity.compareapi.similarity_algorithm.tf_idf;

import upc.similarity.compareapi.entity.exception.InternalErrorException;
import upc.similarity.compareapi.util.Logger;
import upc.similarity.compareapi.similarity_algorithm.SimilarityModel;
import upc.similarity.compareapi.similarity_algorithm.SimilarityAlgorithm;

import java.util.*;

public class SimilarityAlgorithmTfIdf implements SimilarityAlgorithm {

    private CosineSimilarityTfIdf cosineSimilarityTfIdf = CosineSimilarityTfIdf.getInstance();
    private double cutOffValue;
    private boolean cutOffDummy;
    private boolean smoothingActive;

    public SimilarityAlgorithmTfIdf(double cutOffValue, boolean cutOffDummy, boolean smoothingActive) {
        this.cutOffValue = cutOffValue;
        this.cutOffDummy = cutOffDummy;
        this.smoothingActive = smoothingActive;
    }


    @Override
    public SimilarityModelTfIdf buildModel(Map<String, List<String>> requirements) {
        double cutOffParameter = computeCutOffParameter(requirements.size());
        boolean smoothing = (requirements.size() < 100);
        Logger.getInstance().showInfoMessage("Cutoff: " + cutOffParameter);

        Map<String,Map<String, Double>> tfIdfValues = new HashMap<>();
        Map<String, Integer> frequencyValues = new HashMap<>();
        List<Map<String, Integer>> tfValues = new ArrayList<>();

        for(Map.Entry<String,List<String>> requirement : requirements.entrySet()) {
            tfValues.add(tf(requirement.getValue(),frequencyValues));
        }

        int i = 0;
        for (Map.Entry<String,List<String>> requirement : requirements.entrySet()) {
            HashMap<String, Double> aux = new HashMap<>();
            for (String token : requirement.getValue()) {
                double idf = idf(requirements.size(), frequencyValues.get(token), smoothing);
                int tf = tfValues.get(i).get(token);
                double tfidf = tf * idf;
                if (tfidf>=cutOffParameter) aux.put(token, tfidf);
            }
            tfIdfValues.put(requirement.getKey(),aux);
            ++i;
        }

        return new SimilarityModelTfIdf(tfIdfValues,frequencyValues);
    }

    @Override
    public double computeSimilarity(SimilarityModel similarityModel, String requirementIdA, String requirementIdB) throws InternalErrorException {
        try {
            SimilarityModelTfIdf modelTfIdf = (SimilarityModelTfIdf) similarityModel;
            return cosineSimilarityTfIdf.compute(modelTfIdf, requirementIdA, requirementIdB);
        } catch (ClassCastException e) {
            throw new InternalErrorException("Error while computing similarity with tf_idf algorithm without a tf_idf model");
        }
    }


    @Override
    public void addRequirements(SimilarityModel similarityModel, Map<String,List<String>> requirements) throws InternalErrorException {
        try {
            SimilarityModelTfIdf modelTfIdf = (SimilarityModelTfIdf) similarityModel;
            Map<String, Integer> newCorpusFrequency = modelTfIdf.getCorpusFrequency();
            Map<String, Integer> oldCorpusFrequency = cloneCorpusFrequency(newCorpusFrequency);
            Map<String, Map<String, Double>> docs = modelTfIdf.getDocs();

            int oldSize = docs.size();
            int finalSize = oldSize + requirements.size();
            double cutOffParameter = computeCutOffParameter(finalSize);
            boolean smoothing = (finalSize < 100);

            //tf new requirements
            List<Map<String, Integer>> wordBagArray = new ArrayList<>();
            for (Map.Entry<String, List<String>> requirement : requirements.entrySet()) {
                List<String> tokens = requirement.getValue();
                wordBagArray.add(tf(tokens, newCorpusFrequency));
            }

            //idf old requirements
            recomputeIdfValues(docs, oldCorpusFrequency, newCorpusFrequency, oldSize, finalSize);

            //idf new requirements
            int i = 0;
            for (Map.Entry<String, List<String>> requirement : requirements.entrySet()) {
                String id = requirement.getKey();
                List<String> tokens = requirement.getValue();
                HashMap<String, Double> aux = new HashMap<>();
                for (String s : tokens) {
                    double idf = idf(finalSize, newCorpusFrequency.get(s), smoothing);
                    Integer tf = wordBagArray.get(i).get(s);
                    double tfidf = idf * tf;
                    if (tfidf >= cutOffParameter) aux.put(s, tfidf);
                }
                docs.put(id, aux);
                ++i;
            }
        } catch (ClassCastException e) {
            throw new InternalErrorException("Error while adding requirements with tf_idf algorithm without a tf_idf model");
        }
    }

    @Override
    public void deleteRequirements(SimilarityModel similarityModel, List<String> requirements) throws InternalErrorException {
        try {
            SimilarityModelTfIdf modelTfIdf = (SimilarityModelTfIdf) similarityModel;
            Map<String, Map<String, Double>> docs = modelTfIdf.getDocs();
            Map<String, Integer> newCorpusFrequency = modelTfIdf.getCorpusFrequency();
            Map<String, Integer> oldCorpusFrequency = cloneCorpusFrequency(newCorpusFrequency);

            int oldSize = docs.size();
            int newSize = oldSize;

            for (String id : requirements) {
                if (docs.containsKey(id)) { //problem: if the requirement had this word before applying cutoff parameter
                    --newSize;
                    Map<String, Double> words = docs.get(id);
                    for (String word : words.keySet()) {
                        int value = newCorpusFrequency.get(word);
                        if (value == 1) newCorpusFrequency.remove(word);
                        else newCorpusFrequency.put(word, value - 1);
                    }
                    docs.remove(id);
                }
            }
            recomputeIdfValues(docs, oldCorpusFrequency, newCorpusFrequency, oldSize, newSize);
        } catch (ClassCastException e) {
            throw new InternalErrorException("Error while deleting requirements with tf_idf algorithm without a tf_idf model");
        }
    }


    /*
    Private methods
     */


    private Map<String, Integer> cloneCorpusFrequency(Map<String, Integer> corpusFrequency) {
        Map<String, Integer> oldCorpusFrequency = new HashMap<>();
        for (Map.Entry<String, Integer> entry : corpusFrequency.entrySet()) {
            String word = entry.getKey();
            int value = entry.getValue();
            oldCorpusFrequency.put(word, value);
        }
        return oldCorpusFrequency;
    }

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

    private void recomputeIdfValues(Map<String, Map<String, Double>> docs, Map<String, Integer> oldCorpusFrequency, Map<String, Integer> newCorpusFrequency, double oldSize, double newSize) {
        for (Map.Entry<String, Map<String, Double>> requirement : docs.entrySet()) {
            Map<String, Double> words = requirement.getValue();
            for (Map.Entry<String, Double> word : words.entrySet()) { //problem: if the value was 0 (because corpus + 1 == totalSize) it will be always 0
                String wordId = word.getKey();
                double score = word.getValue();
                double newScore = recomputeIdf(score, oldSize, oldCorpusFrequency.get(wordId), newSize, newCorpusFrequency.get(wordId));
                word.setValue(newScore);
            }
        }
    }

    private double recomputeIdf(double oldValue, double oldSize, double oldCorpusFrequency, double newSize, double newCorpusFrequency) {
        double quocient = Math.log(oldSize/(oldCorpusFrequency+1));
        return (quocient <= 0) ? 0 : (oldValue * Math.log(newSize/(newCorpusFrequency+1)))/quocient;
    }
}
