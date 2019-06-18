package upc.similarity.compareapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;

import java.io.Serializable;

//Class used to represent requirements
public class Requirement implements Serializable {

    @JsonProperty(value="id")
    private String id;
    @JsonProperty(value="name")
    private String name;
    @JsonProperty(value="text")
    private String text;
    @JsonProperty(value="created_at")
    private long created_at;

    public Requirement() {}

    /*
    Get
     */

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    public long getCreated_at() {
        return created_at;
    }

    /*
    Set
     */

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setCreated_at(long created_at) {
        this.created_at = created_at;
    }

    /*
    Auxiliary operations
     */

    @Override
    public String toString() {
        return "Requirement with id " + id + ".";
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id",id);
        json.put("name",name);
        json.put("text",text);
        json.put("created_at",created_at);
        return json;
    }

    public Requirement(JSONObject jsonObject) {
        this.setId(jsonObject.getString("id"));
        this.setName(jsonObject.getString("name"));
        this.setText(jsonObject.getString("text"));
        this.setCreated_at(jsonObject.getLong("created_at"));
    }
}
