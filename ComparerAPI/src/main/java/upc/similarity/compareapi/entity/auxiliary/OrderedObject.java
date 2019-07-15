package upc.similarity.compareapi.entity.auxiliary;

import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Requirement;

public class OrderedObject {

    private Dependency dependency;
    private Requirement requirement;
    private long time;

    public OrderedObject(Dependency dependency, Requirement requirement, long time) {
        this.dependency = dependency;
        this.requirement = requirement;
        this.time = time;
    }

    public boolean isDependency() {
        return dependency != null;
    }

    public Dependency getDependency() {
        return dependency;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public long getTime() {
        return time;
    }
}
