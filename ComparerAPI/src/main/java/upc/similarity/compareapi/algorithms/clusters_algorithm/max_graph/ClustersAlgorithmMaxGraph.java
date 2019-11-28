package upc.similarity.compareapi.algorithms.clusters_algorithm.max_graph;

import upc.similarity.compareapi.algorithms.clusters_algorithm.ClustersAlgorithm;
import upc.similarity.compareapi.algorithms.clusters_algorithm.ClustersModel;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.dao.algorithm_models_dao.clusters_algorithm.max_graph.ClustersModelDatabaseMaxGraph;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.exception.InternalErrorException;
import upc.similarity.compareapi.entity.exception.NotFoundException;
import upc.similarity.compareapi.algorithms.similarity_algorithm.SimilarityAlgorithm;
import upc.similarity.compareapi.algorithms.similarity_algorithm.SimilarityModel;
import upc.similarity.compareapi.service.RequirementsSimilarity;

import java.util.*;

public class ClustersAlgorithmMaxGraph implements ClustersAlgorithm {

    /**
     * Consists of saving the user feedback as graphs where the nodes are requirements and the edges
     * are accepted or rejected dependencies. It recommends proposed pairs of similar requirements to
     * the user taking into account the tf-idf value of the previous similarity algorithm and the existing
     * clusters (i.e., it returns similar requirements with a single requirement of a cluster, the one
     * having the highest similarity score)
     */

    private ClustersModelDatabaseMaxGraph databaseOperations;

    public ClustersAlgorithmMaxGraph(ClustersModelDatabaseMaxGraph databaseOperations) {
        this.databaseOperations = databaseOperations;
    }

    /**
     *  This method computes the clusters using the existing dependencies. All the requirements that do
     *  not have duplicates relationships with other requirements are considered to be in a cluster
     *  of just one requirement.
     */
    @Override
    public ClustersModel buildModel(List<Requirement> requirements, List<Dependency> dependencies) {
        HashMap<Integer,List<String>> clusters = new HashMap<>();
        HashMap<String, Integer> reqCluster = new HashMap<>();
        int countIds = 0;

        //Initializes reqClusters
        for (Requirement requirement: requirements) {
            reqCluster.put(requirement.getId(),-1);
        }

        //Merges clusters depending on the input accepted dependencies
        countIds = computeDependencies(dependencies, reqCluster, clusters, countIds);

        // create the clusters of the lonely requirements
        for (Map.Entry<String, Integer> entry : reqCluster.entrySet()) {
            String requirementId = entry.getKey();
            int clusterId = entry.getValue();
            if (clusterId == -1) {
                List<String> aux = new ArrayList<>();
                aux.add(requirementId);
                clusters.put(countIds, aux);
                reqCluster.put(requirementId, countIds);
                ++countIds;
            }
        }

        List<Dependency> modelDependencies = new ArrayList<>();

        //Updates the attribute clusterId of each accepted dependency
        for (Dependency dependency: dependencies) {
            if (validDependency(dependency)) {
                String fromid = dependency.getFromid();
                String toid = dependency.getToid();
                String status = dependency.getStatus();
                modelDependencies.add(dependency);
                if (status.equals("accepted") && reqCluster.containsKey(fromid) && reqCluster.containsKey(toid)) {
                    dependency.setClusterId(reqCluster.get(fromid));
                }
            }
        }

        return new ClustersModelMaxGraph(countIds-1,clusters,duplicateDependencies(modelDependencies));
    }

    /**
     * Computes the proposed dependencies. It is done after the buildModel because it needs to access
     * the pertinent organization model in the database. Doing this process now allows the reqClusters method to only do
     * a select in the database.
     */
    @Override
    public void updateModel(String organization, OrganizationModels organizationModels) throws InternalErrorException {
        try {
            SimilarityModel similarityModel = organizationModels.getSimilarityModel();
            ClustersModelMaxGraph clustersModelMaxGraph = (ClustersModelMaxGraph) organizationModels.getClustersModel();
            databaseOperations.createDepsAuxiliaryTable(organization);
            computeProposedDependencies(organization, new HashSet<>(similarityModel.getRequirementsIds()), clustersModelMaxGraph.getClusters().keySet(), organizationModels, true);
            databaseOperations.updateClustersAndDependencies(organization, organizationModels, null, true);
        } catch (ClassCastException e) {
            throw new InternalErrorException("A max_graph method received a model that is not max_graph");
        }
    }

    @Override
    public List<Dependency> getReqProposedDependencies(String organization, String requirementId) throws InternalErrorException {
        return databaseOperations.getReqDependencies(organization, requirementId, "proposed", false);
    }

    @Override
    public List<Dependency> getReqAcceptedDependencies(String organization, String requirementId) throws InternalErrorException {
        return databaseOperations.getReqDependencies(organization, requirementId, "accepted", false);
    }

    /**
     * Prepares the database and the memory structure to start the update process
     */
    @Override
    public void startUpdateProcess(String organization, OrganizationModels organizationModels) throws InternalErrorException {
        try {
            SimilarityModel similarityModel = organizationModels.getSimilarityModel();
            ClustersModelMaxGraph clustersModelMaxGraph = (ClustersModelMaxGraph) organizationModels.getClustersModel();
            clustersModelMaxGraph.startUpdateProcess(new HashSet<>(similarityModel.getRequirementsIds()));
            databaseOperations.createDepsAuxiliaryTable(organization);
        } catch (ClassCastException e) {
            throw new InternalErrorException("A max_graph method received a model that is not max_graph");
        }
    }

    /**
     * Updates the proposed dependencies before finishing the update process
     */
    @Override
    public void finishUpdateProcess(String organization, OrganizationModels organizationModels) throws InternalErrorException {
        try {
            ClustersModelMaxGraph clustersModelMaxGraph = (ClustersModelMaxGraph) organizationModels.getClustersModel();
            updateProposedDependencies(organization, organizationModels, clustersModelMaxGraph.getClustersChanged(), true);
            databaseOperations.updateClustersAndDependencies(organization, organizationModels, null, true);
            clustersModelMaxGraph.finishUpdateProcess();
        } catch (ClassCastException e) {
            throw new InternalErrorException("A max_graph method received a model that is not max_graph");
        }
    }

    /**
     * - If the dependency already exists as proposed or rejected, changes the status of the dependency to accepted and merges clusters if necessary
     * - If the dependency does not exists, creates a new accepted dependency and creates a new cluster with the two requirements and merges clusters if necessary
     * - If the dependency was already accepted, does nothing
     */
    @Override
    public void addAcceptedDependencies(String organization, List<Dependency> acceptedDependencies, OrganizationModels organizationModels) throws InternalErrorException {
        try {
            //Initialization
            SimilarityModel similarityModel = organizationModels.getSimilarityModel();
            ClustersModelMaxGraph clustersModelMaxGraph = (ClustersModelMaxGraph) organizationModels.getClustersModel();
            Map<Integer, List<String>> clusters = clustersModelMaxGraph.getClusters();
            Set<Integer> clustersChanged = clustersModelMaxGraph.getClustersChanged();
            Map<String,Integer> reqCluster = clustersModelMaxGraph.getReqCluster();

            for (Dependency dependency : acceptedDependencies) {
                String fromid = dependency.getFromid();
                String toid = dependency.getToid();
                //Checks if the two requirements are valid
                if (fromid != null && toid != null && similarityModel.containsRequirement(fromid) && similarityModel.containsRequirement(toid)) {
                    //Gets the old cluster ids
                    int oldId1 = reqCluster.get(fromid);
                    int oldId2 = reqCluster.get(toid);
                    boolean exists = false;
                    String status = "accepted";
                    try {
                        //Gets the dependency between the two requirements from the database if exists
                        Dependency aux = databaseOperations.getDependency(organization, fromid, toid, true);
                        exists = true;
                        status = aux.getStatus();
                    } catch (NotFoundException e) {
                        //It does not exist
                    }
                    if (!exists || status.equals("proposed") || status.equals("rejected")) {
                        //If the dependency doesn't exist or if it exists as proposed or rejected
                        mergeClusters(clusters, reqCluster, fromid, toid, clustersModelMaxGraph.getLastClusterId());
                        int newId = reqCluster.get(fromid);
                        clustersChanged.add(newId);
                        if (oldId1 != -1) clustersChanged.add(oldId1);
                        if (oldId2 != -1) clustersChanged.add(oldId2);
                        databaseOperations.saveDependencyOrReplace(organization, new Dependency(fromid, toid, "accepted", dependency.getDependencyScore(), newId), true);
                        databaseOperations.saveDependencyOrReplace(organization, new Dependency(toid, fromid, "accepted", dependency.getDependencyScore(), newId), true);
                        if (oldId1 != newId && oldId1 != -1) databaseOperations.updateClusterDependencies(organization, oldId1, newId, true);
                        if (oldId2 != newId && oldId2 != -1) databaseOperations.updateClusterDependencies(organization, oldId2, newId, true);
                    }
                }
            }
        } catch (ClassCastException e) {
            throw new InternalErrorException("A max_graph method received a model that is not max_graph");
        }
    }

    /**
     * - If the dependency already exists as proposed or accepted, changes the status of the dependency to rejected and splits clusters if necessary
     * - If the dependency does not exists, creates a new rejected dependency
     * - If the dependency was already rejected, does nothing
     */
    @Override
    public void addRejectedDependencies(String organization, List<Dependency> deletedDependencies, OrganizationModels organizationModels) throws InternalErrorException {
        try {
            //Initialization
            SimilarityModel similarityModel = organizationModels.getSimilarityModel();
            ClustersModelMaxGraph clustersModelMaxGraph = (ClustersModelMaxGraph) organizationModels.getClustersModel();
            Map<Integer, List<String>> clusters = clustersModelMaxGraph.getClusters();
            Set<Integer> clustersChanged = clustersModelMaxGraph.getClustersChanged();
            Map<String,Integer> reqCluster = clustersModelMaxGraph.getReqCluster();
            int lastClusterId = clustersModelMaxGraph.getLastClusterId();

            for (Dependency dependency : deletedDependencies) {
                String fromid = dependency.getFromid();
                String toid = dependency.getToid();
                //Checks if the two requirements are valid
                if (fromid != null && toid != null && similarityModel.containsRequirement(fromid) && similarityModel.containsRequirement(toid)) {
                    double score = dependency.getDependencyScore();
                    try {
                        //Gets the dependency between the two requirements from the database if exists
                        Dependency aux = databaseOperations.getDependency(organization, fromid, toid, true);
                        String status = aux.getStatus();
                        int clusterId = aux.getClusterId();
                        if (status.equals("accepted")) {
                            //Splits the corresponding clusters if necessary
                            List<String> clusterRequirements = clusters.get(clusterId);
                            databaseOperations.updateDependencyStatus(organization, fromid, toid, "rejected", -1, true);
                            List<Dependency> dependencies = databaseOperations.getClusterDependencies(organization, clusterId, true);
                            HashMap<String, List<String>> reqDeps = createReqDeps(clusterRequirements, dependencies);
                            List<String> requirementIds = new ArrayList<>();
                            requirementIds.add(fromid);
                            requirementIds.add(toid);
                            lastClusterId = recreateClusters(organization, clusters, clustersChanged, reqCluster, lastClusterId, clusterId, clusterRequirements, reqDeps, requirementIds);
                        }
                        if (status.equals("accepted") || status.equals("proposed")) {
                            //Updates the dependency status
                            databaseOperations.saveDependencyOrReplace(organization, new Dependency(fromid, toid, "rejected", score, -1), true);
                            databaseOperations.saveDependencyOrReplace(organization, new Dependency(toid, fromid, "rejected", score, -1), true);
                        }
                    } catch (NotFoundException e) {
                        //The dependency does not exist
                        databaseOperations.saveDependencyOrReplace(organization, new Dependency(fromid, toid, "rejected", score, -1), true);
                        databaseOperations.saveDependencyOrReplace(organization, new Dependency(toid, fromid, "rejected", score, -1), true);
                    }
                }
                clustersModelMaxGraph.setLastClusterId(lastClusterId);
            }
        } catch (ClassCastException e) {
            throw new InternalErrorException("A max_graph method received a model that is not max_graph");
        }
    }

    /**
     * - If the dependency already exists, deletes it and updates the clusters accordingly
     * - If the dependency does not exists, does nothing
     */
    @Override
    public void addDeletedDependencies(String organization, List<Dependency> deletedDependencies, OrganizationModels organizationModels) throws InternalErrorException {
        try {
            //Initialization
            SimilarityModel similarityModel = organizationModels.getSimilarityModel();
            ClustersModelMaxGraph clustersModelMaxGraph = (ClustersModelMaxGraph) organizationModels.getClustersModel();
            Map<Integer, List<String>> clusters = clustersModelMaxGraph.getClusters();
            Set<Integer> clustersChanged = clustersModelMaxGraph.getClustersChanged();
            Map<String,Integer> reqCluster = clustersModelMaxGraph.getReqCluster();
            int lastClusterId = clustersModelMaxGraph.getLastClusterId();

            for (Dependency dependency : deletedDependencies) {
                String fromid = dependency.getFromid();
                String toid = dependency.getToid();
                //Checks if the two requirements are valid
                if (fromid != null && toid != null && similarityModel.containsRequirement(fromid) && similarityModel.containsRequirement(toid)) {
                    try {
                        Dependency aux = databaseOperations.getDependency(organization, fromid, toid, true);
                        String status = aux.getStatus();
                        int clusterId = aux.getClusterId();
                        if (status.equals("accepted")) {
                            //Splits the corresponding clusters if necessary
                            List<String> clusterRequirements = clusters.get(clusterId);
                            databaseOperations.deleteDependencies(organization,fromid,toid,true);
                            List<Dependency> dependencies = databaseOperations.getClusterDependencies(organization, clusterId, true);
                            HashMap<String, List<String>> reqDeps = createReqDeps(clusterRequirements, dependencies);
                            List<String> requirementIds = new ArrayList<>();
                            requirementIds.add(fromid);
                            requirementIds.add(toid);
                            lastClusterId = recreateClusters(organization, clusters, clustersChanged, reqCluster, lastClusterId, clusterId, clusterRequirements, reqDeps, requirementIds);
                        }
                        else if (status.equals("rejected")) {
                            //Deletes the dependency
                            databaseOperations.deleteDependencies(organization,fromid,toid,true);
                        }
                    } catch (NotFoundException e) {
                        //The dependency does not exist
                    }
                }
                clustersModelMaxGraph.setLastClusterId(lastClusterId);
            }
        } catch (ClassCastException e) {
            throw new InternalErrorException("A max_graph method received a model that is not max_graph");
        }
    }

    /**
     * Adds the input requirements to the cluster model. It creates a new cluster with each requirement. If the requirement
     * was inside the cluster model, the method deletes it first.
     */
    @Override
    public void addRequirementsToClusters(String organization, List<Requirement> addRequirements, OrganizationModels organizationModels) throws InternalErrorException {
        try {
            //Initialization
            SimilarityModel similarityModel = organizationModels.getSimilarityModel();
            ClustersModelMaxGraph clustersModelMaxGraph = (ClustersModelMaxGraph) organizationModels.getClustersModel();
            Set<Integer> clustersChanged = clustersModelMaxGraph.getClustersChanged();
            Map<String,Integer> reqCluster = clustersModelMaxGraph.getReqCluster();

            //Deletes the requirement that were inside the model
            for (Requirement requirement : addRequirements) {
                if (similarityModel.containsRequirement(requirement.getId())) {
                    deleteReqFromClusters(organization, requirement.getId(), clustersModelMaxGraph, clustersChanged, reqCluster);
                }
            }
            //Creates a new cluster for each input requirement
            int lastClusterId = clustersModelMaxGraph.getLastClusterId();
            Map<Integer, List<String>> clusters = clustersModelMaxGraph.getClusters();
            for (Requirement requirement : addRequirements) {
                ++lastClusterId;
                List<String> aux = new ArrayList<>();
                aux.add(requirement.getId());
                clusters.put(lastClusterId, aux);
                reqCluster.put(requirement.getId(), lastClusterId);
                clustersChanged.add(lastClusterId);
            }
            clustersModelMaxGraph.setLastClusterId(lastClusterId);
        } catch (ClassCastException e) {
            throw new InternalErrorException("A max_graph method received a model that is not max_graph");
        }
    }

    /**
     * Deletes the input requirements from the cluster model. If the requirement was inside a cluster with more than one requirement, the
     * method splits the cluster if necessary.
     */
    @Override
    public void deleteRequirementsFromClusters(String organization, List<Requirement> deleteRequirements, OrganizationModels organizationModels) throws InternalErrorException {
        try {
            //Initialization
            SimilarityModel similarityModel = organizationModels.getSimilarityModel();
            ClustersModelMaxGraph clustersModelMaxGraph = (ClustersModelMaxGraph) organizationModels.getClustersModel();
            Set<Integer> clustersChanged = clustersModelMaxGraph.getClustersChanged();
            Map<String,Integer> reqCluster = clustersModelMaxGraph.getReqCluster();

            //Deletes the input requirements
            for (Requirement requirement : deleteRequirements) {
                if (similarityModel.containsRequirement(requirement.getId())) {
                    deleteReqFromClusters(organization, requirement.getId(), clustersModelMaxGraph, clustersChanged, reqCluster);
                }
            }
        } catch (ClassCastException e) {
            throw new InternalErrorException("A max_graph method received a model that is not max_graph");
        }
    }


    /*
    Private methods
     */

    private void deleteReqFromClusters(String organization, String req, ClustersModelMaxGraph clustersModelMaxGraph, Set<Integer> clustersChanged, Map<String,Integer> reqCluster) throws InternalErrorException {
        //Deletes one requirement from the cluster model
        Map<Integer,List<String>> clusters = clustersModelMaxGraph.getClusters();
        int lastClusterId = clustersModelMaxGraph.getLastClusterId();

        int clusterId = reqCluster.get(req);
        List<String> clusterRequirements = clusters.get(clusterId);
        clustersChanged.add(clusterId);

        //Checks number of requirements in the cluster
        if (clusterRequirements.size() > 1) {
            //Checks if it is necessary to split the cluster
            List<Dependency> dependencies = databaseOperations.getClusterDependencies(organization, clusterId, true);
            HashMap<String, List<String>> reqDeps = createReqDeps(clusterRequirements, dependencies);
            List<String> candidateReqs = new ArrayList<>(reqDeps.get(req));
            HashSet<String> avoidReqs = new HashSet<>();
            avoidReqs.add(req);
            Clusters aux = bfsClusters(reqDeps, clusterRequirements, candidateReqs, avoidReqs);
            clusterRequirements.remove(req);
            HashMap<Integer, List<String>> candidateClusters = aux.candidateClusters;
            //Updates clusters accordingly
            if (candidateClusters.size() > 1) {
                boolean firstOne = true;
                for (Map.Entry<Integer, List<String>> entry : candidateClusters.entrySet()) {
                    List<String> auxClusterRequirements = entry.getValue();
                    if (firstOne) {
                        clusters.put(clusterId, auxClusterRequirements);
                        firstOne = false;
                    } else {
                        ++lastClusterId;
                        clusters.put(lastClusterId, auxClusterRequirements);
                        clustersChanged.add(lastClusterId);
                        for (String auxReq : auxClusterRequirements) {
                            reqCluster.put(auxReq, lastClusterId);
                            databaseOperations.updateClusterDependencies(organization, auxReq, lastClusterId, true);
                        }
                    }
                }
            }
        } else clustersModelMaxGraph.getClusters().remove(reqCluster.get(req));

        reqCluster.remove(req);
        databaseOperations.deleteReqDependencies(organization, req, true);
        clustersModelMaxGraph.setLastClusterId(lastClusterId);
    }

    private int recreateClusters(String organization, Map<Integer, List<String>> clusters, Set<Integer> clustersChanged, Map<String, Integer> reqCluster, int lastClusterId, int clusterId, List<String> clusterRequirements, HashMap<String, List<String>> reqDeps, List<String> requirementIds) throws InternalErrorException {
        Clusters auxClusters = bfsClusters(reqDeps, clusterRequirements, requirementIds, new HashSet<>());
        HashMap<Integer, List<String>> candidateClusters = auxClusters.candidateClusters;
        if (candidateClusters.size() > 1) {
            boolean firstOne = true;
            for (Map.Entry<Integer, List<String>> entry : candidateClusters.entrySet()) {
                List<String> auxClusterRequirements = entry.getValue();
                if (firstOne) {
                    clusters.put(clusterId, auxClusterRequirements);
                    clustersChanged.add(clusterId);
                    firstOne = false;
                } else {
                    ++lastClusterId;
                    clusters.put(lastClusterId, auxClusterRequirements);
                    clustersChanged.add(lastClusterId);
                    for (String req : auxClusterRequirements) {
                        reqCluster.put(req, lastClusterId);
                        databaseOperations.updateClusterDependencies(organization, req, lastClusterId, true);
                    }
                }
            }
        }
        return lastClusterId;
    }

    private void updateProposedDependencies(String organization, OrganizationModels organizationModels, Set<Integer> clustersChanged, boolean useAuxiliaryTable) throws InternalErrorException {
        try {
            ClustersModelMaxGraph clustersModelMaxGraph = (ClustersModelMaxGraph) organizationModels.getClustersModel();
            Map<Integer, List<String>> clusters = clustersModelMaxGraph.getClusters();
            Set < Integer > clusterIds = new HashSet<>();
            for (int clusterId : clustersChanged) {
                if (clusters.containsKey(clusterId)) clusterIds.add(clusterId);
                databaseOperations.deleteProposedClusterDependencies(organization, clusterId, useAuxiliaryTable);
            }
            computeProposedDependencies(organization, new HashSet<>(organizationModels.getSimilarityModel().getRequirementsIds()), clusterIds, organizationModels, useAuxiliaryTable);
        } catch (ClassCastException e) {
            throw new InternalErrorException("A max_graph method received a model that is not max_graph");
        }
    }

    private void computeProposedDependencies(String organization, Set<String> requirements, Set<Integer> clustersIds, OrganizationModels organizationModels, boolean useAuxiliaryTable) throws InternalErrorException {
        //Recomputes the proposed dependencies between all the requirements of the cluster model and the input clusters
        try {
            //Initialization
            RequirementsSimilarity requirementsSimilarity = Constants.getInstance().getRequirementsSimilarity();
            ClustersModelMaxGraph clustersModelMaxGraph = (ClustersModelMaxGraph) organizationModels.getClustersModel();
            Map<Integer, List<String>> clusters = clustersModelMaxGraph.getClusters();
            Set<String> rejectedDependencies = loadDependenciesByStatus(organization, "rejected", useAuxiliaryTable);
            List<Dependency> proposedDependencies = new ArrayList<>();

            //Loops for each requirement
            int cont = 0;
            int maxDeps = Constants.getInstance().getMaxDepsForPage();
            for (String req1 : requirements) {
                //Loops for each input cluster
                for (int clusterId : clustersIds) {
                    List<String> clusterRequirements = clusters.get(clusterId);
                    double maxScore = organizationModels.getThreshold();
                    String maxReq = null;
                    //Loops for each requirement inside the cluster
                    for (String req2 : clusterRequirements) {
                        //Checks they are not equal nor a rejected dependency
                        if (!rejectedDependencies.contains(req1 + req2) && !req1.equals(req2)) {
                            //Computes the similarity score and updates the maxScore if it is higher
                            double score = requirementsSimilarity.computeSimilarity(organizationModels, req1, req2);
                            if (score >= maxScore) {
                                maxScore = score;
                                maxReq = req2;
                            }
                        }
                    }
                    if (maxReq != null) {
                        //Updates the found dependency
                        ++cont;
                        proposedDependencies.add(new Dependency(req1, maxReq, "proposed", maxScore, clusterId));
                        if (cont >= maxDeps) {
                            cont = 0;
                            databaseOperations.saveDependencies(organization, proposedDependencies, useAuxiliaryTable);
                            proposedDependencies = new ArrayList<>();
                        }
                    }
                }
            }
            if (!proposedDependencies.isEmpty())
                databaseOperations.saveDependencies(organization, proposedDependencies, useAuxiliaryTable);
        } catch (ClassCastException e) {
            throw new InternalErrorException("A max_graph method received a model that is not max_graph");
        }
    }
    private class Clusters {
        HashMap<Integer,List<String>> candidateClusters;
        HashMap<String,Integer> reqCluster; //TODO is truly necessary?
        Clusters(HashMap<Integer,List<String>> candidateClusters, HashMap<String,Integer> reqCluster) {
            this.candidateClusters = candidateClusters;
            this.reqCluster = reqCluster;
        }
    }

    private Set<String> loadDependenciesByStatus(String organization, String status, boolean useAuxiliaryTable) throws InternalErrorException {
        //Loads the dependencies from the database with the input status
        Set<String> result = new HashSet<>();
        if (Constants.getInstance().getDatabaseModel().existsOrganization(organization)) {
            List<Dependency> dependencies = databaseOperations.getDependenciesByStatus(organization, status, useAuxiliaryTable);
            for (Dependency dependency : dependencies) {
                String fromId = dependency.getFromid();
                String toId = dependency.getToid();
                result.add(fromId + toId);
                result.add(toId + fromId);
            }
        }
        return result;
    }

    private Clusters bfsClusters(HashMap<String,List<String>> reqDeps, List<String> clusterRequirements, List<String> requirementIds, HashSet<String> avoidReqs) {
        //Recreates a cluster according to its connectivity

        //Initialization
        HashMap<Integer,List<String>> candidateClusters = new HashMap<>();
        HashSet<String> processedReqs = new HashSet<>();
        HashMap<String,Integer> reqCluster = new HashMap<>();
        PriorityQueue<String> priorityQueue = new PriorityQueue<>();
        int countIds = 0;

        //Puts each requirement in a different cluster
        for (String requirement: clusterRequirements) {
            reqCluster.put(requirement, -1);
        }
        for (String requirement: requirementIds) {
            List<String> aux = new ArrayList<>();
            aux.add(requirement);
            candidateClusters.put(countIds, aux);
            reqCluster.put(requirement, countIds);
            priorityQueue.add(requirement);
            ++countIds;
        }

        //Does a bfs until all requirements are checked or all of them are inside the same cluster
        while(!priorityQueue.isEmpty() && candidateClusters.size() > 1) {
            String requirement = priorityQueue.poll();
            if (!processedReqs.contains(requirement)) {
                for (String req2 : reqDeps.get(requirement)) {
                    if (!processedReqs.contains(req2) && !avoidReqs.contains(req2)) {
                        countIds = mergeClusters(candidateClusters, reqCluster, requirement, req2, countIds);
                        priorityQueue.add(req2);
                    }
                }
                processedReqs.add(requirement);
            }
        }
        return new Clusters(candidateClusters, reqCluster);
    }

    private HashMap<String,List<String>> createReqDeps(List<String> clusterRequirements, List<Dependency> dependencies) {
        HashMap<String,List<String>> reqDeps = new HashMap<>();
        for (String requirement: clusterRequirements) {
            List<String> aux = new ArrayList<>();
            reqDeps.put(requirement, aux);
        }
        for (Dependency dependency: dependencies) {
            String fromid = dependency.getFromid();
            String toid = dependency.getToid();
            List<String> aux1 = reqDeps.get(fromid);
            List<String> aux2 = reqDeps.get(toid);
            if (!aux1.contains(toid)) {
                aux1.add(toid);
                aux2.add(fromid);
                reqDeps.put(fromid, aux1);
                reqDeps.put(toid, aux2);
            }
        }
        return reqDeps;
    }


    private int computeDependencies(List<Dependency> dependencies, Map<String,Integer> reqCluster, Map<Integer,List<String>> clusters, int countIds) {
        for (Dependency dependency: dependencies) {
            if (validDependency(dependency) && dependency.getStatus().equals("accepted")) {
                String fromid = dependency.getFromid();
                String toid = dependency.getToid();
                if (reqCluster.containsKey(fromid) && reqCluster.containsKey(toid)) {
                    countIds = mergeClusters(clusters, reqCluster, fromid, toid, countIds);
                }
            }
        }
        return countIds;
    }

    private boolean validDependency(Dependency dependency) {
        String type = dependency.getDependencyType();
        String status = dependency.getStatus();
        return (type != null && status != null && (type.equals("similar") || type.equals("duplicates")) && (status.equals("accepted") || status.equals("rejected")));
    }

    private int mergeClusters(Map<Integer,List<String>> clusters, Map<String,Integer> reqCluster, String req1, String req2, int countIds) {
        //Merges the clusters of the two input requirements if possible
        int clusterReq1 = reqCluster.get(req1);
        int clusterReq2 = reqCluster.get(req2);
        if (clusterReq1 == -1 && clusterReq2 == -1) {
            List<String> aux = new ArrayList<>();
            aux.add(req1);
            aux.add(req2);
            clusters.put(countIds, aux);
            reqCluster.put(req1, countIds);
            reqCluster.put(req2, countIds);
            ++countIds;
        } else if (clusterReq1 == -1) {
            List<String> aux = clusters.get(clusterReq2);
            aux.add(req1);
            clusters.put(clusterReq2, aux);
            reqCluster.put(req1, clusterReq2);
        } else if (clusterReq2 == -1) {
            List<String> aux = clusters.get(clusterReq1);
            aux.add(req2);
            clusters.put(clusterReq1, aux);
            reqCluster.put(req2, clusterReq1);
        } else if (clusterReq1 != clusterReq2) {
            List<String> aux1 = clusters.get(clusterReq1);
            List<String> aux2 = clusters.get(clusterReq2);
            aux1.addAll(aux2);
            for (String req: aux2) {
                reqCluster.put(req,clusterReq1);
            }
            clusters.remove(clusterReq2);
        }
        return countIds;
    }

    private List<Dependency> duplicateDependencies(List<Dependency> dependencies) {
        List<Dependency> result = new ArrayList<>();
        for (Dependency dependency: dependencies) {
            result.add(dependency);
            result.add(new Dependency(dependency.getToid(), dependency.getFromid(), dependency.getStatus(), dependency.getDependencyScore(), dependency.getClusterId()));
        }
        return result;
    }
}
