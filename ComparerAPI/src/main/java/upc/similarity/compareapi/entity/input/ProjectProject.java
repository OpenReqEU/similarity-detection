package upc.similarity.compareapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class ProjectProject implements Serializable {

    @JsonProperty(value="first_project_requirements")
    private List<String> firstProjectRequirements;
    @JsonProperty(value="second_project_requirements")
    private List<String> secondProjectRequirements;

    public ProjectProject() {}

    public ProjectProject(List<String> firstProjectRequirements, List<String> secondProjectRequirements) {
        this.firstProjectRequirements = firstProjectRequirements;
        this.secondProjectRequirements = secondProjectRequirements;
    }

    public List<String> getFirstProjectRequirements() {
        return firstProjectRequirements;
    }

    public List<String> getSecondProjectRequirements() {
        return secondProjectRequirements;
    }
}