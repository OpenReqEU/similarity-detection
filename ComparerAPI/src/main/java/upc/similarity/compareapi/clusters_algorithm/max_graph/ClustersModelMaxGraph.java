package upc.similarity.compareapi.clusters_algorithm.max_graph;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.compareapi.clusters_algorithm.ClustersModel;
import upc.similarity.compareapi.entity.Dependency;

import java.util.*;

public class ClustersModelMaxGraph implements ClustersModel {

    private int lastClusterId;
    private Map<Integer, List<String>> clusters;
    private List<Dependency> dependencies;

    private Map<String, Integer> reqCluster;
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

    private HashMap<String, Integer> computeReqClusterMap(Map<Integer,List<String>> clusters, Set<String> requirements) {
        HashMap<String,Integer> reqCluster = new HashMap<>();
        for (String requirement: requirements) {
            reqCluster.put(requirement, -1);
        }
        for (Map.Entry<Integer, List<String>> entry : clusters.entrySet()) {
            int id = entry.getKey();
            List<String> clusterRequirements = entry.getValue();
            for (String req : clusterRequirements) {
                reqCluster.put(req, id);
            }
        }
        return reqCluster;
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
