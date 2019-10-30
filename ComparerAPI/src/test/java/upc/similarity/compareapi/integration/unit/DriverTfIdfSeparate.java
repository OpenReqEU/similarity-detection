package upc.similarity.compareapi.integration.unit;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.preprocess.PreprocessPipelineDefault;
import upc.similarity.compareapi.preprocess.PreprocessPipelineSeparate;
import upc.similarity.compareapi.similarity_algorithm.tf_idf.SimilarityAlgorithmTfIdf;
import upc.similarity.compareapi.similarity_algorithm.tf_idf.SimilarityModelTfIdf;
import upc.similarity.compareapi.similarity_algorithm.tf_idf_double.CosineSimilarityTfIdfDouble;
import upc.similarity.compareapi.similarity_algorithm.tf_idf_separate.CosineSimilarityTfIdfSeparate;
import upc.similarity.compareapi.similarity_algorithm.tf_idf_separate.SimilarityAlgorithmTfIdfSeparate;
import upc.similarity.compareapi.similarity_algorithm.tf_idf_separate.SimilarityModelTfIdfSeparate;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class DriverTfIdfSeparate {

    private static int numberIter = 100;
    private static JSONArray duplicateDependencies;
    private static JSONArray notDuplicateDependencies;
    private static JSONArray duplicateRequirements;
    private static JSONArray notDuplicateRequirements;
    private static SimilarityModelTfIdfSeparate modelTfIdf;

    private static class State {
        double cut_off_topics;
        double importance_low;
        HashSet<String> alreadyTested;
        double score;
        //int cutOffValue;
        double threshold;
        State(){}
        State(double cut_off_topics, double importance_low/*, int cutOffValue*/, double threshold) {
            this.cut_off_topics = cut_off_topics;
            this.importance_low = importance_low;
            //this.cutOffValue = cutOffValue;
            this.threshold = threshold;
        }
        State(State state) {
            this.cut_off_topics = state.cut_off_topics;
            this.importance_low = state.importance_low;
            //this.cutOffValue = state.cutOffValue;
            this.threshold = state.threshold;
            this.score = state.score;
            this.alreadyTested = state.alreadyTested;
        }
        String print() {
            return "score -> " + score + "; cut_off_topics -> " + cut_off_topics + "; importance_low -> " + importance_low/* + "; cut_off_value -> " + cutOffValue*/ + "; threshold -> " + threshold;
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
            duplicateRequirements = duplicatesDataset.getJSONArray("requirements");
            notDuplicateRequirements = notDuplicatesDataset.getJSONArray("requirements");
            duplicateDependencies = duplicatesDataset.getJSONArray("dependencies");
            notDuplicateDependencies = notDuplicatesDataset.getJSONArray("dependencies");
            modelTfIdf = createModel(0.1);

            System.out.println("Finished initialization");

            System.out.println("Started computation");
            State bestState = new State();
            bestState.score = 0;

            for (int i = 0; i < numberIter; ++i) {
                System.out.println("Global iteration -> " + i);
                State state = new State();
                state.cut_off_topics = random.nextDouble();
                state.importance_low = random.nextDouble();
                state.threshold = random.nextDouble();
                //state.cutOffValue = random.nextInt(10);
                HashSet<String> alreadyTested = new HashSet<>();
                alreadyTested.add(create_unique_map_id(state));
                state.alreadyTested = alreadyTested;
                state.score = getAccuracy(state);

                System.out.println("Start state: " + state.print());

                boolean changed = true;
                int cont = 0;
                double last_score = state.score;
                while (changed && (cont < 100) ) {
                    System.out.println("Iter number: " + cont);
                    changed = testNeighbours(state);
                    if (changed) {
                        if (state.score < last_score) System.out.println("ERRORRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR");
                        last_score = state.score;
                    }
                    ++cont;
                }
                System.out.println("Result state: " + state.print());
                if (state.score > bestState.score) bestState = state;
            }
            System.out.println("Finished computation");
            System.out.println("Result state: " + bestState.print());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String create_unique_map_id(State state) {
        return state.cut_off_topics+""+state.importance_low/*+""+state.cutOffValue*/+""+state.threshold;
    }

    private static State testState(State currentState, State maxState) throws Exception {
        double auxScore = getAccuracy(currentState);
        if (auxScore > maxState.score) {
            currentState.score = auxScore;
            return currentState;
        } else return maxState;
    }

    private static boolean testNeighbours(State initialState) throws Exception {
        State maxState = new State(initialState);
        HashSet<String> alreadyTested = initialState.alreadyTested;

        State auxState = new State(initialState);
        auxState.cut_off_topics += 0.05;
        String id = create_unique_map_id(auxState);
        if (!alreadyTested.contains(id) && (auxState.cut_off_topics <= 1)) {
            alreadyTested.add(id);
            maxState = testState(auxState,maxState);
        }
        auxState = new State(initialState);
        auxState.cut_off_topics -= 0.05;
        id = create_unique_map_id(auxState);
        if (!alreadyTested.contains(id) && (auxState.cut_off_topics >= 0)) {
            alreadyTested.add(id);
            maxState = testState(auxState,maxState);
        }
        auxState = new State(initialState);
        auxState.importance_low += 0.05;
        id = create_unique_map_id(auxState);
        if (!alreadyTested.contains(id) && (auxState.importance_low <= 1)) {
            alreadyTested.add(id);
            maxState = testState(auxState,maxState);
        }
        auxState = new State(initialState);
        auxState.importance_low -= 0.05;
        id = create_unique_map_id(auxState);
        if (!alreadyTested.contains(id) && (auxState.importance_low >= 0)) {
            alreadyTested.add(id);
            maxState = testState(auxState,maxState);
        }
        /*auxState = new State(initialState);
        auxState.cutOffValue += 1;
        id = create_unique_map_id(auxState);
        if (!alreadyTested.contains(id) && (auxState.cutOffValue <= 20)) {
            alreadyTested.add(id);
            maxState = testState(auxState,maxState);
        }
        auxState = new State(initialState);
        auxState.cutOffValue -= 1;
        id = create_unique_map_id(auxState);
        if (!alreadyTested.contains(id) && (auxState.cutOffValue >= -1)) {
            alreadyTested.add(id);
            maxState = testState(auxState,maxState);
        }*/
        auxState = new State(initialState);
        auxState.threshold += 0.05;
        id = create_unique_map_id(auxState);
        if (!alreadyTested.contains(id) && (auxState.threshold <= 1)) {
            alreadyTested.add(id);
            maxState = testState(auxState,maxState);
        }
        auxState = new State(initialState);
        auxState.threshold -= 0.05;
        id = create_unique_map_id(auxState);
        if (!alreadyTested.contains(id) && (auxState.threshold >= 0)) {
            alreadyTested.add(id);
            maxState = testState(auxState,maxState);
        }
        return  (maxState != initialState);
    }

    private static double getAccuracy(State state) throws Exception {
        //SimilarityModelTfIdf modelTfIdf = createModel(state.cutOffValue);
        CosineSimilarityTfIdfSeparate cosineSimilarityTfIdfSeparate = new CosineSimilarityTfIdfSeparate(state.cut_off_topics,state.importance_low);
        int truePositive = 0;
        int falsePositive = 0;
        for (int i = 0; i < duplicateDependencies.length(); ++i) {
            JSONObject aux = duplicateDependencies.getJSONObject(i);
            String fromid = aux.getString("fromid");
            String toid = aux.getString("toid");
            double score = cosineSimilarityTfIdfSeparate.compute(modelTfIdf,fromid,toid);
            if (score >= state.threshold) ++truePositive;
        }
        for (int i = 0; i < notDuplicateDependencies.length(); ++i) {
            JSONObject aux = notDuplicateDependencies.getJSONObject(i);
            String fromid = aux.getString("fromid");
            String toid = aux.getString("toid");
            double score = cosineSimilarityTfIdfSeparate.compute(modelTfIdf,fromid,toid);
            if (score >= state.threshold) ++falsePositive;
        }
        double result = (double)truePositive/duplicateDependencies.length() - (double)falsePositive/notDuplicateDependencies.length();
        if (result < 0) result = 0;
        return result*100;
    }

    private static SimilarityModelTfIdfSeparate createModel(double cutOffValue) {
        try {
            List<Requirement> requirements = new ArrayList<>();
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
            PreprocessPipelineSeparate preprocessPipelineSeparate = new PreprocessPipelineSeparate();
            SimilarityAlgorithmTfIdfSeparate similarityAlgorithmTfIdfSeparate = new SimilarityAlgorithmTfIdfSeparate(cutOffValue, false, false,0,0);
            return similarityAlgorithmTfIdfSeparate.buildModel(preprocessPipelineSeparate.preprocessRequirements(true, requirements));
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
