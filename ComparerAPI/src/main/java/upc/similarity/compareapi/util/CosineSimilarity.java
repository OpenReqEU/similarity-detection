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

    public double compute(double[] a, double[] b) {
        double cosine=0.0;
        for (int i = 0; i < a.length; ++i) {
            cosine += a[i]*b[i];
        }

        double normA=norm(a);
        double normB=norm(b);

        cosine=cosine/(normA*normB);
        return cosine;
    }

    private double norm(double[] a) {
        double norm=0.0;
        for (int i = 0; i < a.length; ++i) {
            norm += a[i]*a[i];
        }
        return sqrt(norm);
    }

    public double compute(Map<String, Map<String, Double>> res, String a, String b) {
        double cosine=0.0;
        Map<String,Double> wordsA=res.get(a);
        Map<String,Double> wordsB=res.get(b);
        Set<String> intersection= new HashSet<String>(wordsA.keySet());
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
        for (String s:wordsB.keySet()) {
            double value = wordsB.get(s);
            norm+=value*value;
        }
        return sqrt(norm);
    }
}
