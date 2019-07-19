package upc.similarity.similaritydetectionapi.entity.input_output;

public abstract class Input {

    protected String message;

    public abstract boolean inputOk();

    public String getMessage() {
        return message;
    }
}
