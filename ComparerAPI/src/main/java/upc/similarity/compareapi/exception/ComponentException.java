package upc.similarity.compareapi.exception;

public class ComponentException extends Exception {

    private final int status;
    private final String error;
    private final String message;

    public ComponentException(String message, int status, String error) {
        super(message);
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
