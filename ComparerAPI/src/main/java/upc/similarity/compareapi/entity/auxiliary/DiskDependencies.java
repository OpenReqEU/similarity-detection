package upc.similarity.compareapi.entity.auxiliary;

import org.json.JSONArray;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.dao.DatabaseModel;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.exception.InternalErrorException;

public class DiskDependencies extends ResponseDependencies {

    private JSONArray dependencies;
    private int currentPageDependencies;
    private long totalNumDependencies;
    private int maxDepsForPage;
    private DatabaseModel databaseOperations;

    public DiskDependencies(String organization, String responseId) {
        super(organization,responseId);
        this.dependencies = new JSONArray();
        this.currentPageDependencies = 0;
        this.totalNumDependencies = 0;
        this.maxDepsForPage = Constants.getInstance().getMaxDepsForPage();
        this.databaseOperations = Constants.getInstance().getDatabaseModel();
    }

    public void addDependency(Dependency dependency) throws InternalErrorException {
        dependencies.put(dependency.toJSON());
        ++currentPageDependencies;
        if (currentPageDependencies >= maxDepsForPage) {
            generateResponsePage(organization, responseId, dependencies, dependenciesArrayName, databaseOperations);
            dependencies = new JSONArray();
            currentPageDependencies = 0;
        }
    }

    public void finish() throws InternalErrorException {
        if (dependencies.length() > 0 || totalNumDependencies == 0) {
            generateResponsePage(organization, responseId, dependencies, dependenciesArrayName, databaseOperations);
        }
    }
}
