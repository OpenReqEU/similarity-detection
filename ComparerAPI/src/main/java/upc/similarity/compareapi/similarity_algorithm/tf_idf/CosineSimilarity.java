package upc.similarity.compareapi.similarity_algorithm.tf_idf;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.StrictMath.sqrt;

public class CosineSimilarity {

    private static CosineSimilarity instance = new CosineSimilarity();

    private CosineSimilarity() {}

    public static CosineSimilarity getInstance() {
        return instance;
    }

    /**
     * Method that computes the similarity between two documents of the tfidf model
     * @param modelTfIdf    tfidf model
     * @param a id of the first requirement
     * @param b id of the second requirement
     * @return a double between -1 and 1
     */
    public double compute(SimilarityTfIdfModel modelTfIdf, String a, String b) {
        Map<String, Map<String, Double>> documents = modelTfIdf.getDocs();
        double cosine = 0.0;
        Map<String, Double> wordsA = documents.get(a);
        Map<String, Double> wordsB = documents.get(b);
        Set<String> intersection = new HashSet<>(wordsA.keySet());
        intersection.retainAll(wordsB.keySet());
        for (String s : intersection) {
            Double forA = wordsA.get(s);
            Double forB = wordsB.get(s);
            cosine += forA * forB;
        }
        double normA = norm(wordsA);
        double normB = norm(wordsB);

        if (normA == 0 || normB == 0) return 0;

        cosine = cosine / (normA * normB);
        return cosine;
    }

    /*
    Private methods
     */

    private Double norm(Map<String, Double> wordsB) {
        double norm=0.0;
        for (Map.Entry<String,Double> value: wordsB.entrySet()) {
            norm+=value.getValue()*value.getValue();
        }
        return sqrt(norm);
    }
}
