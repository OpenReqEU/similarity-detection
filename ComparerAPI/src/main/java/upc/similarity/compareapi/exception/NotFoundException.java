package upc.similarity.compareapi.exception;

public class NotFoundException extends ComponentException {

    public NotFoundException(String message) {
        super(message,404,"Not found");
    }
}