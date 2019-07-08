package upc.similarity.compareapi.entity.output;

import upc.similarity.compareapi.entity.Dependency;

import java.io.Serializable;
import java.util.List;

public class Dependencies implements Serializable {

    private List<Dependency> dependencies;

    public Dependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }
}
