package upc.similarity.compareapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class ReqProject implements Serializable {

    @JsonProperty(value="reqs_to_compare")
    private List<String> reqsToCompare;
    @JsonProperty(value="project_reqs")
    private List<String> projectReqs;

    public ReqProject(List<String> reqsToCompare, List<String> projectReqs) {
        this.reqsToCompare = reqsToCompare;
        this.projectReqs = projectReqs;
    }

    public List<String> getReqsToCompare() {
        return reqsToCompare;
    }

    public List<String> getProjectReqs() {
        return projectReqs;
    }
}
