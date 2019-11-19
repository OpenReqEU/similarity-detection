package upc.similarity.compareapi.entity;

import upc.similarity.compareapi.algorithms.clusters_algorithm.ClustersModel;
import upc.similarity.compareapi.algorithms.similarity_algorithm.SimilarityModel;

public class OrganizationModels {

    /**
     * A double that shows the threshold used, only necessary in with clusters methods
     */
    private double threshold;

    /**
     * A boolean that shows if the model has been constructed using title + text (true) or only title (false)
     */
    private boolean compare;

    /**
     * A boolean that shows if the model has or does not have clusters
     */
    private boolean withClusters;

    private SimilarityModel similarityModel;
    private ClustersModel clustersModel;

    public OrganizationModels() {}

    public OrganizationModels(double threshold, boolean compare, boolean withClusters, SimilarityModel similarityModel) {
        this.threshold = threshold;
        this.compare = compare;
        this.withClusters = withClusters;
        this.similarityModel = similarityModel;
    }

    public OrganizationModels(double threshold, boolean compare, boolean withClusters, SimilarityModel similarityModel, ClustersModel clustersModel) {
        this.threshold = threshold;
        this.compare = compare;
        this.withClusters = withClusters;
        this.similarityModel = similarityModel;
        this.clustersModel = clustersModel;
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

    public ClustersModel getClustersModel() {
        return clustersModel;
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

    public void setClustersModel(ClustersModel clustersModel) {
        this.clustersModel = clustersModel;
    }
}
