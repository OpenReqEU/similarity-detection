package upc.similarity.compareapi.config;

public class Constants {

    private static Constants instance = new Constants();
    private String component = "Similarity-UPC";
    private String status = "proposed";
    private String dependencyType = "similar";
    private int maxDepsForPage = 20000;
    private String badRequestMessage = "Bad request";
    private String notFoundMessage = "Not found";
    private String notFinishedMessage = "Not finished";
    private String internalErrorMessage = "Internal Error";
    private String forbiddenErrorMessage = "The organization already has a model created. Please use the method called DeleteOrganizationData to delete the organization's model";
    private String sqlErrorMessage = "Database error";
    private String dependenciesArrayName = "dependencies";
    private int sleepTime = 30*60; //seconds

    private Constants(){}

    public static Constants getInstance() {
        return instance;
    }

    public String getComponent() {
        return component;
    }

    public String getStatus() {
        return status;
    }

    public String getDependencyType() {
        return dependencyType;
    }

    public int getMaxDepsForPage() {
        return maxDepsForPage;
    }

    public String getBadRequestMessage() {
        return badRequestMessage;
    }

    public String getNotFoundMessage() {
        return notFoundMessage;
    }

    public String getNotFinishedMessage() {
        return notFinishedMessage;
    }

    public String getInternalErrorMessage() {
        return internalErrorMessage;
    }

    public String getSqlErrorMessage() {
        return sqlErrorMessage;
    }

    public String getForbiddenErrorMessage() {
        return forbiddenErrorMessage;
    }

    public String getDependenciesArrayName() {
        return dependenciesArrayName;
    }

    public int getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }
}
