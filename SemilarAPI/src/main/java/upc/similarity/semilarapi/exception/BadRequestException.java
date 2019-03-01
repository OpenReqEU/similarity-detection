package upc.similarity.semilarapi.exception;

public class BadRequestException extends Exception {

    private int status;
    private String error;
    private String message;

    public BadRequestException(String message) {
        this.status = 412;
        this.error = "Bad Request";
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
