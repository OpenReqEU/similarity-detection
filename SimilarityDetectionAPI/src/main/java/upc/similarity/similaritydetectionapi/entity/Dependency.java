package upc.similarity.similaritydetectionapi.entity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import org.json.JSONObject;
import upc.similarity.similaritydetectionapi.config.Control;
import upc.similarity.similaritydetectionapi.exception.InternalErrorException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Class used to represent dependencies between requirements
public class Dependency implements Serializable {

    private static ObjectMapper mapper = new ObjectMapper();

    @ApiModelProperty(example = "0.48")
    @JsonProperty(value="dependency_score")
    private double dependencyScore;
    @ApiModelProperty(example = "UPC-2")
    @JsonProperty(value="fromid")
    private String fromid;
    @ApiModelProperty(example = "UPC-1")
    @JsonProperty(value="toid")
    private String toid;
    @ApiModelProperty(example = "accepted")
    @JsonProperty(value="status")
    private String status;
    @ApiModelProperty(example = "similar")
    @JsonProperty(value="dependency_type")
    private String dependencyType;
    @ApiModelProperty(example = "[Similarity-UPC]")
    @JsonProperty(value="description")
    private List<String> description;
    @ApiModelProperty(example = "1354019441000")
    @JsonProperty(value="created_at")
    private long createdAt;
    @ApiModelProperty(example = "1354019441000")
    @JsonProperty(value = "modified_at")
    private long modifiedAt;

    public Dependency() {
        description = new ArrayList<>();
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

    /*
    Get
     */

    public double getDependencyScore() {
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

    public long getModifiedAt() {
        return modifiedAt;
    }

    public long getCreatedAt() {
        return createdAt;
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

    public void setModifiedAt(long modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /*
    Auxiliary operations
     */

    public JSONObject toJSON() throws InternalErrorException {
        String jsonInString = "";
        try {
            jsonInString = mapper.writeValueAsString(this);
        } catch (Exception e) {
            Control.getInstance().showErrorMessage(e.getMessage());
            throw new InternalErrorException("Error while converting dependency to jsonObject");
        }
        return new JSONObject(jsonInString);
    }

    @Override
    public String toString() {
        return "Dependency between requirement " + fromid + "and requirement " + toid + " with type " + dependencyType + ", status " + status + " and score " + dependencyScore + ".";
    }
}
