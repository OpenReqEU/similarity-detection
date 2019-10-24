package upc.similarity.compareapi.entity.exception;

public class LockedOrganizationException extends ComponentException {

    public LockedOrganizationException(String message) {
        super(message,423,"Locked");
    }
}
