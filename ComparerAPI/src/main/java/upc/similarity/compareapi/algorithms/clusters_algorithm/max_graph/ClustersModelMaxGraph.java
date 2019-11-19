package upc.similarity.compareapi.algorithms.clusters_algorithm.max_graph;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.compareapi.algorithms.clusters_algorithm.ClustersModel;
import upc.similarity.compareapi.entity.Dependency;

import java.util.*;

public class ClustersModelMaxGraph implements ClustersModel {

    /**
     * An integer that shows the las cluster identifier used. Necessary to
     * maintain the cluster id as unique
     */
    private int lastClusterId;

    /**
     * A map that represents the structure of each cluster in the model.
     * It is implemented as a Map that saves the cluster identifier and
     * an array of String values that represent the ids of the requirements
     * included in the cluster
     */
    private Map<Integer, List<String>> clusters;

    /**
     * A list with all the dependencies of the model (accepted, rejected and proposed).
     * Each dependency contains its status and the requirements that form the link. If
     * the dependency is accepted, it also contains the identifier of the cluster where
     * it belongs. This attribute is only use when building the model, the service never
     * loads all the dependencies of a previous created model (memory issue)
     */
    private List<Dependency> dependencies;

    /**
     * A temporary map that shows the cluster id of each requirement of the model.
     * This makes more efficient the different algorithms during the update process
     */
    private Map<String, Integer> reqCluster;

    /**
     * A temporary set to save the ids of the clusters changed during the update process.
     * Thanks to this we only recompute the dependencies of the clusters changed.
     */
    private Set<Integer> clustersChanged;

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

    public Map<String, Integer> getReqCluster() {
        return reqCluster;
    }

    public Set<Integer> getClustersChanged() {
        return clustersChanged;
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


    /*
    Auxiliary methods
     */

    public void startUpdateProcess(Set<String> requirements) {
        reqCluster = computeReqClusterMap(clusters,requirements);
        clustersChanged = new HashSet<>();
    }

    public void finishUpdateProcess() {
        reqCluster = null;
        clustersChanged = null;
    }


    /*
    Private methods
     */

    private Map<String, Integer> computeReqClusterMap(Map<Integer,List<String>> clusters, Set<String> requirements) {
        HashMap<String,Integer> result = new HashMap<>();
        for (String requirement: requirements) {
            result.put(requirement, -1);
        }
        for (Map.Entry<Integer, List<String>> entry : clusters.entrySet()) {
            int id = entry.getKey();
            List<String> clusterRequirements = entry.getValue();
            for (String req : clusterRequirements) {
                result.put(req, id);
            }
        }
        return result;
    }

    public JSONObject extractModel() {
        JSONObject result = new JSONObject();
        JSONArray clustersArray = new JSONArray();
        for (Map.Entry<Integer, List<String>> entry : clusters.entrySet()) {
            int clusterId = entry.getKey();
            List<String> clusterRequirements = entry.getValue();
            JSONArray requirementsArray = new JSONArray();
            for (String requirement : clusterRequirements) requirementsArray.put(requirement);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("clusterId", clusterId);
            jsonObject.put("clusterRequirements", requirementsArray);
            clustersArray.put(jsonObject);
        }
        result.put("clusters",clustersArray);
        result.put("lastClusterId", lastClusterId);
        return result;
    }
}
