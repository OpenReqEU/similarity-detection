package upc.similarity.compareapi.clusters_algorithm.max_graph;

import upc.similarity.compareapi.clusters_algorithm.ClustersAlgorithm;
import upc.similarity.compareapi.clusters_algorithm.ClustersModel;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.dao.algorithm_models_dao.clusters_algorithm.max_graph.ClustersModelDatabaseMaxGraph;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.similarity_algorithm.SimilarityAlgorithm;
import upc.similarity.compareapi.similarity_algorithm.SimilarityModel;

import java.util.*;

public class ClustersAlgorithmMaxGraph implements ClustersAlgorithm {

    private ClustersModelDatabaseMaxGraph databaseOperations;

    public ClustersAlgorithmMaxGraph(ClustersModelDatabaseMaxGraph databaseOperations) {
        this.databaseOperations = databaseOperations;
    }

    @Override
    public ClustersModel buildModel(List<Requirement> requirements, List<Dependency> dependencies) throws InternalErrorException {
        HashMap<Integer,List<String>> clusters = new HashMap<>();
        HashMap<String, Integer> reqCluster = new HashMap<>();
        int countIds = 0;

        for (Requirement requirement: requirements) {
            reqCluster.put(requirement.getId(),-1);
        }

        countIds = computeDependencies(dependencies, reqCluster, clusters, countIds);

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

        List<Dependency> acceptedDependencies = new ArrayList<>();
        HashSet<String> notRepeated = new HashSet<>();

        for (Dependency dependency: dependencies) {
            if (validDependency(dependency)) {
                String fromid = dependency.getFromid();
                String toid = dependency.getToid();
                if (reqCluster.containsKey(fromid) && reqCluster.containsKey(toid)) {
                    if (!notRepeated.contains(fromid+toid) && !notRepeated.contains(toid+fromid)) {
                        notRepeated.add(fromid+toid);
                        dependency.setClusterId(reqCluster.get(fromid));
                        acceptedDependencies.add(dependency);
                    }
                }
            }
        }

        return new ClustersModelMaxGraph(countIds-1,clusters,duplicateDependencies(acceptedDependencies));
    }

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


    /*
    Private methods
     */

    private void computeProposedDependencies(String organization, Set<String> requirements, Set<Integer> clustersIds, OrganizationModels organizationModels, boolean useAuxiliaryTable) throws InternalErrorException {
        try {
            SimilarityModel similarityModel = organizationModels.getSimilarityModel();
            SimilarityAlgorithm similarityAlgorithm = Constants.getInstance().getSimilarityAlgorithm();
            ClustersModelMaxGraph clustersModelMaxGraph = (ClustersModelMaxGraph) organizationModels.getClustersModel();
            Map<Integer, List<String>> clusters = clustersModelMaxGraph.getClusters();
            Set<String> rejectedDependencies = loadDependenciesByStatus(organization, "rejected", useAuxiliaryTable);
            List<Dependency> proposedDependencies = new ArrayList<>();
            int cont = 0;
            int maxDeps = Constants.getInstance().getMaxDepsForPage();
            //TODO this is causing n*n efficiency, can be improved saving the result of the pairs and only compute half of the matrix (less memory efficiency)
            for (String req1 : requirements) {
                for (int clusterId : clustersIds) {
                    List<String> clusterRequirements = clusters.get(clusterId);
                    double maxScore = organizationModels.getThreshold();
                    String maxReq = null;
                    for (String req2 : clusterRequirements) {
                        if (!rejectedDependencies.contains(req1 + req2) && !req1.equals(req2)) {
                            double score = similarityAlgorithm.computeSimilarity(similarityModel, req1, req2);
                            if (score >= maxScore) {
                                maxScore = score;
                                maxReq = req2;
                            }
                        }
                    }
                    if (maxReq != null) {
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
        HashMap<Integer,List<String>> candidateClusters = new HashMap<>();
        HashSet<String> processedReqs = new HashSet<>();
        HashMap<String,Integer> reqCluster = new HashMap<>();
        PriorityQueue<String> priorityQueue = new PriorityQueue<>();
        int countIds = 0;
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
            if (validDependency(dependency)) {
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
        return (type != null && (type.equals("similar") || type.equals("duplicates")) && dependency.getStatus().equals("accepted"));
    }

    private int mergeClusters(Map<Integer,List<String>> clusters, Map<String,Integer> reqCluster, String req1, String req2, int countIds) {
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

    //TODO maybe change this
    private List<Dependency> duplicateDependencies(List<Dependency> dependencies) {
        List<Dependency> result = new ArrayList<>();
        for (Dependency dependency: dependencies) {
            result.add(dependency);
            result.add(new Dependency(dependency.getToid(), dependency.getFromid(), dependency.getStatus(), dependency.getDependencyScore(), dependency.getClusterId()));
        }
        return result;
    }
}
