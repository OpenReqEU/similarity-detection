package upc.similarity.compareapi.entity.auxiliary;

import org.json.JSONArray;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.dao.DatabaseOperations;

public class DiskDependencies extends ResponseDependencies {

    private JSONArray dependencies;
    private int currentPageDependencies;
    private long totalNumDependencies;
    private int maxDepsForPage;
    private DatabaseOperations databaseOperations;
    private String dependenciesArrayName;

    public DiskDependencies(String organization, String responseId) {
        super(organization,responseId);
        this.dependencies = new JSONArray();
        this.currentPageDependencies = 0;
        this.totalNumDependencies = 0;
        this.maxDepsForPage = Constants.getInstance().getMaxDepsForPage();
        this.databaseOperations = DatabaseOperations.getInstance();
        this.dependenciesArrayName = Constants.getInstance().getDependenciesArrayName();
    }

    public void addDependency(Dependency dependency) throws InternalErrorException {
        dependencies.put(dependency.toJSON());
        ++currentPageDependencies;
        if (currentPageDependencies >= maxDepsForPage) {
            databaseOperations.generateResponsePage(responseId, organization, dependencies, dependenciesArrayName);
            dependencies = new JSONArray();
            currentPageDependencies = 0;
        }
    }

    public void finish() throws InternalErrorException {
        if (dependencies.length() > 0) {
            databaseOperations.generateResponsePage(responseId, organization, dependencies, dependenciesArrayName);
        } else if (totalNumDependencies == 0) {
            databaseOperations.generateResponsePage(responseId, organization, dependencies, dependenciesArrayName);
        }
    }
}
