package upc.similarity.compareapi.entity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import upc.similarity.compareapi.util.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.Long.max;

public class Dependency implements Serializable {

    private static ObjectMapper mapper = new ObjectMapper();
    private String defaultComponent = "Similarity-UPC";
    private String defaultDependencyType = "similar";

    @JsonProperty(value="dependency_score")
    private double dependencyScore;
    @JsonProperty(value="fromid")
    private String fromid;
    @JsonProperty(value="toid")
    private String toid;
    @JsonProperty(value="status")
    private String status;
    @JsonProperty(value="dependency_type")
    private String dependencyType;
    @JsonProperty(value="description")
    private List<String> description;
    @JsonProperty(value="created_at", access = JsonProperty.Access.WRITE_ONLY)
    private long createdAt;
    @JsonProperty(value = "modified_at", access = JsonProperty.Access.WRITE_ONLY)
    private long modifiedAt;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private int clusterId;

    public Dependency() {
        this.description = new ArrayList<>();
    }

    public Dependency(String fromid, String toid) {
        this.fromid = fromid;
        this.toid = toid;
        this.dependencyType = defaultDependencyType;
        this.description = new ArrayList<>();
        description.add(defaultComponent);
    }

    public Dependency(double dependencyScore, String fromid, String toid) {
        this.dependencyScore = dependencyScore;
        this.fromid = fromid;
        this.toid = toid;
        this.status = "proposed";
        this.dependencyType = defaultDependencyType;
        this.description = new ArrayList<>();
        description.add(defaultComponent);
    }

    public Dependency(double dependencyScore, String fromid, String toid, String status) {
        this.dependencyScore = dependencyScore;
        this.fromid = fromid;
        this.toid = toid;
        this.status = status;
        this.dependencyType = defaultDependencyType;
        this.description = new ArrayList<>();
        description.add(defaultComponent);
    }

    public Dependency(String fromid, String toid, String status, double dependencyScore, int clusterId) {
        this.fromid = fromid;
        this.toid = toid;
        this.status = status;
        this.dependencyScore = dependencyScore;
        this.clusterId = clusterId;
        this.dependencyType = defaultDependencyType;
        this.description = new ArrayList<>();
        description.add(defaultComponent);
    }

    /*
    Get
     */

    public Double getDependencyScore() {
        return dependencyScore;
    }

    public String getFromid() {
        return fromid;
    }

    public String getToid() {
        return toid;
    }

    public String getStatus() {
        return status;
    }

    public String getDependencyType() {
        return dependencyType;
    }

    public List<String> getDescription() {
        return description;
    }

    public int getClusterId() {
        return clusterId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getModifiedAt() {
        return modifiedAt;
    }

    public long computeTime() {
        return max(createdAt,modifiedAt);
    }

    /*
    Set
     */

    public void setDependencyScore(double dependencyScore) {
        this.dependencyScore = dependencyScore;
    }

    public void setFromid(String fromid) {
        this.fromid = fromid;
    }

    public void setToid(String toid) {
        this.toid = toid;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDependencyType(String dependencyType) {
        this.dependencyType = dependencyType;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setModifiedAt(long modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    /*
    Auxiliary operations
     */

    public org.json.JSONObject toJSON() {
        String jsonInString = "";
        try {
            jsonInString = mapper.writeValueAsString(this);
        } catch (Exception e) {
            Logger.getInstance().showErrorMessage(e.getMessage());
            throw new InternalError("Error while converting dependency to jsonObject");
        }
        return new org.json.JSONObject(jsonInString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromid, toid, status);
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public String toString() {
        return "Dependency between requirement " + fromid + "and requirement " + toid + " with type " + dependencyType + ", status " + status + " and score " + dependencyScore + ".";
    }
}
