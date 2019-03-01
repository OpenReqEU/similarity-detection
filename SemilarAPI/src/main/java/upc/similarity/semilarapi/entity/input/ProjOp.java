package upc.similarity.semilarapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import upc.similarity.semilarapi.entity.Dependency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Input class for Proj operations with requirements and dependencies
public class ProjOp implements Serializable {

    @JsonProperty(value="requirements")
    private List<RequirementId> requirements;
    @JsonProperty(value="dependencies")
    private List<Dependency> dependencies;

    public ProjOp() {
        this.requirements = new ArrayList<>();
        this.dependencies = new ArrayList<>();
    }

    public List<RequirementId> getRequirements() {
        return requirements;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }
}
