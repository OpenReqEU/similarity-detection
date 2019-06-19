package upc.similarity.similaritydetectionapi.entity.input_output;

import com.fasterxml.jackson.annotation.*;
import io.swagger.annotations.ApiModel;
import upc.similarity.similaritydetectionapi.entity.Project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApiModel(value = "Projects", description = "OpenReqJson with projects")
public class Projects implements Serializable {

    @JsonProperty(value="projects")
    private List<Project> projects;

    public Projects() {
        this.projects = new ArrayList<>();
    }

    public List<Project> getProjects() {
        return projects;
    }

    public boolean inputOk() {
        return !projects.isEmpty();
    }
}
