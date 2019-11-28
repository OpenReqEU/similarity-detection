package upc.similarity.compareapi.entity;

import upc.similarity.compareapi.algorithms.clusters_algorithm.ClustersModel;
import upc.similarity.compareapi.algorithms.similarity_algorithm.SimilarityModel;

import java.util.Map;

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

    /**
     * Boolean that tells whether to use the component attribute of the requirements during the comparison step
     */
    private boolean useComponent;

    /**
     * A map with the component of each requirement in the model. The component is taken into account during
     * the similarity comparison step
     */
    private Map<String, String> reqComponent;

    private SimilarityModel similarityModel;
    private ClustersModel clustersModel;

    public OrganizationModels() {}

    public OrganizationModels(SimilarityModel similarityModel, Map<String, String> reqComponent) {
        this.similarityModel = similarityModel;
        this.reqComponent = reqComponent;
    }

    public OrganizationModels(OrganizationModels organizationModels, double threshold, boolean compare, boolean useComponent, boolean withClusters) {
        this.threshold = threshold;
        this.compare = compare;
        this.useComponent = useComponent;
        this.withClusters = withClusters;
        this.reqComponent = organizationModels.getReqComponent();
        this.similarityModel = organizationModels.getSimilarityModel();
    }

    public OrganizationModels(OrganizationModels organizationModels, double threshold, boolean compare, boolean useComponent, boolean withClusters, ClustersModel clustersModel) {
        this.threshold = threshold;
        this.compare = compare;
        this.useComponent = useComponent;
        this.withClusters = withClusters;
        this.reqComponent = organizationModels.getReqComponent();
        this.similarityModel = organizationModels.getSimilarityModel();
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

    public boolean isUseComponent() {
        return useComponent;
    }

    public boolean hasClusters() {
        return withClusters;
    }

    public Map<String, String> getReqComponent() {
        return reqComponent;
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

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void setCompare(boolean compare) {
        this.compare = compare;
    }

    public void setUseComponent(boolean useComponent) {
        this.useComponent = useComponent;
    }

    public void setHasCluster(boolean withClusters) {
        this.withClusters = withClusters;
    }

    public void setReqComponent(Map<String, String> reqComponent) {
        this.reqComponent = reqComponent;
    }

    public void setSimilarityModel(SimilarityModel similarityModel) {
        this.similarityModel = similarityModel;
    }

    public void setClustersModel(ClustersModel clustersModel) {
        this.clustersModel = clustersModel;
    }
}
