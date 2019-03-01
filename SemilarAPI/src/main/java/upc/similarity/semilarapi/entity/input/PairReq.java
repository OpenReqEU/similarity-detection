package upc.similarity.semilarapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import upc.similarity.semilarapi.entity.Dependency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Input class for ReqReq operation with two requirements
public class PairReq implements Serializable {

    @JsonProperty(value="req1")
    private RequirementId req1;
    @JsonProperty(value="req2")
    private RequirementId req2;
    @JsonProperty(value="dependencies")
    private List<Dependency> dependencies;

    public PairReq() {
        this.dependencies = new ArrayList<>();
    }

    public RequirementId getReq1() {
        return req1;
    }

    public RequirementId getReq2() {
        return req2;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }
}
