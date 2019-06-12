package upc.similarity.compareapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class ReqProject implements Serializable {

    private List<String> reqs_to_compare;
    private List<String> project_reqs;

    @JsonProperty(value="reqs_to_compare")
    public List<String> getReqs_to_compare() {
        return reqs_to_compare;
    }

    @JsonProperty(value="project_reqs")
    public List<String> getProject_reqs() {
        return project_reqs;
    }
}
