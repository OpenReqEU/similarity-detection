package upc.similarity.semilarapi.service;

import semilar.data.Sentence;
import semilar.sentencemetrics.AbstractComparer;
import upc.similarity.semilarapi.entity.Dependency;
import upc.similarity.semilarapi.entity.Requirement;

import java.util.List;

public class ComparisonBetweenSentences {

    private AbstractComparer comparer;
    private boolean compare_text;
    private boolean efficiency;
    private float threshold;
    private String component;

    public ComparisonBetweenSentences(AbstractComparer comparer, String compare, float threshold, boolean efficiency, String component) {
        this.comparer = comparer;
        this.compare_text = assign_number_compare(compare);
        this.threshold = threshold;
        this.efficiency = efficiency;
        this.component = component;
    }

    public float compare_two_requirements(Requirement req1, Requirement req2) {
        float name = 0.0f;
        float text = 0.0f;
        //float comments = 0.0f;
        Sentence sentence1;
        Sentence sentence2;
        sentence1 = req1.getSentence_name();
        sentence2 = req2.getSentence_name();
        if (sentence1 != null && sentence2 != null) {
            try {
                name = comparer.computeSimilarity(sentence1, sentence2);
                if (efficiency && name >= threshold) return name;
            } catch (Exception e) {
                name = 0.0f;
            }
        }
        if (compare_text) {
            sentence1 = req1.getSentence_text();
            sentence2 = req2.getSentence_text();
            if (sentence1 != null && sentence2 != null) {
                try {
                    text = comparer.computeSimilarity(sentence1, sentence2);
                    if (efficiency && text >= threshold) return text;
                } catch (Exception e) {
                    text = 0.0f;
                }
            }
        }
        /*sentence1 = req1.getSentence_comments();
        sentence2 = req2.getSentence_comments();
        if (sentence1 != null && sentence2 != null) {
            comments = greedyComparerWNLin.computeSimilarity(sentence1,sentence2);
            if (comments >= threshold) return comments;
        }*/

        return Float.max(name,text);
    }


    private boolean assign_number_compare(String compare) {
        return compare.equals("true");

    }

    public Dependency compare_two_requirements_dep(Requirement req1, Requirement req2) {

        if (req1.getId() == null || req2.getId() == null) return null;
        if (req1.getId().equals(req2.getId())) return null;
        float result = compare_two_requirements(req1,req2);
        return new Dependency(result, req1.getId(), req2.getId(), "proposed", "similar", component);
    }

    public boolean existsDependency(String fromid, String toid, List<Dependency> dependencies) {

        for (int i = 0; i < dependencies.size(); i++) {
            Dependency dependency = dependencies.get(i);

            if (dep_ok(dependency)) {
                if (((dependency.getFromid().equals(fromid) &&
                        dependency.getToid().equals(toid)) || (dependency.getToid().equals(fromid) &&
                        dependency.getFromid().equals(toid))) &&
                        (dependency.getDependency_type().toLowerCase().equals("similar") || dependency.getDependency_type().toLowerCase().equals("duplicates")))
                    return true;
            }
        }
        return false;
    }

    private boolean dep_ok(Dependency dep) {
        if (dep.getFromid() == null || dep.getToid() == null || dep.getDependency_type() == null) return false;
        else return true;
    }
}
