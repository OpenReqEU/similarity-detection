package upc.similarity.similaritydetectionapi.entity;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Requirement", description = "A requirement with id and text")
public class Requirement implements Serializable {

    @JsonProperty(value="id")
    private String id;
    @JsonProperty(value="name")
    private String name;
    @JsonProperty(value="text")
    private String text;
    @JsonProperty(value="created_at")
    private Long created_at;

    /*@JsonProperty(value="comments")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Comment> comments;

    private Map<String, Object> optional = new HashMap<>();

    @JsonAnySetter
    public void addOptional(String name, Object value) {
        optional.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getOptional() {
        return optional;
    }*/

    public Requirement() {
        //comments = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id",id);
        json.put("name",name);
        json.put("text",text);
        json.put("created_at",created_at);
        /*JSONArray json_comments = new JSONArray();
        for (Comment comment: comments) {
            json_comments.put(comment.toJSON());
        }
        json.put("comments",json_comments);*/
        return json;
    }
}