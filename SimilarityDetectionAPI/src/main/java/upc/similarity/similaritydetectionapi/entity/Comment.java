package upc.similarity.similaritydetectionapi.entity;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Comment", description = "A requirement comment with text")
public class Comment implements Serializable {

    @JsonProperty(value="id")
    private String id;
    @JsonProperty(value="text")
    private String text;

    private Map<String, Object> optional = new HashMap<>();

    @JsonAnySetter
    public void addOptional(String name, Object value) {
        optional.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getOptional() {
        return optional;
    }

    public Comment() {

    }

    public String getText() {
        return text;
    }

    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        result.put("id",id);
        result.put("text",text);
        return result;
    }
}
