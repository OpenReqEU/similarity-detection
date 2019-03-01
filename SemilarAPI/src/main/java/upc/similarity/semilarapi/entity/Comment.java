package upc.similarity.semilarapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

//Class use to represent comments
public class Comment implements Serializable {

    @JsonProperty(value="id")
    private String id;
    @JsonProperty(value="text")
    private String text;

    public Comment() {

    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}
