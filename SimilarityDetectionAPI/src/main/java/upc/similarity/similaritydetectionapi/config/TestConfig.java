package upc.similarity.similaritydetectionapi.config;

import org.json.JSONObject;

public class TestConfig {

    private static TestConfig instance = new TestConfig();
    private boolean computationFinished;
    private JSONObject result;

    private TestConfig() {
        computationFinished = false;
    }

    public static TestConfig getInstance() {
        return instance;
    }

    public boolean isComputationFinished() {
        return computationFinished;
    }

    public JSONObject getResult() {
        return result;
    }

    public void setComputationFinished(boolean computationFinished) {
        this.computationFinished = computationFinished;
    }

    public void setResult(JSONObject result) {
        this.result = result;
    }
}
