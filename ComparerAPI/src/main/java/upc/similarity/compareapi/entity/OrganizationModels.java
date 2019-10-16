package upc.similarity.compareapi.entity;

import upc.similarity.compareapi.similarity_algorithm.SimilarityModel;

import java.util.List;
import java.util.Map;

public class OrganizationModels {

    private double threshold;
    private boolean compare;
    private boolean withClusters;

    private SimilarityModel similarityModel;

    private Map<Integer, List<String>> clusters;
    private int lastClusterId;
    private List<Dependency> dependencies;

    public OrganizationModels() {}

    public OrganizationModels(double threshold, boolean compare, boolean withClusters, SimilarityModel similarityModel) {
        this.threshold = threshold;
        this.compare = compare;
        this.withClusters = withClusters;
        this.similarityModel = similarityModel;
    }

    public OrganizationModels(double threshold, boolean compare, boolean withClusters, SimilarityModel similarityModel, int lastClusterId, Map<Integer, List<String>> clusters, List<Dependency> dependencies) {
        this.threshold = threshold;
        this.compare = compare;
        this.withClusters = withClusters;
        this.similarityModel = similarityModel;
        if (withClusters) {
            this.lastClusterId = lastClusterId;
            this.clusters = clusters;
            this.dependencies = dependencies;
        }
    }

    /*
    Get methods
     */

    public double getThreshold() {
        return threshold;
    }

    public boolean isCompare() {
        return compare;
    }

    public boolean hasClusters() {
        return withClusters;
    }

    public SimilarityModel getSimilarityModel() {
        return similarityModel;
    }

    public int getLastClusterId() {
        return lastClusterId;
    }

    public Map<Integer, List<String>> getClusters() {
        return clusters;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /*
    Set methods
     */

    public void setCompare(boolean compare) {
        this.compare = compare;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void setHasCluster(boolean withClusters) {
        this.withClusters = withClusters;
    }

    public void setSimilarityModel(SimilarityModel similarityModel) {
        this.similarityModel = similarityModel;
    }

    public void setLastClusterId(int lastClusterId) {
        this.lastClusterId = lastClusterId;
    }

    public void setClusters(Map<Integer, List<String>> clusters) {
        this.clusters = clusters;
    }
}
