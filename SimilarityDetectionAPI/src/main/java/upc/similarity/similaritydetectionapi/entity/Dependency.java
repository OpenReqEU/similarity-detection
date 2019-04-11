package upc.similarity.similaritydetectionapi.entity;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.similaritydetectionapi.exception.BadRequestException;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Dependency", description = "A dependency with fromid, toid, dependency_type, status, description (component) and dependency_score")
public class Dependency implements Serializable {

    @JsonProperty(value="fromid")
    private String fromid;
    @JsonProperty(value="toid")
    private String toid;
    @JsonProperty(value="dependency_type")
    private String dependency_type;
    @JsonProperty(value="status")
    private String status;
    @JsonProperty(value="dependency_score")
    private double dependency_score;
    @JsonProperty(value="description")
    private List<String> description;
    private Map<String, Object> optional = new HashMap<>();

    @JsonAnySetter
    public void addOptional(String name, Object value) {
        optional.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getOptional() {
        return optional;
    }


    public Dependency() {
        this.description = new ArrayList<>();
    }

    public Dependency(JSONObject json) {
        this.dependency_score = json.getDouble("dependency_score");
        this.fromid = json.getString("fromid");
        this.toid = json.getString("toid");
        this.dependency_type = json.getString("dependency_type");
        this.status = json.getString("status");
        JSONArray array = json.getJSONArray("description");
        this.description = new ArrayList<>();
        this.description.add(array.getString(0));
    }

    public Dependency(double dependency_score, String fromid, String toid, String dependency_type, String status, String component) {
        this.dependency_score = dependency_score;
        this.fromid = fromid;
        this.toid = toid;
        this.dependency_type = dependency_type;
        this.status = status;
        this.description = new ArrayList<>();
        this.description.add(component);
    }

    public Dependency(double dependency_score, String fromid, String toid, String dependency_type, String status, List<String> description) {
        this.dependency_score = dependency_score;
        this.fromid = fromid;
        this.toid = toid;
        this.dependency_type = dependency_type;
        this.status = status;
        this.description = description;
        if(this.description == null) this.description = new ArrayList<>();
    }

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

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("fromid",fromid);
        json.put("toid",toid);
        json.put("dependency_type",dependency_type);
        /*JSONArray json_description = new JSONArray();
        for (String aux: description) {
            json_description.put(aux);
        }
        json.put("description",json_description);*/
        return json;
    }

    public double getDependency_score() {
        return dependency_score;
    }

    public List<String> getDescription() {
        return description;
    }

    public String getDependency_type() {
        return dependency_type;
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
}