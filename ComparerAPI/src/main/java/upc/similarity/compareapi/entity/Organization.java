package upc.similarity.compareapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class Organization implements Serializable {

    private String name;
    private double threshold;
    @JsonProperty("uses_text")
    private boolean compare;
    @JsonProperty("has_clusters")
    private boolean clusters;
    @JsonProperty("current_executions")
    private List<Execution> currentExecutions;
    @JsonProperty("pending_responses")
    private List<Execution> pendingResponses;

    public Organization(String name, double threshold, boolean compare, boolean clusters, List<Execution> currentExecutions, List<Execution> pendingResponses) {
        this.name = name;
        this.threshold = threshold;
        this.compare = compare;
        this.clusters = clusters;
        this.currentExecutions = currentExecutions;
        this.pendingResponses = pendingResponses;
    }

    public String getName() {
        return name;
    }

    public double getThreshold() {
        return threshold;
    }

    public boolean isCompare() {
        return compare;
    }

    public boolean isClusters() {
        return clusters;
    }

    public List<Execution> getPendingResponses() {
        return pendingResponses;
    }

    public List<Execution> getCurrentExecutions() {
        return currentExecutions;
    }
}
