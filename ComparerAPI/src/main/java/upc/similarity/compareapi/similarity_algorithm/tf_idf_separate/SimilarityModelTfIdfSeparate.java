package upc.similarity.compareapi.similarity_algorithm.tf_idf_separate;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.compareapi.similarity_algorithm.SimilarityModel;

import java.util.*;

public class SimilarityModelTfIdfSeparate implements SimilarityModel {

    private Map<String, Map<String, Double>> names;
    private Map<String, Map<String, Double>> texts;
    private Map<String, Integer> corpusFrequency;

    public SimilarityModelTfIdfSeparate(Map<String, Map<String, Double>> names, Map<String, Map<String, Double>> texts, Map<String, Integer> corpusFrequency) {
        this.names = names;
        this.texts = texts;
        this.corpusFrequency = corpusFrequency;
    }

    @Override
    public boolean containsRequirement(String requirementId) {
        return names.containsKey(requirementId);
    }

    @Override
    public List<String> getRequirementsIds() {
        return new ArrayList<>(names.keySet());
    }

    @Override
    public boolean checkIfRequirementIsUpdated(String requirementId, List<String> tokens) {
        //Not implemented
        return true;
    }

    /*
    Get methods
     */

    public Map<String, Map<String, Double>> getNames() {
        return names;
    }

    public Map<String, Map<String, Double>> getTexts() {
        return texts;
    }

    public Map<String, Integer> getCorpusFrequency() {
        return corpusFrequency;
    }

    /*
    Set methods
     */

    public void setNames(Map<String, Map<String, Double>> names) {
        this.names = names;
    }

    public void setTexts(Map<String, Map<String, Double>> texts) {
        this.texts = texts;
    }

    public void setCorpusFrequency(Map<String, Integer> corpusFrequency) {
        this.corpusFrequency = corpusFrequency;
    }
}
