package upc.similarity.similaritydetectionapi.entity.input_output;

import com.fasterxml.jackson.annotation.*;
import io.swagger.annotations.ApiModel;
import upc.similarity.similaritydetectionapi.entity.Project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApiModel(value = "Projects", description = "OpenReqJson with projects")
public class Projects implements Input, Serializable {

    @JsonProperty(value="projects")
    private List<Project> projectsArray;

    public Projects() {
        this.projectsArray = new ArrayList<>();
    }

    public List<Project> getProjects() {
        return projectsArray;
    }

    @Override
    public boolean inputOk() {
        return !projectsArray.isEmpty();
    }

    @Override
    public String checkMessage() {
        return "The input projects array is empty";
    }
}
