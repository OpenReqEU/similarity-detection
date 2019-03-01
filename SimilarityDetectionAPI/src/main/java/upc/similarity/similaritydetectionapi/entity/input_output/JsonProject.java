package upc.similarity.similaritydetectionapi.entity.input_output;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModel;
import org.json.JSONObject;
import upc.similarity.similaritydetectionapi.entity.Dependency;
import upc.similarity.similaritydetectionapi.entity.Project;
import upc.similarity.similaritydetectionapi.entity.Requirement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "JsonProject", description = "OpenReqJson with requirements, projects and dependencies")
public class JsonProject implements Serializable {

    @JsonProperty(value="projects")
    private List<Project> projects;

    @JsonProperty(value="requirements")
    private List<Requirement> requirements;

    @JsonProperty(value="dependencies")
    private List<Dependency> dependencies;

    private Map<String, Object> optional = new HashMap<>();

    @JsonAnySetter
    public void addOptional(String name, Object value) {
        optional.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getOptional() {
        return optional;
    }

    public JsonProject() {
        this.projects = new ArrayList<>();
        this.requirements = new ArrayList<>();
        this.dependencies = new ArrayList<>();
    }

    public List<Project> getProjects() {
        return projects;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void add_dependency(Dependency dependency) {
        dependencies.add(dependency);
    }

    public JSONObject toJSON() {
        String jsonInString = "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            jsonInString = mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new InternalError("Error in json conversion");
        }
        return new JSONObject(jsonInString);
    }

    public boolean OK() {
        if (projects.size() == 0 || requirements.size() == 0) return false;
        else return true;
    }
}
