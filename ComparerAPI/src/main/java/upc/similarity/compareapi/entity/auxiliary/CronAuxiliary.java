package upc.similarity.compareapi.entity.auxiliary;

import upc.similarity.compareapi.entity.Dependency;

import java.util.List;

public class CronAuxiliary {

    private List<Dependency> updatedDependencies;
    private List<String> removedDependencies; //id of the requirement deleted

    public CronAuxiliary(List<Dependency> updatedDependencies, List<String> removedDependencies) {
        this.updatedDependencies = updatedDependencies;
        this.removedDependencies = removedDependencies;
    }

    public List<Dependency> getUpdatedDependencies() {
        return updatedDependencies;
    }

    public List<String> getRemovedDependencies() {
        return removedDependencies;
    }
}
