package upc.similarity.similaritydetectionapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.json.JSONObject;

import java.io.Serializable;

@ApiModel(value = "Requirement", description = "A requirement with id and text")
public class Requirement implements Serializable {

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
    @JsonProperty(value = "modifiedAt")
    private long modifiedAt;

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

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id",id);
        json.put("name",name);
        json.put("text",text);
        json.put("status",status);
        json.put("created_at",createdAt);
        json.put("modified_at", modifiedAt);
        return json;
    }
}