package upc.similarity.similaritydetectionapi.exception;

public class InternalErrorException extends ComponentException {

    public InternalErrorException(String message) {
        super(message,500,"Internal error");
    }
}
