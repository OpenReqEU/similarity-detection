package upc.similarity.compareapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import upc.similarity.compareapi.entity.auxiliary.RequirementDeserializer;

import java.io.Serializable;

import static java.lang.Long.max;

@JsonDeserialize(using = RequirementDeserializer.class)
public class Requirement implements Serializable {

    @JsonProperty(value="id")
    private String id;
    @JsonProperty(value="name")
    private String name;
    @JsonProperty(value="text")
    private String text;
    @JsonProperty(value="created_at")
    private long createdAt;
    @JsonProperty(value = "modified_at")
    private long modifiedAt;
    @JsonProperty(value="status")
    private String status;
    private String component;

    public Requirement() {}

    public Requirement(String id, String name, String text, long createdAt, long modifiedAt, String status, String component) {
        this.id = id;
        this.name = name;
        this.text = text;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.status = status;
        this.component = component;
    }


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

    public long getModifiedAt() {
        return modifiedAt;
    }

    public String getStatus() {
        return status;
    }

    public long getTime() {
        return max(createdAt,modifiedAt);
    }

    public String getComponent() {
        return component;
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

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setModifiedAt(long modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    /*
    Auxiliary operations
     */

    @Override
    public String toString() {
        return "Requirement with id " + id + ".";
    }

}
