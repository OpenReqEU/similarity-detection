package upc.similarity.similaritydetectionapi.exception;

public class BadRequestException extends ComponentException {

    public BadRequestException(String message) {
        super(message);
        this.status = 400;
        this.error = "Bad request";
    }
}
