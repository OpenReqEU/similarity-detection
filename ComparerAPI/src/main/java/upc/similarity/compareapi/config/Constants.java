package upc.similarity.compareapi.config;

import org.json.JSONObject;
import upc.similarity.compareapi.dao.algorithm_models_dao.similarity_algorithm.SimilarityModelDatabase;
import upc.similarity.compareapi.dao.algorithm_models_dao.similarity_algorithm.SimilarityModelDatabaseTfIdf;
import upc.similarity.compareapi.preprocess.PreprocessPipeline;
import upc.similarity.compareapi.preprocess.PreprocessPipelineDefault;
import upc.similarity.compareapi.similarity_algorithm.SimilarityAlgorithm;
import upc.similarity.compareapi.similarity_algorithm.tf_idf.SimilarityAlgorithmTfIdf;
import upc.similarity.compareapi.util.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Constants {

    private static Constants instance = new Constants();
    private Integer maxDepsForPage = null;
    private Integer maxWaitingTime = null;
    private String databasePath = null;
    private SimilarityAlgorithm similarityAlgorithm = null;
    private SimilarityModelDatabase similarityModelDatabase = null;
    private PreprocessPipeline preprocessPipeline = null;

    //TODO delete these unused stuff
    private String component = "Similarity-UPC";
    private String status = "proposed";
    private String dependencyType = "similar";
    private String badRequestMessage = "Bad request";
    private String notFoundMessage = "Not found";
    private String notFinishedMessage = "Not finished";
    private String internalErrorMessage = "Internal Error";
    private String forbiddenErrorMessage = "The organization already has a model created. Please use the method called DeleteOrganizationData to delete the organization's model";
    private String sqlErrorMessage = "Database error";
    private String dependenciesArrayName = "dependencies";

    private Constants(){
        Logger.getInstance().showInfoMessage("Reading configuration file");
        try {
            Path path = Paths.get("../config_files/config.json");
            List<String> lines = Files.readAllLines(path);
            String file = "";
            for (String line : lines) file = file.concat(line);
            JSONObject json = new JSONObject(file);

            String preprocessPipelineAux = json.getString("preprocess_pipeline");
            String similarityAlgorithmAux = json.getString("similarity_algorithm");

            String databasePathAux = json.getString("database_path");
            int maxDepsForPageAux = json.getInt("max_dependencies_page");
            int maxWaitingTimeAux = json.getInt("max_waiting_time_seconds");

            selectPreprocessPipeline(preprocessPipelineAux);
            selectSimilarityAlgorithm(similarityAlgorithmAux);
            this.databasePath = databasePathAux;
            this.maxDepsForPage = maxDepsForPageAux;
            this.maxWaitingTime = maxWaitingTimeAux;

        } catch (Exception e) {
            Logger.getInstance().showErrorMessage("Error while reading config file: " + e.getMessage());
        }
    }

    private void selectSimilarityAlgorithm(String algorithmType) {
        switch (algorithmType) {
            case "tf_idf":
                int cutOff = 10;
                boolean smoothing = true;
                try {
                    Path path = Paths.get("../config_files/config_tfidf.json");
                    List<String> lines = Files.readAllLines(path);
                    String file = "";
                    for (String line : lines) file = file.concat(line);
                    JSONObject jsonObject = new JSONObject(file);
                    cutOff = jsonObject.getInt("cut_off");
                    smoothing = jsonObject.getBoolean("smoothing");
                } catch (Exception e) {
                    Logger.getInstance().showErrorMessage("Error while reading tf_idf config file: " + e.getMessage());
                }
                this.similarityAlgorithm = new SimilarityAlgorithmTfIdf(cutOff,false,smoothing);
                this.similarityModelDatabase = new SimilarityModelDatabaseTfIdf();
                break;
            default:
                Logger.getInstance().showErrorMessage("The similarity algorithm specified in the configuration file does not exist.");
                break;
        }
    }

    private void selectPreprocessPipeline(String algorithmType) {
        switch (algorithmType) {
            case "default":
                this.preprocessPipeline = new PreprocessPipelineDefault();
                break;
            default:
                Logger.getInstance().showErrorMessage("The preprocess pipeline specified in the configuration file does not exist.");
                break;
        }
    }

    public static Constants getInstance() {
        return instance;
    }

    public String getComponent() {
        return component;
    }

    public String getStatus() {
        return status;
    }

    public String getDependencyType() {
        return dependencyType;
    }

    public Integer getMaxDepsForPage() {
        return maxDepsForPage;
    }

    public String getBadRequestMessage() {
        return badRequestMessage;
    }

    public String getNotFoundMessage() {
        return notFoundMessage;
    }

    public String getNotFinishedMessage() {
        return notFinishedMessage;
    }

    public String getInternalErrorMessage() {
        return internalErrorMessage;
    }

    public String getSqlErrorMessage() {
        return sqlErrorMessage;
    }

    public String getForbiddenErrorMessage() {
        return forbiddenErrorMessage;
    }

    public String getDependenciesArrayName() {
        return dependenciesArrayName;
    }

    public Integer getMaxWaitingTime() {
        return maxWaitingTime;
    }

    public void setMaxWaitingTime(Integer maxWaitingTime) {
        this.maxWaitingTime = maxWaitingTime;
    }

    public String getDatabasePath() {
        return databasePath;
    }

    public PreprocessPipeline getPreprocessPipeline() {
        return preprocessPipeline;
    }

    public SimilarityAlgorithm getSimilarityAlgorithm() {
        return similarityAlgorithm;
    }

    public SimilarityModelDatabase getSimilarityModelDatabase() {
        return similarityModelDatabase;
    }

    /*
    Test purpose methods
     */

    public void setSimilarityAlgorithm(SimilarityAlgorithm similarityAlgorithm) {
        this.similarityAlgorithm = similarityAlgorithm;
    }

    public void setSimilarityModelDatabase(SimilarityModelDatabase similarityModelDatabase) {
        this.similarityModelDatabase = similarityModelDatabase;
    }
}
