package upc.similarity.compareapi.util;

import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.dao.DatabaseModel;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.entity.auxiliary.ClusterAndDeps;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFoundException;
import upc.similarity.compareapi.similarity_algorithm.SimilarityAlgorithm;
import upc.similarity.compareapi.similarity_algorithm.SimilarityModel;

import java.util.*;

public class ClusterOperations {

    private static ClusterOperations instance = new ClusterOperations();
    private SimilarityAlgorithm similarityAlgorithm = Constants.getInstance().getSimilarityAlgorithm();
    private DatabaseModel databaseOperations = Constants.getInstance().getDatabaseModel();

    private ClusterOperations(){}

    public static ClusterOperations getInstance() {
        return instance;
    }

    public ClusterAndDeps computeIniClusters(List<Dependency> dependencies, List<Requirement> requirements) {

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

        return new ClusterAndDeps(countIds-1, clusters, reqCluster, duplicateDependencies(acceptedDependencies));
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

    public void addRequirementsToClusters(String organization, List<Requirement> addRequirements, OrganizationModels organizationModels, Set<Integer> clustersChanged, Map<String,Integer> reqCluster) throws InternalErrorException {
        SimilarityModel similarityModel = organizationModels.getSimilarityModel();
        for (Requirement requirement: addRequirements) {
            if (similarityModel.containsRequirement(requirement.getId())) {
                deleteReqFromClusters(organization, requirement.getId(), organizationModels, clustersChanged, reqCluster);
            }
        }
        int lastClusterId = organizationModels.getLastClusterId();
        Map<Integer,List<String>> clusters = organizationModels.getClusters();
        for (Requirement requirement: addRequirements) {
            ++lastClusterId;
            List<String> aux = new ArrayList<>();
            aux.add(requirement.getId());
            clusters.put(lastClusterId,aux);
            reqCluster.put(requirement.getId(), lastClusterId);
            clustersChanged.add(lastClusterId);
        }
        organizationModels.setLastClusterId(lastClusterId);
    }

    public void updateProposedDependencies(String organization, OrganizationModels organizationModels, Set<Integer> clustersChanged, boolean useAuxiliaryTable) throws InternalErrorException {
        Map<Integer,List<String>> clusters = organizationModels.getClusters();
        Set<Integer> clusterIds = new HashSet<>();
        for (int clusterId: clustersChanged) {
           if (clusters.containsKey(clusterId)) clusterIds.add(clusterId);
           databaseOperations.deleteProposedClusterDependencies(organization, clusterId, useAuxiliaryTable);
        }
        computeProposedDependencies(organization, new HashSet<>(organizationModels.getSimilarityModel().getRequirementsIds()), clusterIds, organizationModels, useAuxiliaryTable);
    }

    public void computeProposedDependencies(String organization, Set<String> requirements, Set<Integer> clustersIds, OrganizationModels organizationModels, boolean useAuxiliaryTable) throws InternalErrorException {
        Map<Integer,List<String>> clusters = organizationModels.getClusters();
        Set<String> rejectedDependencies = loadDependenciesByStatus(organization, "rejected", useAuxiliaryTable);
        List<Dependency> proposedDependencies = new ArrayList<>();
        int cont = 0;
        int maxDeps = Constants.getInstance().getMaxDepsForPage();
        SimilarityModel similarityModel = organizationModels.getSimilarityModel();
        //TODO this is causing n*n efficiency, can be improved saving the result of the pairs and only compute half of the matrix (less memory efficiency)
        for (String req1: requirements) {
            for (int clusterId: clustersIds) {
                List<String> clusterRequirements = clusters.get(clusterId);
                double maxScore = organizationModels.getThreshold();
                String maxReq = null;
                for (String req2: clusterRequirements) {
                    if (!rejectedDependencies.contains(req1+req2) && !req1.equals(req2)) {
                        double score = similarityAlgorithm.computeSimilarity(similarityModel,req1,req2);
                        if (score >= maxScore) {
                            maxScore = score;
                            maxReq = req2;
                        }
                    }
                }
                if (maxReq != null) {
                    ++cont;
                    proposedDependencies.add(new Dependency(req1,maxReq,"proposed",maxScore,clusterId));
                    if (cont >= maxDeps) {
                        cont = 0;
                        databaseOperations.saveDependencies(organization, proposedDependencies, useAuxiliaryTable);
                        proposedDependencies = new ArrayList<>();
                    }
                }
            }
        }
        if (!proposedDependencies.isEmpty()) databaseOperations.saveDependencies(organization, proposedDependencies, useAuxiliaryTable);
    }

    private void deleteReqFromClusters(String organization, String req, OrganizationModels organizationModels, Set<Integer> clustersChanged, Map<String,Integer> reqCluster) throws InternalErrorException {
        Map<Integer,List<String>> clusters = organizationModels.getClusters();
        int lastClusterId = organizationModels.getLastClusterId();

        int clusterId = reqCluster.get(req);
        List<String> clusterRequirements = clusters.get(clusterId);
        clustersChanged.add(clusterId);
        if (clusterRequirements.size() > 1) {
            List<Dependency> dependencies = databaseOperations.getClusterDependencies(organization, clusterId, true);
            HashMap<String, List<String>> reqDeps = createReqDeps(clusterRequirements, dependencies);
            List<String> candidateReqs = new ArrayList<>(reqDeps.get(req));
            HashSet<String> avoidReqs = new HashSet<>();
            avoidReqs.add(req);
            Clusters aux = bfsClusters(reqDeps, clusterRequirements, candidateReqs, avoidReqs);
            clusterRequirements.remove(req);
            HashMap<Integer, List<String>> candidateClusters = aux.candidateClusters;
            //updating clusters
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
        } else organizationModels.getClusters().remove(reqCluster.get(req));

        reqCluster.remove(req);
        databaseOperations.deleteReqDependencies(organization, req, true);
        organizationModels.setLastClusterId(lastClusterId);
    }

    public void addRejectedDependencies(String organization, List<Dependency> deletedDependencies, OrganizationModels organizationModels, Set<Integer> clustersChanged, Map<String,Integer> reqCluster) throws InternalErrorException {
        SimilarityModel similarityModel = organizationModels.getSimilarityModel();
        Map<Integer, List<String>> clusters = organizationModels.getClusters();

        int lastClusterId = organizationModels.getLastClusterId();

        for (Dependency dependency: deletedDependencies) {
            String fromid = dependency.getFromid();
            String toid = dependency.getToid();
            if (fromid != null && toid != null && similarityModel.containsRequirement(fromid) && similarityModel.containsRequirement(toid)) {
                double score = dependency.getDependencyScore();
                try {
                    Dependency aux = databaseOperations.getDependency(organization, fromid, toid, true);
                    String status = aux.getStatus();
                    int clusterId = aux.getClusterId();
                    if (status.equals("accepted")) {
                        List<String> clusterRequirements = clusters.get(clusterId);
                        databaseOperations.updateDependencyStatus(organization, fromid, toid, "rejected", -1, true);
                        List<Dependency> dependencies = databaseOperations.getClusterDependencies(organization, clusterId, true);
                        HashMap<String, List<String>> reqDeps = createReqDeps(clusterRequirements, dependencies);
                        List<String> requirementIds = new ArrayList<>();
                        requirementIds.add(fromid);
                        requirementIds.add(toid);
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
                    }
                    if (status.equals("accepted") || status.equals("proposed")) {
                        databaseOperations.saveDependencyOrReplace(organization, new Dependency(fromid, toid, "rejected", score, -1), true);
                        databaseOperations.saveDependencyOrReplace(organization, new Dependency(toid, fromid, "rejected", score, -1), true);
                    }
                } catch (NotFoundException e) {
                    databaseOperations.saveDependencyOrReplace(organization, new Dependency(fromid, toid, "rejected", score, -1), true);
                    databaseOperations.saveDependencyOrReplace(organization, new Dependency(toid, fromid, "rejected", score, -1), true);
                }
            }
            organizationModels.setLastClusterId(lastClusterId);
        }
    }

    public void addAcceptedDependencies(String organization, List<Dependency> acceptedDependencies, OrganizationModels organizationModels, Set<Integer> clustersChanged, Map<String,Integer> reqCluster) throws InternalErrorException {
        Map<Integer,List<String>> clusters = organizationModels.getClusters();
        SimilarityModel similarityModel = organizationModels.getSimilarityModel();

        for (Dependency dependency: acceptedDependencies) {
            String fromid = dependency.getFromid();
            String toid = dependency.getToid();
            if (fromid != null && toid != null && similarityModel.containsRequirement(fromid) && similarityModel.containsRequirement(toid)) {
                int oldId1 = reqCluster.get(fromid);
                int oldId2 = reqCluster.get(toid);
                boolean exists = false;
                String status = "accepted";
                try {
                    Dependency aux = databaseOperations.getDependency(organization, fromid, toid, true);
                    exists = true;
                    status = aux.getStatus();
                } catch (NotFoundException e) {
                    //empty
                }
                if (!exists || status.equals("proposed") || status.equals("rejected")) {
                    mergeClusters(clusters, reqCluster, fromid, toid, organizationModels.getLastClusterId());
                    int newId = reqCluster.get(fromid);
                    clustersChanged.add(newId);
                    if (oldId1 != -1) clustersChanged.add(oldId1);
                    if (oldId2 != -1) clustersChanged.add(oldId2);
                    databaseOperations.saveDependencyOrReplace(organization, new Dependency(fromid, toid, "accepted", dependency.getDependencyScore(), newId), true);
                    databaseOperations.saveDependencyOrReplace(organization, new Dependency(toid, fromid, "accepted", dependency.getDependencyScore(), newId), true);
                    if (oldId1 != newId && oldId1 != -1)
                        databaseOperations.updateClusterDependencies(organization, oldId1, newId, true);
                    if (oldId2 != newId && oldId2 != -1)
                        databaseOperations.updateClusterDependencies(organization, oldId2, newId, true);
                }
            }
        }
    }


    /*
    Auxiliary operations
     */

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
        if (databaseOperations.existsOrganization(organization)) {
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
}
