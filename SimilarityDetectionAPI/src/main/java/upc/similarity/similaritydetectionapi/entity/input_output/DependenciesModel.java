package upc.similarity.similaritydetectionapi.entity.input_output;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import upc.similarity.similaritydetectionapi.entity.Dependency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApiModel(value = "Dependencies", description = "OpenReqJson with dependencies")
public class DependenciesModel implements Input, Serializable {

    @JsonProperty(value="dependencies")
    private List<Dependency> dependencies;

    public DependenciesModel() {
        this.dependencies = new ArrayList<>();
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    @Override
    public boolean inputOk() {
        return !dependencies.isEmpty();
    }

    @Override
    public String checkMessage() {
        return "The input dependencies array is empty";
    }
}
