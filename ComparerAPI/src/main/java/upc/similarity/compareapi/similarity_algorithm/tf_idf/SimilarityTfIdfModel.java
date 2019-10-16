package upc.similarity.compareapi.similarity_algorithm.tf_idf;

import upc.similarity.compareapi.similarity_algorithm.SimilarityModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimilarityTfIdfModel implements SimilarityModel {

    private Map<String, Map<String, Double>> docs;
    private Map<String, Integer> corpusFrequency;

    public SimilarityTfIdfModel(Map<String, Map<String, Double>> docs, Map<String, Integer> corpusFrequency) {
        this.docs = docs;
        this.corpusFrequency = corpusFrequency;
    }

    @Override
    public boolean containsRequirement(String requirementId) {
        return docs.containsKey(requirementId);
    }

    @Override
    public List<String> getRequirementsIds() {
        return new ArrayList<>(docs.keySet());
    }

    /*
    Get methods
     */

    public Map<String, Map<String, Double>> getDocs() {
        return docs;
    }

    public Map<String, Integer> getCorpusFrequency() {
        return corpusFrequency;
    }

    /*
    Set methods
     */

    public void setDocs(Map<String, Map<String, Double>> docs) {
        this.docs = docs;
    }

    public void setCorpusFrequency(Map<String, Integer> corpusFrequency) {
        this.corpusFrequency = corpusFrequency;
    }
}
