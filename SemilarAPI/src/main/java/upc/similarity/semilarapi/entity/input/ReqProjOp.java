package upc.similarity.semilarapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import upc.similarity.semilarapi.entity.Dependency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Input class for ReqProj operations with the specified requirements, a list of requirements and dependencies
public class ReqProjOp implements Serializable {

    @JsonProperty(value="requirements")
    private List<RequirementId> requirements;
    @JsonProperty(value="project_requirements")
    private List<RequirementId> project_requirements;
    @JsonProperty(value="dependencies")
    private List<Dependency> dependencies;

    public ReqProjOp() {
        this.project_requirements = new ArrayList<>();
        this.requirements = new ArrayList<>();
        this.dependencies = new ArrayList<>();
    }

    public List<RequirementId> getProject_requirements() {
        return project_requirements;
    }

    public List<RequirementId> getRequirements() {
        return requirements;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }
}
