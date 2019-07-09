package upc.similarity.compareapi.entity.auxiliary;

import upc.similarity.compareapi.entity.Dependency;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CronAuxiliary {

    private List<Dependency> updatedDependencies;
    private List<String> removedDependencies; //id of the requirement deleted
    private Map<String,Integer> reqCluster;
    private Map<Integer,Map<String,Dependency>> dependencies = new HashMap<>();

    public CronAuxiliary(List<Dependency> updatedDependencies, List<String> removedDependencies) {
        this.updatedDependencies = updatedDependencies;
        this.removedDependencies = removedDependencies;
    }

    public List<Dependency> getUpdatedDependencies() {
        return updatedDependencies;
    }

    public List<String> getRemovedDependencies() {
        return removedDependencies;
    }

    public Map<String, Integer> getReqCluster() {
        return reqCluster;
    }

    public Map<Integer, Map<String, Dependency>> getDependencies() {
        return dependencies;
    }

    public void setReqCluster(Map<String, Integer> reqCluster) {
        this.reqCluster = reqCluster;
    }
}
