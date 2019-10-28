package upc.similarity.compareapi.clusters_algorithm;

import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.exception.InternalErrorException;

import java.util.List;

public interface ClustersAlgorithm {

    /**
     * Builds a clusters model with the input requirements and dependencies
     * @param requirements a list with the input requirements, they can't be repeated
     * @param dependencies a list with the input dependencies, they can be repeated
     * @return the resulting clusters model
     */
    ClustersModel buildModel(List<Requirement> requirements, List<Dependency> dependencies) throws InternalErrorException;

    /**
     * This method is call after the buildModel method, it is used when the build model operation needs a long and costly running process
     * @param organization the name of the organization
     * @param organizationModels the generated organization models, similarity model and the clusters model generated in the prior buildModel method
     */
    void updateModel(String organization, OrganizationModels organizationModels) throws InternalErrorException;

    /**
     * Returns the proposed dependencies of the input requirement
     * @param organization the name of the organization
     * @param requirementId the input requirement id
     * @return the list with the proposed dependencies
     */
    List<Dependency> getReqProposedDependencies(String organization, String requirementId) throws InternalErrorException;

    /**
     * Returns the accepted dependencies of the input requirement
     * @param organization the name of the organization
     * @param requirementId the input requirement id
     * @return the list with the accepted dependencies
     */
    List<Dependency> getReqAcceptedDependencies(String organization, String requirementId) throws InternalErrorException;

    /**
     * This method is called before doing an update operation in the clusters model
     * @param organization the name of the organization
     * @param organizationModels the generated organization models, similarity model and the clusters model
     */
    void startUpdateProcess(String organization, OrganizationModels organizationModels) throws InternalErrorException;

    /**
     * This method is called after doing an update operation in the clusters model
     * @param organization the name of the organization
     * @param organizationModels the generated organization models, similarity model and the clusters model
     */
    void finishUpdateProcess(String organization, OrganizationModels organizationModels) throws InternalErrorException;

    /**
     * Adds the input accepted dependencies to the clusters model
     * @param organization the name of the organization
     * @param acceptedDependencies the input accepted dependencies, they can be repeated
     * @param organizationModels the generated organization models, similarity model and the clusters model
     */
    void addAcceptedDependencies(String organization, List<Dependency> acceptedDependencies, OrganizationModels organizationModels) throws InternalErrorException;

    /**
     * Adds the input rejected dependencies to the clusters model
     * @param organization the name of the organization
     * @param deletedDependencies the input rejected dependencies, they can be repeated
     * @param organizationModels the generated organization models, similarity model and the clusters model
     */
    void addRejectedDependencies(String organization, List<Dependency> deletedDependencies, OrganizationModels organizationModels) throws InternalErrorException;

    /**
     * Adds the input requirements to the clusters model
     * @param organization the name of the organization
     * @param addRequirements the input requirements, they can't be repeated
     * @param organizationModels the generated organization models, similarity model and the clusters model
     */
    void addRequirementsToClusters(String organization, List<Requirement> addRequirements, OrganizationModels organizationModels) throws InternalErrorException;
}
