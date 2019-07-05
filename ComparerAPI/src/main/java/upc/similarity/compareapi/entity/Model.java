package upc.similarity.compareapi.entity;

import java.util.List;
import java.util.Map;

public class Model {

    private Map<String, Map<String, Double>> docs;
    private Map<String, Integer> corpusFrequency;
    private double threshold;
    private boolean compare;
    private boolean cluster;
    private Integer lastClusterId;
    private Map<Integer, List<String>> clusters;
    private List<Dependency> dependencies; //accepted and rejected dependencies (proposed dependencies are not included)

    public Model(){}

    public Model(Map<String, Map<String, Double>> docs, Map<String, Integer> corpusFrequency, double threshold, boolean compare) {
        this.docs = docs;
        this.corpusFrequency = corpusFrequency;
        this.threshold = threshold;
        this.compare = compare;
        this.cluster = false;
        this.lastClusterId = -1;
    }

    public Model(Map<String, Map<String, Double>> docs, Map<String, Integer> corpusFrequency, double threshold, boolean compare, Integer lastClusterId, Map<Integer, List<String>> clusters, List<Dependency> dependencies) {
        this.docs = docs;
        this.corpusFrequency = corpusFrequency;
        this.threshold = threshold;
        this.compare = compare;
        this.cluster = true;
        this.lastClusterId = lastClusterId;
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

    public double getThreshold() {
        return threshold;
    }

    public boolean isCompare() {
        return compare;
    }

    public boolean hasClusters() {
        return cluster;
    }

    public Integer getLastClusterId() {
        return lastClusterId;
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

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void setCompare(boolean compare) {
        this.compare = compare;
    }

    public void setHasClusters(boolean cluster) {
        this.cluster = cluster;
    }

    public void setLastClusterId(Integer lastClusterId) {
        this.lastClusterId = lastClusterId;
    }

    public void setClusters(Map<Integer, List<String>> clusters) {
        this.cluster = true;
        this.clusters = clusters;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.cluster = true;
        this.dependencies = dependencies;
    }
}
