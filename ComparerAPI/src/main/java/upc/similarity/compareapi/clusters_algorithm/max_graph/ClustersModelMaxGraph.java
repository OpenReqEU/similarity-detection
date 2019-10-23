package upc.similarity.compareapi.clusters_algorithm.max_graph;

import upc.similarity.compareapi.clusters_algorithm.ClustersModel;
import upc.similarity.compareapi.entity.Dependency;

import java.util.List;
import java.util.Map;

public class ClustersModelMaxGraph implements ClustersModel {

    private int lastClusterId;
    private Map<Integer, List<String>> clusters;
    private List<Dependency> dependencies;

    public ClustersModelMaxGraph(int lastClusterId, Map<Integer, List<String>> clusters) {
        this.lastClusterId = lastClusterId;
        this.clusters = clusters;
    }

    public ClustersModelMaxGraph(int lastClusterId, Map<Integer, List<String>> clusters, List<Dependency> dependencies) {
        this.lastClusterId = lastClusterId;
        this.clusters = clusters;
        this.dependencies = dependencies;
    }


    /*
    Get methods
     */

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

    public void setLastClusterId(int lastClusterId) {
        this.lastClusterId = lastClusterId;
    }

    public void setClusters(Map<Integer, List<String>> clusters) {
        this.clusters = clusters;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }
}
