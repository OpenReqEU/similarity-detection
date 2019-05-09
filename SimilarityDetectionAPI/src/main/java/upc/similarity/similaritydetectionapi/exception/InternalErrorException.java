package upc.similarity.similaritydetectionapi.exception;

public class InternalErrorException extends ComponentException {

    public InternalErrorException(String message) {
        super(message);
        this.status = 500;
        this.error = "Internal Error";
    }
}
