package upc.similarity.compareapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.exception.BadRequestException;
import upc.similarity.compareapi.entity.exception.InternalErrorException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Clusters implements Serializable {

    @JsonProperty(value="requirements")
    private List<Requirement> requirements;
    @JsonProperty(value="dependencies")
    private List<Dependency> dependencies;


    public Clusters() {
        this.requirements = new ArrayList<>();
        this.dependencies = new ArrayList<>();
    }

    public Clusters(JSONObject jsonObject) throws BadRequestException, InternalErrorException {
        this.requirements = new ArrayList<>();
        this.dependencies = new ArrayList<>();
        if (jsonObject.containsKey("requirements")) {
            JSONArray jsonArray = (JSONArray) jsonObject.get("requirements");
            for (Object aux : jsonArray) {
                if (aux instanceof JSONObject) {
                    JSONObject requirement = (JSONObject) aux;
                    requirements.add(new Requirement(requirement));
                } else throw new InternalErrorException("Error while converting input clusters json");
            }
        }
        if (jsonObject.containsKey("dependencies")) {
            JSONArray jsonArray = (JSONArray) jsonObject.get("dependencies");
            for (Object aux : jsonArray) {
                if (aux instanceof JSONObject) {
                    JSONObject requirement = (JSONObject) aux;
                    dependencies.add(new Dependency(requirement));
                } else throw new InternalErrorException("Error while converting input clusters json");
            }
        }
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public boolean inputOk() {
        return !requirements.isEmpty();
    }
}
