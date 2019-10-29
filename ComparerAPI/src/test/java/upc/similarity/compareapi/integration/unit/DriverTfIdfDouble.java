package upc.similarity.compareapi.integration.unit;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.preprocess.PreprocessPipelineDefault;
import upc.similarity.compareapi.similarity_algorithm.tf_idf.SimilarityAlgorithmTfIdf;
import upc.similarity.compareapi.similarity_algorithm.tf_idf.SimilarityModelTfIdf;
import upc.similarity.compareapi.similarity_algorithm.tf_idf_double.CosineSimilarityTfIdfDouble;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class DriverTfIdfDouble {

    private static SimilarityModelTfIdf modelTfIdf;
    private static double threshold = 0.13;
    private static int numberIter = 50;
    private static JSONArray duplicateDependencies;
    private static JSONArray notDuplicateDependencies;

    private static class State {
        int topic_threshold;
        double cut_off_topics;
        double importance_low;
        HashSet<String> alreadyTested;
        double score;
        State(){}
        State(int topic_threshold, double cut_off_topics, double importance_low) {
            this.topic_threshold = topic_threshold;
            this.cut_off_topics = cut_off_topics;
            this.importance_low = importance_low;
        }
    }

    private static Random random = new Random();

    public static void main(String[] args) {
        try {
            System.out.println("Started initialization");

            String filePathDuplicates = "/home/ferran/Documents/trabajo/DOCS_Qt/final_results/duplicates_1500.xlsx";
            JSONObject duplicatesDataset = readExcelFile(filePathDuplicates, 1437);
            String filePathNotDuplicates = "/home/ferran/Documents/trabajo/DOCS_Qt/final_results/not_duplicates_1500.xlsx";
            JSONObject notDuplicatesDataset = readExcelFile(filePathNotDuplicates, 1500);
            modelTfIdf = createModel(duplicatesDataset, notDuplicatesDataset);
            duplicateDependencies = duplicatesDataset.getJSONArray("dependencies");
            notDuplicateDependencies = notDuplicatesDataset.getJSONArray("dependencies");

            System.out.println("Finished initialization");

            System.out.println("Started computation");
            State bestState = new State();
            bestState.score = 0;

            for (int i = 0; i < numberIter; ++i) {
                State state = new State();
                state.topic_threshold = random.nextInt(30);
                state.cut_off_topics = random.nextDouble();
                state.importance_low = random.nextDouble();
                HashSet<String> alreadyTested = new HashSet<>();
                alreadyTested.add(state.topic_threshold + "" + state.cut_off_topics + "" + state.importance_low);
                state.alreadyTested = alreadyTested;
                state.score = getAccuracy(state);

                System.out.println("Start state: topic_threshold -> " + state.topic_threshold + "; cut_off_topics -> " + state.cut_off_topics + "; importance_low -> " + state.importance_low);

                boolean changed = true;
                int cont = 0;
                while (changed) {
                    System.out.println("Iter number: " + cont);
                    changed = testNeighbours(state);
                    ++cont;
                }
                System.out.println("Result state: score -> " + state.score + ";topic_threshold -> " + state.topic_threshold + "; cut_off_topics -> " + state.cut_off_topics + "; importance_low -> " + state.importance_low);
                if (state.score > bestState.score) bestState = state;
            }
            System.out.println("Finished computation");
            System.out.println("Result state: score -> " + bestState.score + ";topic_threshold -> " + bestState.topic_threshold + "; cut_off_topics -> " + bestState.cut_off_topics + "; importance_low -> " + bestState.importance_low);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean testNeighbours(State state) throws Exception {
        int initial_topic_threshold = state.topic_threshold;
        double initial_cut_off_topics = state.cut_off_topics;
        double initial_importance_low = state.importance_low;
        HashSet<String> initial_alreadyTested = state.alreadyTested;
        double initial_score = state.score;

        if (!initial_alreadyTested.contains((initial_topic_threshold+1)+""+initial_cut_off_topics+""+initial_importance_low) && ((initial_topic_threshold+1) <= 30)) {
            initial_alreadyTested.add((initial_topic_threshold+1)+""+initial_cut_off_topics+""+initial_importance_low);
            double aux_score = getAccuracy(new State(initial_topic_threshold + 1, initial_cut_off_topics, initial_importance_low));
            if (aux_score > state.score) {
                state.topic_threshold = initial_topic_threshold + 1;
                state.cut_off_topics = initial_cut_off_topics;
                state.importance_low = initial_importance_low;
                state.score = aux_score;
            }
        }
        if (!initial_alreadyTested.contains((initial_topic_threshold-1)+""+initial_cut_off_topics+""+initial_importance_low) && ((initial_topic_threshold-1) >= 0)) {
            initial_alreadyTested.add((initial_topic_threshold-1) + "" + initial_cut_off_topics + "" + initial_importance_low);
            double aux_score = getAccuracy(new State(initial_topic_threshold - 1, initial_cut_off_topics, initial_importance_low));
            if (aux_score > state.score) {
                state.topic_threshold = initial_topic_threshold - 1;
                state.cut_off_topics = initial_cut_off_topics;
                state.importance_low = initial_importance_low;
                state.score = aux_score;
            }
        }
        if (!initial_alreadyTested.contains(initial_topic_threshold+""+(initial_cut_off_topics+0.05)+""+initial_importance_low) && ((initial_cut_off_topics+0.05) <= 1)) {
            initial_alreadyTested.add(initial_topic_threshold+""+(initial_cut_off_topics+0.05)+""+initial_importance_low);
            double aux_score = getAccuracy(new State(initial_topic_threshold, initial_cut_off_topics + 0.05, initial_importance_low));
            if (aux_score > state.score) {
                state.topic_threshold = initial_topic_threshold;
                state.cut_off_topics = initial_cut_off_topics + 0.05;
                state.importance_low = initial_importance_low;
                state.score = aux_score;
            }
        }
        if (!initial_alreadyTested.contains(initial_topic_threshold+""+(initial_cut_off_topics-0.05)+""+initial_importance_low) && ((initial_cut_off_topics-0.05) >= 0)) {
            initial_alreadyTested.add(initial_topic_threshold + "" + (initial_cut_off_topics-0.05) + "" + initial_importance_low);
            double aux_score = getAccuracy(new State(initial_topic_threshold, initial_cut_off_topics - 0.05, initial_importance_low));
            if (aux_score > state.score) {
                state.topic_threshold = initial_topic_threshold;
                state.cut_off_topics = initial_cut_off_topics - 0.05;
                state.importance_low = initial_importance_low;
                state.score = aux_score;
            }
        }
        if (!initial_alreadyTested.contains(initial_topic_threshold+""+initial_cut_off_topics+""+(initial_importance_low+0.05)) && ((initial_importance_low+0.05) <= 1)) {
            initial_alreadyTested.add(initial_topic_threshold+""+initial_cut_off_topics+""+(initial_importance_low+0.05));
            double aux_score = getAccuracy(new State(initial_topic_threshold, initial_cut_off_topics, initial_importance_low + 0.05));
            if (aux_score > state.score) {
                state.topic_threshold = initial_topic_threshold;
                state.cut_off_topics = initial_cut_off_topics;
                state.importance_low = initial_importance_low + 0.05;
                state.score = aux_score;
            }
        }
        if (!initial_alreadyTested.contains(initial_topic_threshold+""+initial_cut_off_topics+""+(initial_importance_low-0.05)) && ((initial_importance_low-0.05) >= 0)) {
            initial_alreadyTested.add(initial_topic_threshold + "" + initial_cut_off_topics + "" + (initial_importance_low - 0.05));
            double aux_score = getAccuracy(new State(initial_topic_threshold, initial_cut_off_topics, initial_importance_low - 0.05));
            if (aux_score > state.score) {
                state.topic_threshold = initial_topic_threshold;
                state.cut_off_topics = initial_cut_off_topics;
                state.importance_low = initial_importance_low - 0.05;
                state.score = aux_score;
            }
        }
        return  (initial_score != state.score);
    }

    private static double getAccuracy(State state) throws Exception {
        CosineSimilarityTfIdfDouble cosineSimilarityTfIdfDouble = new CosineSimilarityTfIdfDouble(state.topic_threshold,state.cut_off_topics,state.importance_low);
        int truePositive = 0;
        int falsePositive = 0;
        for (int i = 0; i < duplicateDependencies.length(); ++i) {
            JSONObject aux = duplicateDependencies.getJSONObject(i);
            String fromid = aux.getString("fromid");
            String toid = aux.getString("toid");
            double score = cosineSimilarityTfIdfDouble.compute(modelTfIdf,fromid,toid);
            if (score >= threshold) ++truePositive;
        }
        for (int i = 0; i < notDuplicateDependencies.length(); ++i) {
            JSONObject aux = notDuplicateDependencies.getJSONObject(i);
            String fromid = aux.getString("fromid");
            String toid = aux.getString("toid");
            double score = cosineSimilarityTfIdfDouble.compute(modelTfIdf,fromid,toid);
            if (score >= threshold) ++falsePositive;
        }
        double result = (double)truePositive/duplicateDependencies.length() - (double)falsePositive/notDuplicateDependencies.length();
        if (result < 0) result = 0;
        return result*100;
    }

    private static SimilarityModelTfIdf createModel(JSONObject duplicatesDataset, JSONObject notDuplicatesDataset) {
        try {
            List<Requirement> requirements = new ArrayList<>();
            JSONArray duplicateRequirements = duplicatesDataset.getJSONArray("requirements");
            JSONArray notDuplicateRequirements = notDuplicatesDataset.getJSONArray("requirements");
            for (int i = 0; i < duplicateRequirements.length(); ++i) {
                JSONObject aux = duplicateRequirements.getJSONObject(i);
                Requirement requirement = new Requirement();
                requirement.setId(aux.getString("id"));
                requirement.setName(aux.getString("name"));
                requirement.setText(aux.getString("text"));
                requirements.add(requirement);
            }
            for (int i = 0; i < notDuplicateRequirements.length(); ++i) {
                JSONObject aux = notDuplicateRequirements.getJSONObject(i);
                Requirement requirement = new Requirement();
                requirement.setId(aux.getString("id"));
                requirement.setName(aux.getString("name"));
                requirement.setText(aux.getString("text"));
                requirements.add(requirement);
            }
            PreprocessPipelineDefault preprocessPipelineDefault = new PreprocessPipelineDefault();
            SimilarityAlgorithmTfIdf similarityAlgorithmTfIdf = new SimilarityAlgorithmTfIdf(0.1, false, false);
            return similarityAlgorithmTfIdf.buildModel(preprocessPipelineDefault.preprocessRequirements(true, requirements));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static JSONObject readExcelFile(String filePath, int number) {
        try {
            File excelFile = new File(filePath);
            FileInputStream fis;
            XSSFWorkbook wb;

            fis = new FileInputStream(excelFile);
            wb = new XSSFWorkbook(fis);
            XSSFSheet sheet = wb.getSheet("Sheet1");
            Row row;
            Cell cell;


            JSONArray pairs = new JSONArray();
            JSONArray reqs = new JSONArray();
            for (int i = 1; i < number; ++i) {
                row = sheet.getRow(i);
                if (row != null) {
                    cell = row.getCell((short) 3);
                    String fromid = cell.getStringCellValue();

                    cell = row.getCell((short) 4);
                    String toid = cell.getStringCellValue();

                    cell = row.getCell((short) 9);
                    String titlefromid = cell.getStringCellValue();

                    cell = row.getCell((short) 10);
                    String descrfromid = "";
                    try {
                        descrfromid = cell.getStringCellValue();
                    } catch (Exception e) {
                        System.out.println("up1");
                    }

                    cell = row.getCell((short) 12);
                    String titletoid = cell.getStringCellValue();

                    cell = row.getCell((short) 13);
                    String descrtoid = "";
                    try {
                        descrtoid = cell.getStringCellValue();
                    } catch (Exception e) {
                        System.out.println("up2");
                    }

                    JSONObject pair = new JSONObject();
                    pair.put("fromid", fromid);
                    pair.put("toid", toid);
                    pair.put("status", "accepted");
                    pair.put("dependency_type", "similar");
                    pairs.put(pair);

                    JSONObject req1 = new JSONObject();
                    JSONObject req2 = new JSONObject();
                    req1.put("id", fromid);
                    req1.put("name", titlefromid);
                    req1.put("text", descrfromid);

                    req2.put("id", toid);
                    req2.put("name", titletoid);
                    req2.put("text", descrtoid);

                    reqs.put(req1);
                    reqs.put(req2);

                }
            }

            JSONObject result = new JSONObject();
            result.put("requirements", reqs);
            result.put("dependencies", pairs);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
