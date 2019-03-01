package upc.similarity.semilarapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

public class Cluster implements Serializable {

    @JsonProperty(value="req")
    private Requirement req_older;
    @JsonProperty(value="specifiedRequirements")
    private List<Req_with_score> specifiedRequirements;

    private Timestamp req_time;

    public Cluster(Requirement req_older, List<Req_with_score> specifiedRequirements) {
        this.req_older = req_older;
        this.specifiedRequirements = specifiedRequirements;
        this.req_time = new Timestamp(req_older.getCreated_at());
    }

    public Requirement getReq_older() {
        return req_older;
    }

    public List<Req_with_score> getSpecifiedRequirements() {
        return specifiedRequirements;
    }

    public void addReq(Requirement requirement, float score) {
        if (requirement.getCreated_at() != null && requirement.getCreated_at() != 0) {
            Timestamp stampNew = new Timestamp(requirement.getCreated_at());
            if (stampNew.before(req_time)) {
                req_older = requirement;
                req_time = stampNew;
            }
        }
        specifiedRequirements.add(new Req_with_score(requirement,score));
    }
}
