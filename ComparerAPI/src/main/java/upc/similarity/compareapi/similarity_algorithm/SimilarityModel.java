package upc.similarity.compareapi.similarity_algorithm;

import java.util.List;

public interface SimilarityModel {

    boolean containsRequirement(String requirementId);

    List<String> getRequirementsIds();


}
