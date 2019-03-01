package upc.similarity.semilarapi.entity.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import upc.similarity.semilarapi.entity.Dependency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Output class for all operations with a list of dependencies
public class Dependencies implements Serializable {

    @JsonProperty(value="dependencies")
    private List<Dependency> dependencies;

    public Dependencies() {
        dependencies = new ArrayList<>();
    }

    public Dependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public Dependencies(Dependency dependency) {
        this.dependencies = new ArrayList<>();
        this.dependencies.add(dependency);
    }
}
