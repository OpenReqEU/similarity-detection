package upc.similarity.similaritydetectionapi.entity.input_output;

import com.fasterxml.jackson.annotation.*;
import io.swagger.annotations.ApiModel;
import upc.similarity.similaritydetectionapi.entity.Project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApiModel(value = "Projects", description = "OpenReqJson with projects")
public class ProjectsModel implements Input, Serializable {

    @JsonProperty(value="projects")
    private List<Project> projects;

    public ProjectsModel() {
        this.projects = new ArrayList<>();
    }

    public List<Project> getProjects() {
        return projects;
    }

    @Override
    public boolean inputOk() {
        return !projects.isEmpty();
    }

    @Override
    public String checkMessage() {
        return "The input projects array is empty";
    }
}
