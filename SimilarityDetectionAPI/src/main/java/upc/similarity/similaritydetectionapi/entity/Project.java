package upc.similarity.similaritydetectionapi.entity;

import com.fasterxml.jackson.annotation.*;
import io.swagger.annotations.ApiModel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Project", description = "A project with id, name and requirements")
public class Project implements Serializable {

    @JsonProperty(value="id")
    private String id;
    @JsonProperty(value="specifiedRequirements")
    private List<String> specifiedRequirements;

    private Map<String, Object> optional = new HashMap<>();

    @JsonAnySetter
    public void addOptional(String name, Object value) {
        optional.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getOptional() {
        return optional;
    }

    public String getId() {
        return id;
    }

    public List<String> getSpecifiedRequirements() {
        return specifiedRequirements;
    }
}