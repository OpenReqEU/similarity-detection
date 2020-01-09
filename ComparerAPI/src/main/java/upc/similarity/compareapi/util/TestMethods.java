package upc.similarity.compareapi.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import upc.similarity.compareapi.algorithms.preprocess.PreprocessPipeline;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.exception.InternalErrorException;
import upc.similarity.compareapi.entity.input.Clusters;
import upc.similarity.compareapi.service.RequirementsSimilarity;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class TestMethods {

    private static TestMethods instance = new TestMethods();
    private static Logger logger = Logger.getInstance();
    private static PreprocessPipeline preprocessPipeline = Constants.getInstance().getPreprocessPipeline();
    private static RequirementsSimilarity requirementsSimilarity = Constants.getInstance().getRequirementsSimilarity();

    private TestMethods(){}

    public static TestMethods getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        try {
            String path = "/home/ferran/Documents/trabajo/DOCS_Qt/not_duplicates.json";
            JSONObject jsonObject = new JSONObject(readFile(path));
            testAccuracy(jsonToClusters(jsonObject));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static String readFile(String filename) throws Exception {
        Path path = Paths.get(filename);
        byte[] fileBytes = Files.readAllBytes(path);
        return new String(fileBytes);
    }

    private static Clusters jsonToClusters(JSONObject jsonObject) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonObject.toString(), Clusters.class);
    }

    private static void testAccuracy(Clusters input) throws Exception {
        logger.showInfoMessage("Start testMethod");
        Path p = Paths.get("../auxiliary_files/result_accuracy.txt");
        Files.deleteIfExists(p);
        Files.createFile(p);
        List<Requirement> notDuplicates = deleteDuplicates(input.getRequirements());

        long start = System.nanoTime();
        OrganizationModels organizationModels = generateModel(true, true, notDuplicates);
        long finish = System.nanoTime();
        long timeElapsed = finish - start;
        logger.showInfoMessage("BuildModel time: " + timeElapsed);

        organizationModels = new OrganizationModels(organizationModels,0,true,true,false);

        String output = "";
        start = System.nanoTime();
        for (Dependency dependency : input.getDependencies()) {
            String fromid = dependency.getFromid();
            String toid = dependency.getToid();
            double value = requirementsSimilarity.computeSimilarity(organizationModels, fromid, toid);
            output = output.concat(value + System.lineSeparator());
        }
        finish = System.nanoTime();
        timeElapsed = finish - start;
        logger.showInfoMessage("Computation time: " + timeElapsed);
        try (BufferedWriter writer = Files.newBufferedWriter(p)) {
            writer.write(output);
        }
        logger.showInfoMessage("Finish testMethod");
    }

    private static OrganizationModels generateModel(boolean compare, boolean useComponent, List<Requirement> requirements) throws InternalErrorException {
        return requirementsSimilarity.buildModel(preprocessPipeline.preprocessRequirements(compare,requirements),requirements,useComponent);
    }

    private static List<Requirement> deleteDuplicates(List<Requirement> requirements) {
        HashSet<String> ids = new HashSet<>();
        List<Requirement> result = new ArrayList<>();
        for (Requirement requirement : requirements) {
            String id = requirement.getId();
            if (id != null && !ids.contains(requirement.getId())) {
                result.add(requirement);
                ids.add(requirement.getId());
            }
        }
        return result;
    }


}