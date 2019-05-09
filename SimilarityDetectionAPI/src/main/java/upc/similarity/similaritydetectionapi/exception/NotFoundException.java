package upc.similarity.similaritydetectionapi.exception;

public class NotFoundException extends ComponentException {

    public NotFoundException(String message) {
        super(message);
        this.status = 404;
        this.error = "Not found";
    }
}
