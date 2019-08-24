package upc.similarity.compareapi.entity.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import upc.similarity.compareapi.entity.Dependency;

import java.io.Serializable;
import java.util.List;

public class Dependencies implements Serializable {

    @JsonProperty(value="dependencies")
    private List<Dependency> dependenciesArray;

    public Dependencies(List<Dependency> dependencies) {
        this.dependenciesArray = dependencies;
    }

    public List<Dependency> getDependencies() {
        return dependenciesArray;
    }
}
