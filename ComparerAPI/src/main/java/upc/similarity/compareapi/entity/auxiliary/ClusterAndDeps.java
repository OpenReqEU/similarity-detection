package upc.similarity.compareapi.entity.auxiliary;

import upc.similarity.compareapi.entity.Dependency;

import java.util.List;
import java.util.Map;

public class ClusterAndDeps {

    private Map<Integer, List<String>> clusters;
    private Map<String, Integer> reqCluster;
    private int lastClusterId;
    private List<Dependency> dependencies;

    public ClusterAndDeps(int lastClusterId, Map<Integer,List<String>> clusters, Map<String, Integer> reqCluster, List<Dependency> dependencies) {
        this.lastClusterId = lastClusterId;
        this.clusters = clusters;
        this.reqCluster = reqCluster;
        this.dependencies = dependencies;
    }

    public int getLastClusterId() {
        return lastClusterId;
    }

    public Map<Integer, List<String>> getClusters() {
        return clusters;
    }

    public Map<String, Integer> getReqCluster() {
        return reqCluster;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }
}
