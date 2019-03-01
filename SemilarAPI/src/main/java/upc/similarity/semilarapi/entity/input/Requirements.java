package upc.similarity.semilarapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import upc.similarity.semilarapi.entity.Requirement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Input class for preprocess operation with a list of requirements
public class Requirements implements Serializable {

    @JsonProperty(value="requirements")
    private List<Requirement> requirements;

    public Requirements() {
        requirements = new ArrayList<>();
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }
}
