package upc.similarity.similaritydetectionapi.exception;

public class ComponentException extends Exception {

    private int status;
    private String error;

    public ComponentException(String message, int status, String error) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}
