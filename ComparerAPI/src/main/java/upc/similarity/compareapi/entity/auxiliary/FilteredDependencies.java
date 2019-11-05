package upc.similarity.compareapi.entity.auxiliary;

import javafx.util.Pair;
import upc.similarity.compareapi.entity.Dependency;

import java.util.*;

public class FilteredDependencies {

    private List<Dependency> acceptedDependencies;
    private List<Dependency> rejectedDependencies;
    private List<Dependency> deletedDependencies;

    public FilteredDependencies(List<Dependency> dependencies, Map<String, Pair<String,Long>> reqDepsToRemove) {
        Collection<Dependency> filteredDependencies = filterDependencies(dependencies,reqDepsToRemove);
        acceptedDependencies = new ArrayList<>();
        rejectedDependencies = new ArrayList<>();
        deletedDependencies = new ArrayList<>();
        splitDependencies(filteredDependencies);
    }

    public List<Dependency> getAcceptedDependencies() {
        return acceptedDependencies;
    }

    public List<Dependency> getDeletedDependencies() {
        return deletedDependencies;
    }

    public List<Dependency> getRejectedDependencies() {
        return rejectedDependencies;
    }


    /*
    Private methods
     */

    private Collection<Dependency> filterDependencies(List<Dependency> dependencies, Map<String,Pair<String,Long>> reqDepsToRemove) {
        Map<String,Dependency> notRepeatedDeps = new HashMap<>();
        for (Dependency dependency: dependencies) {
            String fromId = dependency.getFromid();
            String toId = dependency.getToid();
            String status = dependency.getStatus();
            String type = dependency.getDependencyType();
            if (fromId != null && toId != null && status != null && type != null && type.equals("similar") && !clashRequirement(dependency,reqDepsToRemove)) {
                boolean b1 = notRepeatedDeps.containsKey(fromId+toId);
                boolean b2 = notRepeatedDeps.containsKey(toId+fromId);
                if (b1 || b2) {
                    Dependency oldDependency;
                    if (b1) oldDependency = notRepeatedDeps.get(fromId+toId);
                    else oldDependency = notRepeatedDeps.get(toId+fromId);
                    if (oldDependency.computeTime() < dependency.computeTime()) {
                        notRepeatedDeps.put(fromId+toId,dependency);
                    }
                } else notRepeatedDeps.put(fromId+toId,dependency);
            }
        }
        return notRepeatedDeps.values();
    }

    private boolean clashRequirement(Dependency dependency, Map<String, Pair<String,Long>> reqDepsToRemove) {
        boolean result = false;
        if (reqDepsToRemove != null) {
            String fromId = dependency.getFromid();
            String toId = dependency.getToid();
            boolean b1 = reqDepsToRemove.containsKey(fromId);
            boolean b2 = reqDepsToRemove.containsKey(toId);
            if (b1 || b2) {
                Pair action;
                if (b1 && b2) {
                    Pair pairA = reqDepsToRemove.get(fromId);
                    Pair pairB = reqDepsToRemove.get(toId);
                    String actionA = (String) pairA.getKey();
                    String actionB = (String) pairB.getKey();
                    if (actionA.equals("all")) action = pairA;
                    else {
                        if (actionB.equals("all")) action = pairB;
                        else {
                            long timeA = (long) pairA.getValue();
                            long timeB = (long) pairB.getValue();
                            if (timeA >= timeB) action = pairA;
                            else action = pairB;
                        }
                    }
                } else {
                    if (b1) action = reqDepsToRemove.get(fromId);
                    else action = reqDepsToRemove.get(toId);
                }
                if (action.getKey().equals("all")) result = true;
                else if (dependency.computeTime() < ((long) action.getValue())) result = true;
            }
        }
        return result;
    }

    private void splitDependencies(Collection<Dependency> dependencies) {
        for (Dependency dependency: dependencies) {
            String status = dependency.getStatus();
            switch (status) {
                case "accepted":
                    acceptedDependencies.add(dependency);
                    break;
                case "rejected":
                    rejectedDependencies.add(dependency);
                    break;
                case "deleted":
                    deletedDependencies.add(dependency);
                    break;
            }
        }
    }
}
