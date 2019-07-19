package upc.similarity.compareapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Requirement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Clusters implements Serializable {

    @JsonProperty(value="requirements")
    private List<Requirement> requirements;
    @JsonProperty(value="dependencies")
    private List<Dependency> dependencies;


    public Clusters() {
        this.requirements = new ArrayList<>();
        this.dependencies = new ArrayList<>();
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public boolean inputOk() {
        return !requirements.isEmpty();
    }
}
