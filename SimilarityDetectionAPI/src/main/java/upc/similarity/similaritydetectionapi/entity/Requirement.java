package upc.similarity.similaritydetectionapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.json.JSONObject;
import upc.similarity.similaritydetectionapi.config.Control;
import upc.similarity.similaritydetectionapi.exception.InternalErrorException;

import java.io.Serializable;
import java.util.List;

@ApiModel(value = "Requirement", description = "A requirement with id and text")
public class Requirement implements Serializable {

    private static ObjectMapper mapper = new ObjectMapper();

    @ApiModelProperty(example = "UPC-98")
    @JsonProperty(value="id")
    private String id;
    @ApiModelProperty(example = "Check swagger version.")
    @JsonProperty(value="name")
    private String name;
    @ApiModelProperty(example = "The swagger version is deprecated. Please update the service asap.")
    @JsonProperty(value="text")
    private String text;
    @ApiModelProperty(example = "empty")
    @JsonProperty(value="status")
    private String status;
    @ApiModelProperty(example = "1354019441000")
    @JsonProperty(value="created_at")
    private Long createdAt;
    @ApiModelProperty(example = "1354019441000")
    @JsonProperty(value = "modified_at")
    private Long modifiedAt;
    @ApiModelProperty(example = "[{\"id\":\"UPC-98-1\",\"name\":\"Components\",\"text\":\"Dependency-Detection\"}]")
    @JsonProperty(value = "requirementParts")
    private List<Object> requirementParts;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public String getStatus() {
        return status;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getModifiedAt() {
        return modifiedAt;
    }

    public List<Object> getRequirementParts() {
        return requirementParts;
    }

    public JSONObject toJSON() throws InternalErrorException {
        String jsonInString = "";
        try {
            jsonInString = mapper.writeValueAsString(this);
        } catch (Exception e) {
            Control.getInstance().showErrorMessage(e.getMessage());
            throw new InternalErrorException("Error while converting requirement to jsonObject");
        }
        return new JSONObject(jsonInString);
    }
}