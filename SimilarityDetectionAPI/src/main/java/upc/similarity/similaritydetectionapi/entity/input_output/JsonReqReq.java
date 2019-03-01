package upc.similarity.similaritydetectionapi.entity.input_output;

import com.fasterxml.jackson.annotation.*;
import io.swagger.annotations.ApiModel;
import upc.similarity.similaritydetectionapi.entity.Dependency;
import upc.similarity.similaritydetectionapi.entity.Requirement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "JsonReqReq", description = "OpenReqJson with requirements and dependencies")
public class JsonReqReq implements Serializable {

    @JsonProperty(value="requirements")
    private List<Requirement> requirements;
    @JsonProperty(value="dependencies")
    private List<Dependency> dependencies;
    private Map<String, Object> optional = new HashMap<>();

    @JsonAnySetter
    public void addOptional(String name, Object value) {
        optional.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getOptional() {
        return optional;
    }


    public JsonReqReq() {
        requirements = new ArrayList<>();
        dependencies = new ArrayList<>();
    }

    public List<Requirement> getRequirements() {
        if (requirements == null) requirements = new ArrayList<>();
        return requirements;
    }

    public List<Dependency> getDependencies() {
        if (dependencies == null) dependencies = new ArrayList<>();
        return dependencies;
    }

    public void setRequirements(List<Requirement> requirements) {
        this.requirements = requirements;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public void add_dependency(Dependency dependency) {
        if (dependencies == null) dependencies = new ArrayList<>();
        dependencies.add(dependency);
    }

    public boolean OK() {
        if (requirements.size() == 0) return false;
        else return true;
    }
}