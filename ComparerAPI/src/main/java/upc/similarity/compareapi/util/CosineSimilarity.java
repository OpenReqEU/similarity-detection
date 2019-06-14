package upc.similarity.compareapi.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.StrictMath.sqrt;

public class CosineSimilarity {

    private static CosineSimilarity instance = new CosineSimilarity();

    private CosineSimilarity() {};

    public static CosineSimilarity getInstance() {
        return instance;
    }

    public double compute(Map<String, Map<String, Double>> res, String a, String b) {
        double cosine=0.0;
        Map<String,Double> wordsA=res.get(a);
        Map<String,Double> wordsB=res.get(b);
        Set<String> intersection= new HashSet<>(wordsA.keySet());
        intersection.retainAll(wordsB.keySet());
        for (String s: intersection) {
            Double forA=wordsA.get(s);
            Double forB=wordsB.get(s);
            cosine+=forA*forB;
        }
        double normA=norm(wordsA);
        double normB=norm(wordsB);

        if (normA == 0 || normB == 0) return 0;

        cosine=cosine/(normA*normB);
        return cosine;
    }

    private Double norm(Map<String, Double> wordsB) {
        double norm=0.0;
        for (Map.Entry<String,Double> value: wordsB.entrySet()) {
            norm+=value.getValue()*value.getValue();
        }
        return sqrt(norm);
    }
}
