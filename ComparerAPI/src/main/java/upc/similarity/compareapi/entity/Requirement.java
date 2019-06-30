package upc.similarity.compareapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

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
    private long createdAt;
    @JsonProperty(value="status")
    private String status;

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

    public long getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
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

    public void setCreatedAt(long created_at) {
        this.createdAt = created_at;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /*
    Auxiliary operations
     */

    @Override
    public String toString() {
        return "Requirement with id " + id + ".";
    }

}
