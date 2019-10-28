package upc.similarity.compareapi.similarity_algorithm.tf_idf_double;

import upc.similarity.compareapi.similarity_algorithm.tf_idf.SimilarityModelTfIdf;

import java.util.*;

import static java.lang.StrictMath.ceil;
import static java.lang.StrictMath.sqrt;

public class CosineSimilarityTfIdfDouble {

    private class Pair {
        private String key;
        private double value;
        public Pair(String key, double value) {
            this.key = key;
            this.value = value;
        }
        public double getValue() {
            return value;
        }
        public String getKey() {
            return key;
        }
    }

    private double topicThreshold;
    private double cutOffTopics;
    private double importanceLow;

    public CosineSimilarityTfIdfDouble(double topicThreshold, double cutOffTopics, double importanceLow) {
        this.topicThreshold = topicThreshold;
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
    public double compute(SimilarityModelTfIdf modelTfIdf, String a, String b) {
        Map<String, Map<String, Double>> documents = modelTfIdf.getDocs();
        Map<String, Double> wordsA = documents.get(a);
        Map<String, Double> wordsB = documents.get(b);

        Map<String,Double> topicWordsA = new HashMap<>();
        Map<String,Double> topicWordsB = new HashMap<>();
        Map<String,Double> lowWordsA = new HashMap<>();
        Map<String,Double> lowWordsB = new HashMap<>();

        fillTopicsMaps(wordsA,topicWordsA,lowWordsA);
        fillTopicsMaps(wordsB,topicWordsB,lowWordsB);

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

    private void fillTopicsMaps(Map<String, Double> words, Map<String,Double> topicWords, Map<String,Double> lowWords) {
        TreeSet<Pair> treeSet = new TreeSet<>(Comparator.comparing(Pair::getValue).thenComparing(Pair::getKey));
        for (Map.Entry<String,Double> word: words.entrySet()) {
            String wordId = word.getKey();
            double value = word.getValue();
            treeSet.add(new Pair(wordId,value));
        }

        int lastPosTopics = Math.min((int) ceil(topicThreshold),treeSet.size());
        int i = 0;
        int index = treeSet.size() - lastPosTopics;
        for (Pair pair: treeSet) {
            String wordId = pair.getKey();
            double value = pair.getValue();
            if (i >= index) {
                topicWords.put(wordId,value);
            } else {
                lowWords.put(wordId,value);
            }
            ++i;
        }
    }

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
