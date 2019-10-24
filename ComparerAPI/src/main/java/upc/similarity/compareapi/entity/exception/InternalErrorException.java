package upc.similarity.compareapi.entity.exception;

public class InternalErrorException extends ComponentException {

    public InternalErrorException(String message) {
        super(message,500,"Internal error");
    }
}
