package upc.similarity.compareapi.entity;

import java.util.List;
import java.util.Map;

public class Model {

    private Map<String, Map<String, Double>> docs;
    private Map<String, Integer> corpusFrequency;
    private boolean cluster;
    private Map<Integer, List<String>> clusters;
    private List<Dependency> dependencies;

    public Model(){}

    public Model(Map<String, Map<String, Double>> docs, Map<String, Integer> corpusFrequency) {
        this.docs = docs;
        this.corpusFrequency = corpusFrequency;
        this.cluster = false;
    }

    public Model(Map<String, Map<String, Double>> docs, Map<String, Integer> corpusFrequency, Map<Integer, List<String>> clusters, List<Dependency> dependencies) {
        this.docs = docs;
        this.corpusFrequency = corpusFrequency;
        this.cluster = true;
        this.clusters = clusters;
        this.dependencies = dependencies;
    }

    /*
    Get
     */

    public Map<String, Map<String, Double>> getDocs() {
        return docs;
    }

    public Map<String, Integer> getCorpusFrequency() {
        return corpusFrequency;
    }

    public boolean hasClusters() {
        return cluster;
    }

    public Map<Integer, List<String>> getClusters() {
        return clusters;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /*
    Set
     */

    public void setDocs(Map<String, Map<String, Double>> docs) {
        this.docs = docs;
    }

    public void setCorpusFrequency(Map<String, Integer> corpusFrequency) {
        this.corpusFrequency = corpusFrequency;
    }

    public void setClusters(Map<Integer, List<String>> clusters) {
        this.cluster = true;
        this.clusters = clusters;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }
}
