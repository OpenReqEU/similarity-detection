package upc.similarity.compareapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class ReqProject implements Serializable {

    private List<String> reqsToCompare;
    private List<String> projectReqs;

    @JsonProperty(value="reqs_to_compare")
    public List<String> getReqsToCompare() {
        return reqsToCompare;
    }

    @JsonProperty(value="project_reqs")
    public List<String> getProjectReqs() {
        return projectReqs;
    }
}
