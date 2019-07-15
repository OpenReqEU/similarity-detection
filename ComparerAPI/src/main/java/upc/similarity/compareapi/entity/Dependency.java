package upc.similarity.compareapi.entity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import upc.similarity.compareapi.config.Control;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Class used to represent dependencies between requirements
public class Dependency implements Serializable {

    private static ObjectMapper mapper = new ObjectMapper();

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
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private int clusterId;

    public Dependency() {
        this.description = new ArrayList<>();
    }

    public Dependency(String fromid, String toid) {
        this.fromid = fromid;
        this.toid = toid;
    }

    public Dependency(double dependencyScore, String fromid, String toid, String status, String dependencyType, String component) {
        this.dependencyScore = dependencyScore;
        this.fromid = fromid;
        this.toid = toid;
        this.status = status;
        this.dependencyType = dependencyType;
        this.description = new ArrayList<>();
        description.add(component);
    }

    public Dependency(String fromid, String toid, String dependencyType) {
        this.fromid = fromid;
        this.toid = toid;
        this.description = new ArrayList<>();
        this.dependencyType = dependencyType;
    }

    public Dependency(String fromid, String toid, String status, double dependencyScore, int clusterId) {
        this.fromid = fromid;
        this.toid = toid;
        this.status = status;
        this.dependencyScore = dependencyScore;
        this.clusterId = clusterId;
        this.description = new ArrayList<>();
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

    /*
    Auxiliary operations
     */

    public JSONObject toJSON() {
        String jsonInString = "";
        try {
            jsonInString = mapper.writeValueAsString(this);
        } catch (Exception e) {
            Control.getInstance().showErrorMessage(e.getMessage());
            throw new InternalError("Error while converting dependency to jsonObject");
        }
        return new JSONObject(jsonInString);
    }

    @Override
    public String toString() {
        return "Dependency between requirement " + fromid + "and requirement " + toid + " with type " + dependencyType + ", status " + status + " and score " + dependencyScore + ".";
    }
}
