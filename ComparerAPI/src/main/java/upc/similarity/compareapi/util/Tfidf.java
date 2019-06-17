package upc.similarity.compareapi.util;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import upc.similarity.compareapi.config.Control;
import upc.similarity.compareapi.exception.InternalErrorException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tfidf {

    private static double cutoffParameter=10;
    private static Tfidf instance = new Tfidf();

    private Tfidf() {}

    public Map<String, Map<String, Double>> extractKeywords(List<String> corpus, List<String> ids, Map<String, Integer> corpusFrequency) throws InternalErrorException {
        List<List<String>> docs = new ArrayList<>();
        for (String s : corpus) {
            try {
                docs.add(englishAnalyze(s));
            } catch (IOException e) {
                Control.getInstance().showErrorMessage("Error while loading preprocess pipeline");
                throw new InternalErrorException("Error loading preprocess pipeline");
            }
        }
        List<List<String>> processed=preProcess(docs);
        return tfIdf(processed,ids,corpusFrequency);

    }

    private Map<String,Map<String, Double>> tfIdf(List<List<String>> docs, List<String> corpus, Map<String, Integer> corpusFrequency) {
        Map<String,Map<String, Double>> tfidfComputed = new HashMap<>();
        List<Map<String, Integer>> wordBag = new ArrayList<>();
        for (List<String> doc : docs) {
            wordBag.add(tf(doc,corpusFrequency));
        }
        int i = 0;
        long total = 0;
        long i_0 = 0;
        long i_1 = 0;
        long i_2 = 0;
        long i_3 = 0;
        long i_4 = 0;
        long i_5 = 0;
        long i_6 = 0;
        long i_7 = 0;
        long i_8 = 0;
        long i_9 = 0;
        long i_10 = 0;
        for (List<String> doc : docs) {
            HashMap<String, Double> aux = new HashMap<>();
            for (String s : doc) {
                Double idf = idf(docs.size(), corpusFrequency.get(s));
                Integer tf = wordBag.get(i).get(s);
                double tfidf = idf * tf;
                ++total;
                if (tfidf>=cutoffParameter) aux.put(s, tfidf);
                int value = (int) Math.floor(tfidf);
                switch (value) {
                    case 0:
                        ++i_1;
                        ++i_2;
                        ++i_3;
                        ++i_4;
                        ++i_5;
                        ++i_6;
                        ++i_7;
                        ++i_8;
                        ++i_9;
                        ++i_10;
                    case 1:
                        ++i_2;
                        ++i_3;
                        ++i_4;
                        ++i_5;
                        ++i_6;
                        ++i_7;
                        ++i_8;
                        ++i_9;
                        ++i_10;
                        break;
                    case 2:
                        ++i_3;
                        ++i_4;
                        ++i_5;
                        ++i_6;
                        ++i_7;
                        ++i_8;
                        ++i_9;
                        ++i_10;
                        break;
                    case 3:
                        ++i_4;
                        ++i_5;
                        ++i_6;
                        ++i_7;
                        ++i_8;
                        ++i_9;
                        ++i_10;
                        break;
                    case 4:
                        ++i_5;
                        ++i_6;
                        ++i_7;
                        ++i_8;
                        ++i_9;
                        ++i_10;
                        break;
                    case 5:
                        ++i_6;
                        ++i_7;
                        ++i_8;
                        ++i_9;
                        ++i_10;
                        break;
                    case 6:
                        ++i_7;
                        ++i_8;
                        ++i_9;
                        ++i_10;
                        break;
                    case 7:
                        ++i_8;
                        ++i_9;
                        ++i_10;
                        break;
                    case 8:
                        ++i_9;
                        ++i_10;
                        break;
                    case 9:
                        ++i_10;
                        break;
                }
            }
            tfidfComputed.put(corpus.get(i),aux);
            ++i;
        }
        System.out.println("total: " + total);
        System.out.println(i_0);
        System.out.println(i_1);
        System.out.println(i_2);
        System.out.println(i_3);
        System.out.println(i_4);
        System.out.println(i_5);
        System.out.println(i_6);
        System.out.println(i_7);
        System.out.println(i_8);
        System.out.println(i_9);
        System.out.println(i_10);
        return tfidfComputed;
    }

    private double idf(int size, int frequency) {
        return Math.log(size / (frequency + 1.0));
    }

    private Map<String, Integer> tf(List<String> doc, Map<String, Integer> corpusFrequency) {
        Map<String, Integer> frequency = new HashMap<>();
        for (String s : doc) {
            if (frequency.containsKey(s)) frequency.put(s, frequency.get(s) + 1);
            else {
                frequency.put(s, 1);
                if (corpusFrequency.containsKey(s)) corpusFrequency.put(s, corpusFrequency.get(s) + 1);
                else corpusFrequency.put(s, 1);
            }

        }
        return frequency;
    }

    private List<List<String>> preProcess(List<List<String>> corpus) {
        return corpus;
    }

    private List<String> englishAnalyze(String text) throws IOException {
        Analyzer analyzer = CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("lowercase")
                .addTokenFilter("commongrams")
                .addTokenFilter("porterstem")
                .addTokenFilter("stop")
                .build();
        return analyze(text, analyzer);
    }

    private List<String> analyze(String text, Analyzer analyzer) throws IOException {
        List<String> result = new ArrayList<>();
        TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text));
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            result.add(attr.toString());
        }
        return result;
    }

    public static Tfidf getInstance() {
        return instance;
    }
}
