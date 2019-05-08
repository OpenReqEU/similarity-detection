package upc.similarity.similaritydetectionapi.entity.input_output;

import org.json.JSONObject;

public class Result_json {

    private String id;
    private String operation;
    private int code;
    private String error;
    private String message;

    public Result_json(String id, String operation) {
        this.id = id;
        this.operation = operation;
    }

    public void setException(int code, String error, String message) {
        this.code = code;
        this.error = error;
        this.message = message;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String toJSON() {
        JSONObject json = new JSONObject();
        json.put("id",id);
        json.put("operation",operation);
        json.put("code",code);
        if (error != null) json.put("error",error);
        if (message != null) json.put("message",message);
        return json.toString();
    }
}
