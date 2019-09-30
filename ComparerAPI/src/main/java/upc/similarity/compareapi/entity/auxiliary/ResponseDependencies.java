package upc.similarity.compareapi.entity.auxiliary;

import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.exception.InternalErrorException;

public abstract class ResponseDependencies {

    protected String organization;
    protected String responseId;

    public ResponseDependencies(String organization, String responseId) {
        this.organization = organization;
        this.responseId = responseId;
    }

    public abstract void addDependency(Dependency elem) throws InternalErrorException;

    public abstract void finish() throws InternalErrorException;
}
