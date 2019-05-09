package upc.similarity.semilarapi.entity;

import java.util.Map;

public class Model {

    private Map<String, Map<String, Double>> docs;
    private Map<String, Integer> corpusFrequency;

    public Model(Map<String, Map<String, Double>> docs, Map<String, Integer> corpusFrequency) {
        this.docs = docs;
        this.corpusFrequency = corpusFrequency;
    }

    public Map<String, Map<String, Double>> getDocs() {
        return docs;
    }

    public Map<String, Integer> getCorpusFrequency() {
        return corpusFrequency;
    }

}
