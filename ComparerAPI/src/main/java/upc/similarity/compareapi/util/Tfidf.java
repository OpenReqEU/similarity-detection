package upc.similarity.compareapi.util;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import upc.similarity.compareapi.config.Control;
import upc.similarity.compareapi.entity.Model;
import upc.similarity.compareapi.exception.InternalErrorException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class Tfidf {

    private static Tfidf instance = new Tfidf();
    public static boolean cutOffDummy = false;

    public static void setCutOffDummy(boolean cutOffDummy) {
        Tfidf.cutOffDummy = cutOffDummy;
    }

    private Tfidf() {}

    private double computeCutOffParameter(long totalSize) {
        if (cutOffDummy) return -1;
        else return (totalSize > 100) ? 10 : (-6.38 + 3.51*Math.log(totalSize));
    }

    public void addNewReqs(List<String> newRequirements, List<String> newIds, Model model, Map<String, Integer> oldCorpusFrequency) throws InternalErrorException {
        Map<String, Integer> newCorpusFrequency = model.getCorpusFrequency();
        Map<String, Map<String, Double>> docs = model.getDocs();
        int finalSize = docs.size()+newRequirements.size();
        double cutOffParameter = computeCutOffParameter(finalSize);

        //preprocess new requirements
        List<List<String>> newDocs = new ArrayList<>();
        for (String s : newRequirements) {
            try {
                newDocs.add(englishAnalyze(s));
            } catch (IOException e) {
                Control.getInstance().showErrorMessage("Error while loading preprocess pipeline");
                throw new InternalErrorException("Error loading preprocess pipeline");
            }
        }
        newDocs = preProcess(newDocs);

        //tf new requirements
        List<Map<String, Integer>> wordBagArray = new ArrayList<>();
        for (List<String> doc : newDocs) {
            wordBagArray.add(tf(doc,newCorpusFrequency));
        }

        //idf old requirements
        Iterator it = docs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Map<String, Double> words = (Map<String, Double>) pair.getValue();
            recomputeIdfValues(words, oldCorpusFrequency, newCorpusFrequency, docs.size(), finalSize);
        }

        //idf new requirements
        int i = 0;
        for (List<String> doc : newDocs) {
            HashMap<String, Double> aux = new HashMap<>();
            for (String s : doc) {
                Double idf = idf(finalSize, newCorpusFrequency.get(s));
                Integer tf = wordBagArray.get(i).get(s);
                double tfidf = idf * tf;
                if (tfidf>=cutOffParameter) aux.put(s, tfidf);
            }
            docs.put(newIds.get(i),aux);
            ++i;
        }
    }

    private void recomputeIdfValues(Map<String, Double> words, Map<String, Integer> oldCorpusFrequency, Map<String, Integer> newCorpusFrequency, double oldSize, double newSize) {
        Iterator it = words.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next(); //problem: if the value was 0 (because corpus + 1 == totalSize) it will be always 0
            String word = (String) pair.getKey();
            double score = (double) pair.getValue();
            double newScore = recomputeIdf(score, oldSize, oldCorpusFrequency.get(word), newSize, newCorpusFrequency.get(word));
            pair.setValue(newScore);
        }
    }

    private double recomputeIdf(double oldValue, double oldSize, double oldCorpusFrequency, double newSize, double newCorpusFrequency) {
        double quocient = Math.log(oldSize/(oldCorpusFrequency+1));
        return (quocient <= 0) ? 0 : (oldValue * Math.log(newSize/(newCorpusFrequency+1)))/quocient;
    }

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
        List<List<String>> processed = preProcess(docs);
        return tfIdf(processed,ids,corpusFrequency);

    }

    private Map<String,Map<String, Double>> tfIdf(List<List<String>> docs, List<String> corpus, Map<String, Integer> corpusFrequency) {
        double cutOffParameter = computeCutOffParameter(docs.size());
        Control.getInstance().showInfoMessage("Cutoff: " + cutOffParameter);
        Map<String,Map<String, Double>> tfidfComputed = new HashMap<>();
        List<Map<String, Integer>> wordBag = new ArrayList<>();
        for (List<String> doc : docs) {
            wordBag.add(tf(doc,corpusFrequency));
        }
        int i = 0;
        for (List<String> doc : docs) {
            HashMap<String, Double> aux = new HashMap<>();
            for (String s : doc) {
                Double idf = idf(docs.size(), corpusFrequency.get(s));
                Integer tf = wordBag.get(i).get(s);
                double tfidf = idf * tf;
                if (tfidf>=cutOffParameter) aux.put(s, tfidf);
            }
            tfidfComputed.put(corpus.get(i),aux);
            ++i;
        }
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
