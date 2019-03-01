package upc.similarity.semilarapi.entity;

public class Req_with_score {

    private Requirement requirement;
    private float score;

    public Req_with_score(Requirement requirement, float score) {
        this.requirement = requirement;
        this.score = score;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public float getScore() {
        return score;
    }
}
