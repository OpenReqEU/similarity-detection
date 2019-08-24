package upc.similarity.similaritydetectionapi.entity.input_output;

import com.fasterxml.jackson.annotation.JsonProperty;
import upc.similarity.similaritydetectionapi.entity.Dependency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Dependencies implements Input, Serializable {

    @JsonProperty(value="dependencies")
    private List<Dependency> dependenciesArray;

    public Dependencies() {
        this.dependenciesArray = new ArrayList<>();
    }

    public List<Dependency> getDependencies() {
        return dependenciesArray;
    }

    @Override
    public boolean inputOk() {
        return !dependenciesArray.isEmpty();
    }

    @Override
    public String checkMessage() {
        return "The input dependencies array is empty";
    }
}
