package upc.similarity.similaritydetectionapi.entity.input_output;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import upc.similarity.similaritydetectionapi.entity.Requirement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApiModel(value = "Requirements", description = "OpenReqJson with requirements")
public class Requirements implements Input, Serializable{

    @JsonProperty(value="requirements")
    private List<Requirement> requirementsArray;

    public Requirements() {
        requirementsArray = new ArrayList<>();
    }

    public Requirements(List<Requirement> requirements) {
        this.requirementsArray = requirements;
    }

    public List<Requirement> getRequirements() {
        return requirementsArray;
    }

    @Override
    public boolean inputOk() {
        return !requirementsArray.isEmpty();
    }

    @Override
    public String checkMessage() {
        return "The input requirements array is empty";
    }
}
