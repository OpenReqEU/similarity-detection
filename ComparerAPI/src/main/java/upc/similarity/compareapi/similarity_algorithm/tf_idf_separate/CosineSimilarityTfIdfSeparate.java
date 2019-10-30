package upc.similarity.compareapi.similarity_algorithm.tf_idf_separate;

import java.util.*;
import static java.lang.StrictMath.sqrt;

public class CosineSimilarityTfIdfSeparate {

    private double cutOffTopics;
    private double importanceLow;

    public CosineSimilarityTfIdfSeparate(double cutOffTopics, double importanceLow) {
        this.cutOffTopics = cutOffTopics;
        this.importanceLow = importanceLow;
    }

    /**
     * Method that computes the similarity between two documents of the tfidf model
     * @param modelTfIdf    tfidf model
     * @param a id of the first requirement
     * @param b id of the second requirement
     * @return a double between -1 and 1
     */
    public double compute(SimilarityModelTfIdfSeparate modelTfIdf, String a, String b) {
        Map<String, Map<String, Double>> names = modelTfIdf.getNames();
        Map<String, Map<String, Double>> texts = modelTfIdf.getTexts();

        Map<String,Double> topicWordsA = names.get(a);
        Map<String,Double> topicWordsB = names.get(b);
        Map<String,Double> lowWordsA = texts.get(a);
        Map<String,Double> lowWordsB = texts.get(b);

        double result;

        double scoreTopics = computeSection(topicWordsA,topicWordsB);
        if (scoreTopics > cutOffTopics) {
            double scoreLow = computeSection(lowWordsA,lowWordsB);
            result = scoreTopics*(1-importanceLow) + scoreLow*importanceLow;
        } else {
            result = scoreTopics*(1-importanceLow);
        }
        return result;
    }

    /*
    Private methods
     */

    private double computeSection(Map<String, Double> wordsA, Map<String, Double> wordsB) {
        double cosine = 0.0;
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

    private Double norm(Map<String, Double> wordsB) {
        double norm=0.0;
        for (Map.Entry<String,Double> value: wordsB.entrySet()) {
            norm+=value.getValue()*value.getValue();
        }
        return sqrt(norm);
    }
}
