package upc.similarity.similaritydetectionapi.exception;

public class BadRequestException extends ComponentException {

    public BadRequestException(String message) {
        super(message,400,"Bad request");
    }
}
