package upc.similarity.semilarapi.entity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Class used to represent dependencies between requirements
public class Dependency implements Serializable {

    @JsonProperty(value="dependency_score")
    private double dependency_score;
    @JsonProperty(value="fromid")
    private String fromid;
    @JsonProperty(value="toid")
    private String toid;
    @JsonProperty(value="status")
    private String status;
    @JsonProperty(value="dependency_type")
    private String dependency_type;
    @JsonProperty(value="description")
    private List<String> description;

    public Dependency() {
        description = new ArrayList<>();
    }

    public Dependency(double dependency_score, String fromid, String toid, String status, String dependency_type, String component) {
        this.dependency_score = dependency_score;
        this.fromid = fromid;
        this.toid = toid;
        this.status = status;
        this.dependency_type = dependency_type;
        this.description = new ArrayList<>();
        description.add(component);
    }

    /*
    Get
     */

    public double getDependency_score() {
        return dependency_score;
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

    public String getDependency_type() {
        return dependency_type;
    }

    public List<String> getDescription() {
        return description;
    }

    /*
    Set
     */

    public void setDependency_score(double dependency_score) {
        this.dependency_score = dependency_score;
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

    public void setDependency_type(String dependency_type) {
        this.dependency_type = dependency_type;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    /*
    Auxiliary operations
     */

    public String print_json() {

        ObjectMapper mapper = new ObjectMapper();

        String jsonInString = "";
        try {
            jsonInString = mapper.writeValueAsString(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonInString;
    }

    @Override
    public String toString() {
        return "Dependency between requirement " + fromid + "and requirement " + toid + " with type " + dependency_type + ", status " + status + " and score " + dependency_score + ".";
    }
}
