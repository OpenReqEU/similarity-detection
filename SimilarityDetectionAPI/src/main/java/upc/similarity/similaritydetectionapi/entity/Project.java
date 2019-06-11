package upc.similarity.similaritydetectionapi.entity;

import com.fasterxml.jackson.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Project", description = "A project with id, name and requirements")
public class Project implements Serializable {

    @ApiModelProperty(example = "UPC-P1")
    @JsonProperty(value="id")
    private String id;
    @ApiModelProperty(example = "[UPC-98,UPC-97]")
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