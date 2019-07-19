package upc.similarity.similaritydetectionapi.entity.input_output;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import upc.similarity.similaritydetectionapi.entity.Requirement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApiModel(value = "Requirements", description = "OpenReqJson with requirements")
public class Requirements extends Input implements Serializable{

    @JsonProperty(value="requirements")
    private List<Requirement> requirements;

    public Requirements() {
        requirements = new ArrayList<>();
    }

    public Requirements(List<Requirement> requirements) {
        this.requirements = requirements;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    @Override
    public boolean inputOk() {
        return !requirements.isEmpty();
    }

    @Override
    public String checkMessage() {
        return "The input requirements array is empty";
    }
}
