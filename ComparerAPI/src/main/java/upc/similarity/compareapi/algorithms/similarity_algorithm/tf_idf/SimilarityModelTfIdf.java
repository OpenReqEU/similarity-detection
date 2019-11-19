package upc.similarity.compareapi.algorithms.similarity_algorithm.tf_idf;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.compareapi.algorithms.similarity_algorithm.SimilarityModel;

import java.util.*;

public class SimilarityModelTfIdf implements SimilarityModel {

    /**
     * A map with the tf-idf metric of all the words of each requirement. It is represented as a Map with a
     * String key (requirement id) and a bag of words which are saved as objects with a String value (word id)
     * and their corresponding tf-idf value (a double)
     */
    private Map<String, Map<String, Double>> docs;

    /**
     * A map with the frequency of each word in the corpus. It is implemented as a Map with a String key (word id)
     * and an Integer number which represents the number of requirements that contain this word. It is useful when
     * updating the model with new requirements or deleting old ones.
     */
    private Map<String, Integer> corpusFrequency;

    public SimilarityModelTfIdf(Map<String, Map<String, Double>> docs, Map<String, Integer> corpusFrequency) {
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

    @Override
    public boolean checkIfRequirementIsUpdated(String requirementId, List<String> tokens) {
        Set<String> newRequirement = new HashSet<>(tokens);
        Set<String> oldRequirement = docs.get(requirementId).keySet();
        return !oldRequirement.equals(newRequirement);
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


    /*
    Test purpose methods
     */

    public JSONObject extractModel(boolean withDocs, boolean withFrequency) {
        JSONArray reqsArray = new JSONArray();
        if (withDocs) {
            Iterator it = docs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                String id = (String) pair.getKey();
                HashMap<String, Double> words = (HashMap<String, Double>) pair.getValue();
                Iterator it2 = words.entrySet().iterator();
                JSONArray wordsArray = new JSONArray();
                while (it2.hasNext()) {
                    Map.Entry pair2 = (Map.Entry) it2.next();
                    String word = (String) pair2.getKey();
                    double value = (double) pair2.getValue();
                    JSONObject auxWord = new JSONObject();
                    auxWord.put("word", word);
                    auxWord.put("tfIdf", value);
                    wordsArray.put(auxWord);
                    it2.remove();
                }
                it.remove();
                JSONObject auxReq = new JSONObject();
                auxReq.put("id", id);
                auxReq.put("words", wordsArray);
                reqsArray.put(auxReq);
            }
        }
        JSONArray wordsFreq = new JSONArray();
        if (withFrequency) {
            Iterator it = corpusFrequency.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                String word = (String) pair.getKey();
                int value = (int) pair.getValue();
                JSONObject auxWord = new JSONObject();
                auxWord.put("word", word);
                auxWord.put("corpusTf", value);
                wordsFreq.put(auxWord);
                it.remove();
            }
        }
        JSONObject result = new JSONObject();
        result.put("corpus", reqsArray);
        result.put("corpusFrequency", wordsFreq);
        return result;
    }
}
