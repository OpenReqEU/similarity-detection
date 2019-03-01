package upc.similarity.semilarapi.entity.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
//Input class for all operations representing a requirement with only an id
public class RequirementId implements Serializable {

    @JsonProperty(value="id")
    private String id;

    public RequirementId() {}

    public String getId() {
        return id;
    }
}
