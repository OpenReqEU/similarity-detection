package upc.similarity.semilarapi.service;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import upc.similarity.semilarapi.dao.modelDAO;
import upc.similarity.semilarapi.dao.SQLiteDAO;
import upc.similarity.semilarapi.entity.*;
import upc.similarity.semilarapi.exception.BadRequestException;

import java.io.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static java.lang.StrictMath.sqrt;

@Service("semilarService")
public class SemilarServiceImpl implements SemilarService {

    private static Double cutoffParameter=-1.0;
    private static String component = "Similarity-UPC";
    private static String status = "proposed";
    private static String dependency_type = "duplicates";
    private modelDAO modelDAO = getValue();

    private modelDAO getValue() {
        try {
            return new SQLiteDAO();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public void buildModel(String compare, String organization, List<Requirement> reqs) throws SQLException, Exception {
        show_time("start");
        Map<String, Integer> corpusFrequency = new HashMap<>();
        List<String> text = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        buildCorpus(compare,reqs,text,ids);
        Map<String, Map<String, Double>> model = extractKeywords(text,ids,corpusFrequency);
        modelDAO.saveModel(organization,new Model(model,corpusFrequency));
        show_time("finish");
    }

    @Override
    public List<Dependency> simReqReq(String organization, String req1, String req2) throws Exception {
        show_time("start");
        List<Dependency> result = new ArrayList<>();
        Model model = modelDAO.getModel(organization);
        if (!model.getModel().containsKey(req1)) throw new BadRequestException("The requirement with id " + req1 + " is not present in the model loaded form the database");
        if (!model.getModel().containsKey(req2)) throw new BadRequestException("The requirement with id " + req2 + " is not present in the model loaded form the database");
        double score = cosine(model.getModel(),req1,req2);
        result.add(new Dependency(score,req1,req2,status,dependency_type,component));
        show_time("finish");
        return result;
    }

    @Override
    public List<Dependency> simReqProject(String organization, String req, List<String> project_reqs) throws Exception {
        show_time("start");
        List<Dependency> result = new ArrayList<>();
        Model model = modelDAO.getModel(organization);
        if (!model.getModel().containsKey(req)) throw new BadRequestException("The requirement with id " + req + " is not present in the model loaded form the database");
        for (String req2: project_reqs) {
            if (!req.equals(req2)) {
                if (!model.getModel().containsKey(req2)) throw new BadRequestException("The requirement with id " + req2 + " is not present in the model loaded form the database");
                double score = cosine(model.getModel(),req,req2);
                result.add(new Dependency(score,req,req2,status,dependency_type,component));
            }
        }
        show_time("finish");
        return result;
    }

    @Override
    public void clearDB() throws SQLException {
        modelDAO.clearDB();
    }





    /*
    auxiliary operations
     */

    private void show_time(String text) {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int hour = now.getHour();
        int minute = now.getMinute();
        System.out.println(text + " -- " + hour + ":" + minute + "  " + month + "/" + day + "/" + year);
    }

    private void buildCorpus(String compare, List<Requirement> requirements, List<String> array_text, List<String> array_ids) throws BadRequestException {
        for (Requirement requirement: requirements) {
            if (requirement.getId() == null) throw new BadRequestException("There is a requirement without id.");
            array_ids.add(requirement.getId());
            String text = "";
            if (requirement.getName() != null) text = text.concat(requirement.getName() + ".");
            if ((compare.equals("true")) && (requirement.getText() != null)) text = text.concat(requirement.getText());
            array_text.add(text);
        }
    }



    private Map<String, Map<String, Double>> extractKeywords(List<String> corpus, List<String> ids, Map<String, Integer> corpusFrequency) throws Exception {
        List<List<String>> docs = new ArrayList<List<String>>();
        for (String s : corpus) {
            docs.add(englishAnalyze(s));
        }
        List<List<String>> processed=preProcess(docs);
        return tfIdf(processed,ids,corpusFrequency);

    }

    private Map<String,Map<String, Double>> tfIdf(List<List<String>> docs, List<String> corpus, Map<String, Integer> corpusFrequency) {
        Map<String,Map<String, Double>> tfidfComputed = new HashMap<String,Map<String, Double>>();
        List<Map<String, Integer>> wordBag = new ArrayList<Map<String, Integer>>();
        for (List<String> doc : docs) {
            wordBag.add(tf(doc,corpusFrequency));
        }
        Integer i = 0;
        for (List<String> doc : docs) {
            HashMap<String, Double> aux = new HashMap<String, Double>();
            for (String s : doc) {
                Double idf = idf(docs.size(), corpusFrequency.get(s));
                Integer tf = wordBag.get(i).get(s);
                Double tfidf = idf * tf;
                if (tfidf>=cutoffParameter) aux.put(s, tfidf);
            }
            tfidfComputed.put(corpus.get(i),aux);
            ++i;
        }
        return tfidfComputed;
    }

    private double idf(Integer size, Integer frequency) {
        return Math.log(size / frequency+1);
    }


    private List<List<String>> preProcess(List<List<String>> corpus) throws Exception {
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
        List<String> result = new ArrayList<String>();
        TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text));
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            result.add(attr.toString());
        }
        Integer i = 0;
        return result;
    }

    private Map<String, Integer> tf(List<String> doc, Map<String, Integer> corpusFrequency) {
        Map<String, Integer> frequency = new HashMap<String, Integer>();
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

    public double cosine(Map<String, Map<String, Double>> res, String a, String b) {
        double cosine=0.0;
        Map<String,Double> wordsA=res.get(a);
        Map<String,Double> wordsB=res.get(b);
        System.out.println(wordsA.keySet());
        System.out.println(wordsB.keySet());
        Set<String> intersection= new HashSet<String>(wordsA.keySet());
        intersection.retainAll(wordsB.keySet());
        for (String s: intersection) {
            Double forA=wordsA.get(s);
            Double forB=wordsB.get(s);
            cosine+=forA*forB;
        }
        Double normA=norm(wordsA);
        Double normB=norm(wordsB);

        cosine=cosine/(normA*normB);
        return cosine;
    }

    public Double norm(Map<String, Double> wordsB) {
        double norm=0.0;
        for (String s:wordsB.keySet()) {
            norm+=wordsB.get(s)*wordsB.get(s);
        }
        return sqrt(norm);
    }
}
