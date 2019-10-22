package upc.similarity.compareapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.simple.JSONObject;
import upc.similarity.compareapi.exception.BadRequestException;

import java.io.Serializable;

import static java.lang.Long.max;

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
    @JsonProperty(value = "modifiedAt")
    private long modifiedAt;
    @JsonProperty(value="status")
    private String status;

    public Requirement() {}

    public Requirement(JSONObject jsonObject) throws BadRequestException {
        try {
            this.id = (String) jsonObject.get("id");
            this.name = (String) jsonObject.get("name");
            this.text = (String) jsonObject.get("text");
            Object aux = jsonObject.get("created_at");
            if (aux instanceof Long) this.createdAt = (Long) aux;
            else {
                String createdAtAux = (String) aux;
                this.createdAt = (aux == null) ? 0 : Long.parseLong(createdAtAux);
            }
            aux = jsonObject.get("modified_at");
            if (aux instanceof Long) this.modifiedAt = (Long) aux;
            else {
                String modifiedAtAux = (String) aux;
                this.modifiedAt = (modifiedAtAux == null) ? 0 : Long.parseLong(modifiedAtAux);
            }
            this.status = (String) jsonObject.get("status");
        } catch (Exception e) {
            throw new BadRequestException("A requirement is not well written: " + e.getMessage());
        }
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

    /*
    Auxiliary operations
     */

    @Override
    public String toString() {
        return "Requirement with id " + id + ".";
    }

}
