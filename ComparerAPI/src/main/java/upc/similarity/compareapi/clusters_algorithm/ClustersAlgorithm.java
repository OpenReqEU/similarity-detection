package upc.similarity.compareapi.clusters_algorithm;

import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.exception.InternalErrorException;

import java.util.List;

public interface ClustersAlgorithm {

    ClustersModel buildModel(List<Requirement> requirements, List<Dependency> dependencies) throws InternalErrorException;

    void updateModel(String organization, OrganizationModels organizationModels) throws InternalErrorException;

    List<Dependency> getReqProposedDependencies(String organization, String requirementId) throws InternalErrorException;

    List<Dependency> getReqAcceptedDependencies(String organization, String requirementId) throws InternalErrorException;

    void startUpdateProcess(String organization, OrganizationModels organizationModels) throws InternalErrorException;

    void finishUpdateProcess(String organization, OrganizationModels organizationModels) throws InternalErrorException;

    void addAcceptedDependencies(String organization, List<Dependency> acceptedDependencies, OrganizationModels organizationModels) throws InternalErrorException;

    void addRejectedDependencies(String organization, List<Dependency> deletedDependencies, OrganizationModels organizationModels) throws InternalErrorException;

    void addRequirementsToClusters(String organization, List<Requirement> addRequirements, OrganizationModels organizationModels) throws InternalErrorException;
}
