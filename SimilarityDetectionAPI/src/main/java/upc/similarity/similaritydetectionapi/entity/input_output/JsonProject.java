package upc.similarity.similaritydetectionapi.entity.input_output;

import com.fasterxml.jackson.annotation.*;
import io.swagger.annotations.ApiModel;
import upc.similarity.similaritydetectionapi.entity.Project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApiModel(value = "JsonProject", description = "OpenReqJson with projects")
public class JsonProject implements Serializable {

    @JsonProperty(value="projects")
    private List<Project> projects;

    public JsonProject() {
        this.projects = new ArrayList<>();
    }

    public List<Project> getProjects() {
        return projects;
    }

    public boolean OK() {
        return !(projects.size() == 0);
    }
}
