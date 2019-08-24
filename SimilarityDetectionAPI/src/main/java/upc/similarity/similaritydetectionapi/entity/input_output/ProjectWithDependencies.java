package upc.similarity.similaritydetectionapi.entity.input_output;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import upc.similarity.similaritydetectionapi.entity.Dependency;
import upc.similarity.similaritydetectionapi.entity.Requirement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApiModel(value = "ProjectWithDependencies", description = "OpenReqJson with requirements and dependencies")
public class ProjectWithDependencies implements Input, Serializable {

    @JsonProperty(value="requirements")
    private List<Requirement> requirements;

    @JsonProperty(value="dependencies")
    private List<Dependency> dependencies;

    public ProjectWithDependencies() {
        this.requirements = new ArrayList<>();
        this.dependencies = new ArrayList<>();
    }

    public ProjectWithDependencies(List<Requirement> requirements, List<Dependency> dependencies) {
        this.requirements = requirements;
        this.dependencies = dependencies;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    @Override
    public boolean inputOk() {
        return (!this.requirements.isEmpty() || !this.dependencies.isEmpty());
    }

    @Override
    public String checkMessage() {
        return "The two input arrays are empty";
    }
}
