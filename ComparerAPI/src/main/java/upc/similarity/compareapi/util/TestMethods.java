package upc.similarity.compareapi.util;

import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.exception.BadRequestException;
import upc.similarity.compareapi.entity.exception.ComponentException;
import upc.similarity.compareapi.entity.exception.InternalErrorException;
import upc.similarity.compareapi.entity.input.Clusters;
import upc.similarity.compareapi.similarity_algorithm.SimilarityAlgorithm;
import upc.similarity.compareapi.similarity_algorithm.SimilarityModel;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class TestMethods {

    private static TestMethods instance = new TestMethods();
    private Constants constants = Constants.getInstance();
    private Logger logger = Logger.getInstance();

    private TestMethods(){}

    public static TestMethods getInstance() {
        return instance;
    }

    public void testAccuracy(Clusters input) {
        SimilarityAlgorithm similarityAlgorithm = Constants.getInstance().getSimilarityAlgorithm();
        Thread thread = new Thread(() -> {
            logger.showInfoMessage("Start testMethod");
            try {
                Path p = Paths.get("../testing/output/result_accuracy.txt");
                Files.deleteIfExists(p);
                Files.createFile(p);
                SimilarityModel similarityModel = generateModel(true,deleteDuplicates(input.getRequirements()));
                String output = "";
                for (Dependency dependency : input.getDependencies()) {
                    String fromid = dependency.getFromid();
                    String toid = dependency.getToid();
                    double value = similarityAlgorithm.computeSimilarity(similarityModel, fromid, toid);
                    output = output.concat(value + System.lineSeparator());
                }
                try (BufferedWriter writer = Files.newBufferedWriter(p)) {
                    writer.write(output);
                }
                logger.showInfoMessage("Finish testMethod");
            } catch (Exception e) {
                logger.showErrorMessage(e.getMessage());
            }        });
        thread.start();
    }

    private List<Requirement> deleteDuplicates(List<Requirement> requirements) throws ComponentException {
        HashSet<String> ids = new HashSet<>();
        List<Requirement> result = new ArrayList<>();
        for (Requirement requirement : requirements) {
            if (requirement.getId() == null) throw new BadRequestException("There is a requirement without id.");
            if (!ids.contains(requirement.getId())) {
                result.add(requirement);
                ids.add(requirement.getId());
            }
        }
        return result;
    }

    private SimilarityModel generateModel(boolean compare, List<Requirement> requirements) throws InternalErrorException {
        return constants.getSimilarityAlgorithm().buildModel(constants.getPreprocessPipeline().preprocessRequirements(compare,requirements));
    }


}