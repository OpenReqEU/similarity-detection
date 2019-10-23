package upc.similarity.compareapi.clusters_algorithm;

import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.exception.InternalErrorException;

import java.util.List;

public interface ClustersAlgorithm {

    /**
     *
     * @param requirements
     * @param dependencies
     * @return
     */
    ClustersModel buildModel(List<Requirement> requirements, List<Dependency> dependencies) throws InternalErrorException;

    void updateModel(String organization, OrganizationModels organizationModels) throws InternalErrorException;
}
