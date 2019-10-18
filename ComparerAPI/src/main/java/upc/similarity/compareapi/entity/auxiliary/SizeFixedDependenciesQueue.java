package upc.similarity.compareapi.entity.auxiliary;

import org.json.JSONArray;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.service.DatabaseOperations;

import java.util.Comparator;
import java.util.TreeSet;

public class SizeFixedDependenciesQueue extends ResponseDependencies {

    private int maxSize;
    private TreeSet<Dependency> queue;

    public SizeFixedDependenciesQueue(String organization, String responseId, int maxSize, Comparator<Dependency> comparator) {
        super(organization,responseId);
        this.maxSize = maxSize;
        this.queue = new TreeSet<>(comparator);
    }

    public void addDependency(Dependency elem) {
        if (queue.size() < maxSize) {
            queue.add(elem);
        } else {
            Dependency last = queue.last();
            if (last.getDependencyScore() < elem.getDependencyScore()) {
                queue.pollLast();
                queue.add(elem);
            }
        }
    }

    public void finish() throws InternalErrorException {
        JSONArray dependencies = new JSONArray();
        for (Dependency dependency: queue) {
            dependencies.put(dependency.toJSON());
        }
        DatabaseOperations.getInstance().generateResponsePage(responseId, organization, dependencies, Constants.getInstance().getDependenciesArrayName());
    }
}
