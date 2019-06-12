package upc.similarity.similaritydetectionapi;

import upc.similarity.similaritydetectionapi.adapter.ComponentAdapter;
import upc.similarity.similaritydetectionapi.adapter.ComparerAdapter;
import upc.similarity.similaritydetectionapi.exception.InternalErrorException;
import upc.similarity.similaritydetectionapi.values.Component;

import java.util.Arrays;
import java.util.List;

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
                return new ComparerAdapter();
            default:
                throw new InternalErrorException("The component " + component + " does not exist.");
        }

    }

}
