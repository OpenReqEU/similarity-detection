package upc.similarity.compareapi.entity;

import java.util.List;
import java.util.Map;

public class Model {

    private Map<String, Map<String, Double>> docs;
    private Map<String, Integer> corpusFrequency;
    private boolean cluster;
    private Map<String, List<String>> clusters;
    private Map<String, ReqClusterInfo> reqCluster;

    public Model(){}

    public Model(Map<String, Map<String, Double>> docs, Map<String, Integer> corpusFrequency) {
        this.docs = docs;
        this.corpusFrequency = corpusFrequency;
        this.cluster = false;
    }

    public Model(Map<String, Map<String, Double>> docs, Map<String, Integer> corpusFrequency, Map<String, List<String>> clusters, Map<String, ReqClusterInfo> reqCluster) {
        this.docs = docs;
        this.corpusFrequency = corpusFrequency;
        this.cluster = true;
        this.clusters = clusters;
        this.reqCluster = reqCluster;
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

    public Map<String, List<String>> getClusters() {
        return clusters;
    }

    public Map<String, ReqClusterInfo> getReqCluster() {
        return reqCluster;
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

    public void setClusters(Map<String, List<String>> clusters) {
        this.cluster = true;
        this.clusters = clusters;
    }

    public void setReqCluster(Map<String, ReqClusterInfo> reqCluster) {
        this.cluster = true;
        this.reqCluster = reqCluster;
    }
}
