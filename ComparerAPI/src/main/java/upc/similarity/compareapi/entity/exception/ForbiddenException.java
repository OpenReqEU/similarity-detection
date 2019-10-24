package upc.similarity.compareapi.entity.exception;

public class ForbiddenException extends ComponentException {

    public ForbiddenException(String message) {
        super(message,403,"Forbidden");
    }
}
