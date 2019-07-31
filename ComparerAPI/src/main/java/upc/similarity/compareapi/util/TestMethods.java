package upc.similarity.compareapi.util;

import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.compareapi.config.Control;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Model;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.input.Clusters;
import upc.similarity.compareapi.exception.BadRequestException;
import upc.similarity.compareapi.exception.InternalErrorException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class TestMethods {

    private static TestMethods instance = new TestMethods();
    private Control control = Control.getInstance();

    private TestMethods(){}

    public static TestMethods getInstance() {
        return instance;
    }

    public void TestAccuracy(boolean compare, Clusters input) {
        /*CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();
        Model model = null;
        try {
            model = generateModel(compare, deleteDuplicates(input.getRequirements(), null, null));
        } catch (Exception e) {
            control.showInfoMessage(e.getMessage());
        }
        JSONArray scoresArray = new JSONArray();
        for (Dependency dependency: input.getDependencies()) {
            String fromid = dependency.getFromid();
            String toid = dependency.getToid();
            double value = cosineSimilarity.compute(model.getDocs(), fromid, toid);
            scoresArray.put(value);
        }
        JSONObject result = new JSONObject();
        result.put("scores", scoresArray);
        return result.toString();*/

        /*Thread thread = new Thread(() -> {
            Control.getInstance().showInfoMessage("Start testMethod");
            Model model = null;
            try {
                model = generateModel(compare, deleteDuplicates(input.getRequirements(), null, null));
            } catch (Exception e) {
                control.showInfoMessage(e.getMessage());
            }
            Svd svd = new Svd();
            Map<String,Integer> ids = svd.compute(model.getDocs(), model.getCorpusFrequency());
            JSONArray scoresArray = new JSONArray();
            for (Dependency dependency: input.getDependencies()) {
                String fromid = dependency.getFromid();
                String toid = dependency.getToid();
                double value = svd.compare(ids.get(fromid), ids.get(toid));
                scoresArray.put(value);
            }
            JSONObject result = new JSONObject();
            result.put("scores", scoresArray);
            try(BufferedWriter writer = new BufferedWriter(new FileWriter("../testing/output/result.txt"))) {
                writer.write(result.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            Control.getInstance().showInfoMessage("Finish testMethod");
        });
        thread.start();*/
    }

    public String extractModel(boolean compare, String organization, Clusters input) {
        Model model = null;
        try {
            model = generateModel(compare, deleteDuplicates(input.getRequirements(), null, null));
        } catch (Exception e) {
            control.showInfoMessage(e.getMessage());
        }
        JSONArray reqsArray = new JSONArray();
        Iterator it = model.getDocs().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String id = (String) pair.getKey();
            HashMap<String,Double> words = (HashMap<String,Double>) pair.getValue();
            Iterator it2 = words.entrySet().iterator();
            JSONArray wordsArray = new JSONArray();
            while (it2.hasNext()) {
                Map.Entry pair2 = (Map.Entry)it2.next();
                String word = (String) pair2.getKey();
                double value = (double) pair2.getValue();
                JSONObject auxWord = new JSONObject();
                auxWord.put("word", word);
                auxWord.put("tfIdf", value);
                wordsArray.put(auxWord);
                it2.remove();
            }
            it.remove();
            JSONObject auxReq = new JSONObject();
            auxReq.put("id", id);
            auxReq.put("words", wordsArray);
            reqsArray.put(auxReq);
        }
        JSONArray wordsFreq = new JSONArray();
        it = model.getCorpusFrequency().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String word = (String) pair.getKey();
            int value = (int) pair.getValue();
            JSONObject auxWord = new JSONObject();
            auxWord.put("word", word);
            auxWord.put("corpusTf", value);
            wordsFreq.put(auxWord);
            it.remove();
        }
        JSONObject result = new JSONObject();
        result.put("corpus", reqsArray);
        result.put("corpusFrequency", wordsFreq);
        return result.toString();
    }

    private void writeToFile(String fileName, String text) {
        try (FileWriter fw = new FileWriter(fileName, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(text);
            bw.newLine();
        } catch (IOException e) {
            control.showInfoMessage(e.getMessage());
        }
    }

    private Model generateModel(boolean compare, List<Requirement> requirements) throws InternalErrorException {
        Tfidf tfidf = Tfidf.getInstance();
        Map<String, Integer> corpusFrequency = new HashMap<>();
        List<String> text = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        buildCorpus(compare,requirements,text,ids);
        Map<String, Map<String, Double>> docs = tfidf.extractKeywords(text,ids,corpusFrequency);
        return new Model(docs,corpusFrequency,0,true);
    }

    private void buildCorpus(boolean compare, List<Requirement> requirements, List<String> arrayText, List<String> arrayIds) {
        for (Requirement requirement: requirements) {
            arrayIds.add(requirement.getId());
            String text = "";
            if (requirement.getName() != null) text = text.concat(cleanText(requirement.getName()) + ". ");
            if (compare && (requirement.getText() != null)) text = text.concat(cleanText(requirement.getText()));
            arrayText.add(text);
        }
    }

    private String cleanText(String text) {
        text = text.replaceAll("(\\{.*?})", " code ");
        text = text.replaceAll("[.$,;\\\"/:|!?=%,()><_0-9\\-\\[\\]{}']", " ");
        String[] aux2 = text.split(" ");
        String result = "";
        for (String a : aux2) {
            if (a.length() > 1) {
                result = result.concat(" " + a);
            }
        }
        return result;
    }

    private List<Requirement> deleteDuplicates(List<Requirement> requirements, String organization, String responseId) throws BadRequestException, InternalErrorException {
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        HashSet<String> ids = new HashSet<>();
        List<Requirement> result = new ArrayList<>();
        try {
            for (Requirement requirement : requirements) {
                if (requirement.getId() == null) throw new BadRequestException("There is a requirement without id.");
                if (!ids.contains(requirement.getId())) {
                    result.add(requirement);
                    ids.add(requirement.getId());
                }
            }
        } catch (BadRequestException e) {
            if (organization != null && responseId != null) databaseOperations.saveBadRequestException(organization, responseId, e);
            else throw e;
        }
        return result;
    }

}
