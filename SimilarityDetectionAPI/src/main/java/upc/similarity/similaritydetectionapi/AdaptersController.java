package upc.similarity.similaritydetectionapi;

import upc.similarity.similaritydetectionapi.adapter.ComponentAdapter;
import upc.similarity.similaritydetectionapi.adapter.CompareAdapter;
import upc.similarity.similaritydetectionapi.exception.InternalErrorException;
import upc.similarity.similaritydetectionapi.values.Component;

public class AdaptersController {

    private static AdaptersController instance;

    private AdaptersController() {
    }

    public static AdaptersController getInstance() {
        if (instance == null) instance = new AdaptersController();
        return instance;
    }

    public ComponentAdapter getAdapter(Component component) throws InternalErrorException {
        switch(component) {
            case Comparer:
                return new CompareAdapter();
            default:
                throw new InternalErrorException("The component " + component + " does not exist.");
        }

    }

}
