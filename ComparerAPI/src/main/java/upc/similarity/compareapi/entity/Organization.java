package upc.similarity.compareapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class Organization implements Serializable {

    private String name;
    private double threshold;
    private boolean compare;
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

}
