package upc.similarity.similaritydetectionapi.exception;

public class ComponentException extends Exception {

    protected int status;
    protected String error;

    public ComponentException(String message) {
        super(message);
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}
