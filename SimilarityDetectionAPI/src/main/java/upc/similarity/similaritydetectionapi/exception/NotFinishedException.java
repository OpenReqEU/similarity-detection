package upc.similarity.similaritydetectionapi.exception;

public class NotFinishedException extends ComponentException {

    public NotFinishedException(String message) {
        super(message);
        this.status = 423;
        this.error = "Locked";
    }
}
