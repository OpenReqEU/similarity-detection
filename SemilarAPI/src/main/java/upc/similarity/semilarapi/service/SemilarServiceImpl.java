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
import upc.similarity.semilarapi.exception.InternalErrorException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static java.lang.StrictMath.sqrt;

@Service("semilarService")
public class SemilarServiceImpl implements SemilarService {

    private static Double cutoffParameter=1.0;
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
    public void buildModel(String compare, String organization, List<Requirement> reqs) throws BadRequestException, InternalErrorException {
        show_time("start");
        System.out.println(reqs.size());
        saveModel(organization,generateModel(compare,reqs));
        show_time("finish");
    }

    @Override
    public void buildModelAndCompute(String filename, String compare, String organization, double threshold, List<Requirement> reqs) throws BadRequestException, InternalErrorException {
        show_time("start initialization");
        Model model = generateModel(compare,reqs);
        saveModel(organization,model);
        show_time("finish initialization");
        List<String> idReqs = new ArrayList<>();
        for (Requirement requirement: reqs) {
            idReqs.add(requirement.getId());
        }
        reqs = null;
        simProject(filename,organization,threshold,idReqs);
    }


    @Override
    public void simReqReq(String filename, String organization, String req1, String req2) throws BadRequestException, InternalErrorException {
        show_time("start");
        Model model = loadModel(organization);
        if (!model.getDocs().containsKey(req1)) throw new BadRequestException("The requirement with id \"" + req1 + "\" is not present in the model loaded form the database");
        if (!model.getDocs().containsKey(req2)) throw new BadRequestException("The requirement with id \"" + req2 + "\" is not present in the model loaded form the database");
        double score = cosine(model.getDocs(),req1,req2);
        Dependency result = new Dependency(score,req1,req2,status,dependency_type,component);

        Path p = generateFile(filename);
        String s = System.lineSeparator() + result.print_json() + System.lineSeparator() + "]}";
        write_to_file(s,p);

        show_time("finish");
    }

    @Override
    public void simReqProject(String filename, String organization, String req, double threshold, List<String> project_reqs) throws BadRequestException, InternalErrorException {
        show_time("start computing");
        Model model = loadModel(organization);
        if (!model.getDocs().containsKey(req)) throw new BadRequestException("The requirement with id \"" + req + "\" is not present in the model loaded form the database");

        Path p = generateFile(filename);

        boolean firsttimeComa = true;
        int cont = 0;
        String result = "";
        for (String req2: project_reqs) {
            if (!req.equals(req2) && model.getDocs().containsKey(req2)) {
                double score = cosine(model.getDocs(),req,req2);
                if (score >= threshold) {
                    Dependency dependency = new Dependency(score, req, req2, status, dependency_type, component);
                    String s = System.lineSeparator() + dependency.print_json();
                    if (!firsttimeComa) s = "," + s;
                    firsttimeComa = false;
                    result = result.concat(s);
                    ++cont;
                    if (cont >= 5000) {
                        write_to_file(result, p);
                        result = "";
                        cont = 0;
                    }
                }
            }
        }

        if (!result.equals("")) write_to_file(result,p);
        String s = System.lineSeparator() + "]}";
        write_to_file(s,p);

        show_time("finish computing");
    }

    @Override
    public void simProject(String filename, String organization, double threshold, List<String> project_reqs) throws BadRequestException, InternalErrorException {
        show_time("start computing");
        Model model = loadModel(organization);

        Path p = generateFile(filename);

        boolean firsttimeComa = true;
        int cont = 0;
        String result = "";

        for (int i = 0; i < project_reqs.size(); ++i) {
            System.out.println(project_reqs.size() - i);
            String req1 = project_reqs.get(i);
            if (model.getDocs().containsKey(req1)) {
                for (int j = i + 1; j < project_reqs.size(); ++j) {
                    String req2 = project_reqs.get(j);
                    if (!req2.equals(req1) && model.getDocs().containsKey(req2)) {
                        double score = cosine(model.getDocs(), req1, req2);
                        if (score >= threshold) {
                            Dependency dependency = new Dependency(score, req1, req2, status, dependency_type, component);
                            String s = System.lineSeparator() + dependency.print_json();
                            if (!firsttimeComa) s = "," + s;
                            firsttimeComa = false;
                            result = result.concat(s);
                            ++cont;
                        }
                    }
                }
                if (cont >= 5000) {
                    write_to_file(result, p);
                    result = "";
                    cont = 0;
                }
            }
        }
        if (!result.equals("")) write_to_file(result,p);
        String s = System.lineSeparator() + "]}";
        write_to_file(s,p);

        show_time("finish computing");
    }

    @Override
    public void clearDB(String organization) throws InternalErrorException {
        try {
            modelDAO.clearDB(organization);
        } catch (SQLException e) {
            throw new InternalErrorException("Error while clearing the database");
        }
    }



    /*
    auxiliary operations
     */

    private Path generateFile(String filename) throws InternalErrorException {
        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";
        write_to_file(s,p);
        return p;
    }

    private Model loadModel(String organization) throws BadRequestException, InternalErrorException {
        try {
            return modelDAO.getModel(organization);
        } catch (SQLException e) {
            throw new InternalErrorException("Error while loading the model from the database");
        }
    }

    private Model generateModel(String compare, List<Requirement> reqs) throws BadRequestException, InternalErrorException {
        Map<String, Integer> corpusFrequency = new HashMap<>();
        List<String> text = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        buildCorpus(compare,reqs,text,ids);
        Map<String, Map<String, Double>> docs = extractKeywords(text,ids,corpusFrequency);
        return new Model(docs,corpusFrequency);
    }

    private void saveModel(String organization, Model model) throws InternalErrorException {
        try {
            modelDAO.saveModel(organization, model);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new InternalErrorException("Error while saving the new model to the database");
        }
    }

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
            if (requirement.getName() != null) text = text.concat(clean_text(requirement.getName(),1) + ". ");
            if ((compare.equals("true")) && (requirement.getText() != null)) text = text.concat(clean_text(requirement.getText(),2));
            array_text.add(text);
        }
    }

    private String clean_text(String text, int clean) {
        text = text.replaceAll("_", "");
        if (clean == 0) return text;
        else if (clean == 1) {

            String[] aux = text.split(" ");
            text = "";
            for (String word: aux) {
                if (word.equals(word.toUpperCase())) word = word.toLowerCase();
                text = text.concat(" " + word);
            }

            //text = text.replace("n't", "");

            text = text.replace("..", ".");
            text = text.replace(":", " ");
            text = text.replace("\"", "");
            text = text.replace("\\r", ".");
            text = text.replace("..", ".");
            text = text.replace("\\n", ".");
            text = text.replace("..", ".");
            text = text.replace("!", ".");
            text = text.replace("..", ".");
            text = text.replace("?", ".");
            text = text.replace("..", ".");
            text = text.replace("=", " ");
            text = text.replace(">", " ");
            text = text.replace("<", " ");
            text = text.replace("%", " ");
            text = text.replace("#", " ");
            text = text.replace(",", " ");
            text = text.replace("(", " ");
            text = text.replace(")", " ");
            text = text.replace("{", " ");
            text = text.replace("}", " ");
            text = text.replace("-", " ");

            String result = "";
            String[] r = text.split("(?=\\p{Upper})");
            for (String word: r) result = result.concat(" " + word);


            return result;
        }
        else {
            text = text.replaceAll("(\\{code.*?\\{code)","");

            String[] aux = text.split(" ");
            text = "";
            for (String word: aux) {
                if (word.equals(word.toUpperCase())) word = word.toLowerCase();
                text = text.concat(" " + word);
            }

            text = text.replace("..", ".");
            text = text.replace("\"", "");
            text = text.replace(":", " ");
            text = text.replace("\\r", ".");
            text = text.replace("..", ".");
            text = text.replace("\\n", ".");
            text = text.replace("..", ".");
            text = text.replace("!", ".");
            text = text.replace("..", ".");
            text = text.replace("?", ".");
            text = text.replace("..", ".");
            text = text.replace("=", " ");
            text = text.replace("%", " ");
            text = text.replace("#", " ");
            text = text.replace(",", " ");
            text = text.replace("(", " ");
            text = text.replace(")", " ");
            text = text.replace("{", " ");
            text = text.replace("}", " ");
            text = text.replace("-", " ");
            text = text.replace(">", " ");
            text = text.replace("<", " ");

            String result = "";
            String[] r = text.split("(?=\\p{Upper})");
            for (String word: r) result = result.concat(" " + word);

            text = "";
            for (String word: result.split(" ")) {
                if (word.length() < 20) text = text.concat(" " + word);
            }
            return result;
        }
    }



    private Map<String, Map<String, Double>> extractKeywords(List<String> corpus, List<String> ids, Map<String, Integer> corpusFrequency) throws InternalErrorException {
        List<List<String>> docs = new ArrayList<>();
        for (String s : corpus) {
            try {
                docs.add(englishAnalyze(s));
            } catch (IOException e) {
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
        for (List<String> doc : docs) {
            HashMap<String, Double> aux = new HashMap<>();
            for (String s : doc) {
                Double idf = idf(docs.size(), corpusFrequency.get(s));
                Integer tf = wordBag.get(i).get(s);
                double tfidf = idf * tf;
                if (tfidf>=cutoffParameter) aux.put(s, tfidf);
            }
            tfidfComputed.put(corpus.get(i),aux);
            ++i;
        }
        return tfidfComputed;
    }

    private double idf(int size, int frequency) {
        return Math.log(size / frequency+1);
    }


    private List<List<String>> preProcess(List<List<String>> corpus) {
        return corpus;
    }

    private List<String> englishAnalyze(String text) throws IOException {
        Analyzer analyzer = CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("lowercase")
                //.addTokenFilter("commongrams")
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

    public Double norm(Map<String, Double> wordsB) {
        double norm=0.0;
        for (String s:wordsB.keySet()) {
            double value = wordsB.get(s);
            norm+=value*value;
        }
        return sqrt(norm);
    }

    private void write_to_file(String text, Path p) throws InternalErrorException {
        try (BufferedWriter writer = Files.newBufferedWriter(p, StandardOpenOption.APPEND)) {
            writer.write(text);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new InternalErrorException("Write start to file fail");
        }
    }
}
