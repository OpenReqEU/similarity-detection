package upc.similarity.compareapi.util;

import org.json.JSONArray;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.entity.ClusterAndDeps;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFoundException;

import java.util.*;

public class ClusterOperations {

    private static ClusterOperations instance = new ClusterOperations();
    private static boolean dummy = false;

    private ClusterOperations(){}

    public static ClusterOperations getInstance() {
        return instance;
    }

    public ClusterAndDeps computeIniClusters(List<Dependency> dependencies, List<Requirement> requirements) {

        HashMap<Integer,List<String>> clusters = new HashMap<>();
        HashMap<String, Integer> reqCluster = new HashMap<>();
        Integer countIds = 0;

        for (Requirement requirement: requirements) {
            List<String> aux = new ArrayList<>();
            aux.add(requirement.getId());
            clusters.put(-1,aux);
        }

        computeDependencies(dependencies, reqCluster, clusters, countIds);

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

        return new ClusterAndDeps(countIds, clusters, reqCluster, acceptedDependencies);
    }

    //se supone que todos los requisitos estan en el modelo y que los requisitos de entrada no estan en ningun cluster
    public void reqClusters(String organization, String responseId, List<String> requirements, Map<String, Map<String, Double>> docs, Map<Integer, List<String>> clusters, double threshold) throws InternalErrorException {
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();
        Constants constants = Constants.getInstance();
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        int cont = 0;
        List<String> requirementsToCompare = new ArrayList<>();
        JSONArray array = new JSONArray();

        for (String req1: requirements) {
            Iterator it = clusters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                List<String> clusterRequirements = (List<String>) pair.getValue();
                double maxScore = -1;
                String maxReq = null;
                for (String req2: clusterRequirements) {
                    double score = cosineSimilarity.compute(docs, req1, req2);
                    if (score > maxScore) {
                        maxScore = score;
                        maxReq = req2;
                    }
                }
                if (maxReq != null && maxScore >= threshold) {
                    Dependency dependency = new Dependency(maxScore, req1, maxReq, constants.getStatus(), constants.getDependencyType(), constants.getComponent());
                    array.put(dependency.toJSON());
                    ++cont;
                    if (cont >= constants.getMaxDepsForPage()) {
                        databaseOperations.generateResponsePage(responseId, organization, array,constants.getDependenciesArrayName());
                        array = new JSONArray();
                        cont = 0;
                    }
                }
            }

            for (String req2: requirementsToCompare) {
                double score = cosineSimilarity.compute(docs, req1, req2);
                if (score >= threshold) {
                    Dependency dependency = new Dependency(score, req1, req2, constants.getStatus(), constants.getDependencyType(), constants.getComponent());
                    array.put(dependency.toJSON());
                    ++cont;
                    if (cont >= constants.getMaxDepsForPage()) {
                        databaseOperations.generateResponsePage(responseId, organization, array,constants.getDependenciesArrayName());
                        array = new JSONArray();
                        cont = 0;
                    }
                }
            }
            requirementsToCompare.add(req1);
        }

        if (array.length() > 0) {
            databaseOperations.generateResponsePage(responseId, organization, array, constants.getDependenciesArrayName());
        }
    }

    //se supone que todos los requisitos estan en el modelo y que los requisitos de entrada no estan en ningun cluster
    public JSONArray reqClustersNotDb(List<String> requirements, Map<String, Map<String, Double>> docs, Map<Integer, List<String>> clusters, double threshold) {
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();
        Constants constants = Constants.getInstance();
        List<String> requirementsToCompare = new ArrayList<>();
        JSONArray array = new JSONArray();

        for (String req1: requirements) {
            Iterator it = clusters.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                List<String> clusterRequirements = (List<String>) pair.getValue();
                double maxScore = -1;
                String maxReq = null;
                for (String req2: clusterRequirements) {
                    double score = cosineSimilarity.compute(docs, req1, req2);
                    if (score > maxScore) {
                        maxScore = score;
                        maxReq = req2;
                    }
                }
                if (maxReq != null && maxScore >= threshold) {
                    Dependency dependency = new Dependency(maxScore, req1, maxReq, constants.getStatus(), constants.getDependencyType(), constants.getComponent());
                    array.put(dependency.toJSON());
                }
            }

            for (String req2: requirementsToCompare) {
                double score = cosineSimilarity.compute(docs, req1, req2);
                if (score >= threshold) {
                    Dependency dependency = new Dependency(score, req1, req2, constants.getStatus(), constants.getDependencyType(), constants.getComponent());
                    array.put(dependency.toJSON());
                }
            }
            requirementsToCompare.add(req1);
        }
        return array;
    }

    private void computeDependencies(List<Dependency> dependencies, Map<String,Integer> reqCluster, Map<Integer,List<String>> clusters, Integer countIds) {
        for (Dependency dependency: dependencies) {
            if (validDependency(dependency)) {
                String fromid = dependency.getFromid();
                String toid = dependency.getToid();
                if (reqCluster.containsKey(fromid) && reqCluster.containsKey(toid)) {
                    mergeClusters(clusters, reqCluster, fromid, toid, countIds);
                }
            }
        }
    }

    private boolean validDependency(Dependency dependency) {
        String type = dependency.getDependencyType();
        return (type != null && (type.equals("similar") || type.equals("duplicates")) && dependency.getStatus().equals("accepted"));
    }

    private void mergeClusters(Map<Integer,List<String>> clusters, Map<String,Integer> reqCluster, String req1, String req2, Integer countIds) {
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
    }

    /*public void deleteReqFromClusters(String organization, String responseId, String requirementId, Map<Integer, List<String>> clusters, int lastClusterId) throws InternalErrorException {

        int cluster = findClusterId(requirementId, clusters);
        if (cluster == -1) {
            DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
            List<String> clusterRequirements = clusters.get(cluster);
            clusterRequirements.remove(requirementId);
            HashMap<String,List<String>> reqDeps = loadClusterDependencies(organization, responseId, cluster, clusterRequirements);
            databaseOperations.deleteReqDependencies(organization, responseId, requirementId);
            HashMap<Integer,List<String>> candidateClusters = bfsClusters(reqDeps, clusterRequirements, reqDeps.get(requirementId));
            if (candidateClusters.size() > 1) {
                boolean firstOne = true;
                Iterator it = candidateClusters.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    List<String> aux = (List<String>) pair.getValue();
                    if (firstOne) {
                        clusters.put(cluster, aux);
                        firstOne = false;
                    } else {
                        ++lastClusterId;
                        clusters.put(lastClusterId, aux);
                        for (String req: aux) {
                            databaseOperations.updateClusterDependencies(organization, responseId, req, "accepted", lastClusterId);
                        }
                    }
                }
            }
        }
    }

    public HashMap<String, Integer> computeReqClusterMap(Map<Integer,List<String>> clusters, Set<String> requirements) {
        HashMap<String,Integer> reqCluster = new HashMap<>();
        for (String requirement: requirements) {
            reqCluster.put(requirement, -1);
        }
        Iterator it = clusters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            int id = (int) pair.getKey();
            List<String> clusterRequirements = (List<String>) pair.getValue();
            for (String req: clusterRequirements) {
                reqCluster.put(req, id);
            }
        }
        return reqCluster;
    }

    public void addAcceptedDependencies(String organization, String responseId, List<Dependency> acceptedDependencies, Map<Integer,List<String>> clusters, Map<String,Integer> reqCluster, Integer countIds) throws InternalErrorException{
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();

        for (Dependency dependency: acceptedDependencies) {
            String fromid = dependency.getFromid();
            String toid = dependency.getToid();
            int oldId1 = reqCluster.get(fromid);
            int oldId2 = reqCluster.get(toid);
            try {
                databaseOperations.getDependency(organization, responseId, fromid, toid);
            } catch (NotFoundException e) {
                mergeClusters(clusters, reqCluster, fromid, toid, countIds);
                dependency.setClusterId(reqCluster.get(fromid));
                databaseOperations.saveDependency(organization, responseId, dependency);
                int newId1 = reqCluster.get(fromid);
                int newId2 = reqCluster.get(toid);
                if (oldId1 != newId1 && oldId1 != -1) databaseOperations.updateClusterDependencies(organization, responseId, oldId1, newId1);
                if (oldId2 != newId2 && oldId2 != -1) databaseOperations.updateClusterDependencies(organization, responseId, oldId2, newId2);
            }
        }
    }

    public void addDeletedDependencies(String organization, String responseId, List<Dependency> deletedDependencies, Map<Integer,List<String>> clusters, Map<String,Integer> reqCluster, Integer lastClusterId) throws InternalErrorException {
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        //TODO need to update reqCluster?
        for (Dependency dep: deletedDependencies) {
            try {
                Dependency dependency = databaseOperations.getDependency(organization, responseId, dep.getFromid(), dep.getToid());
                if (dependency.getStatus().equals("accepted")) {
                    int clusterId = dependency.getClusterId();
                    String fromid = dep.getFromid();
                    String toid = dep.getToid();
                    databaseOperations.updateDependency(organization, responseId, fromid, toid, "rejected", -1);
                    HashMap<String,List<String>> reqDeps = loadClusterDependencies(organization, responseId, clusterId, clusters.get(clusterId));
                    List<String> aux = new ArrayList<>();
                    aux.add(fromid);
                    aux.add(toid);
                    HashMap<Integer,List<String>> candidateClusters = bfsClusters(reqDeps, clusters.get(clusterId), aux);
                    if (candidateClusters.size() > 1) {
                        boolean firstOne = true;
                        Iterator it = candidateClusters.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry) it.next();
                            List<String> clusterRequirements = (List<String>) pair.getValue();
                            if (firstOne) {
                                clusters.put(clusterId, clusterRequirements);
                                firstOne = false;
                            } else {
                                ++lastClusterId;
                                clusters.put(lastClusterId, clusterRequirements);
                                for (String req: clusterRequirements) {
                                    databaseOperations.updateClusterDependencies(organization, responseId, req, "accepted", lastClusterId);
                                }
                            }
                        }
                    }
                }
            } catch (NotFoundException e) {
                //empty
            }
        }

    }*/

    /*
    Auxiliary operations
     */

    private int findClusterId(String requirement, Map<Integer,List<String>> clusters) {
        Iterator it = clusters.entrySet().iterator();
        int cluster = -1;
        while (it.hasNext() && cluster == -1) {
            Map.Entry pair = (Map.Entry) it.next();
            int id = (int) pair.getKey();
            List<String> clusterRequirements = (List<String>) pair.getValue();
            if (clusterRequirements.contains(requirement)) {
                cluster = id;
            }
        }
        return cluster;
    }

    private HashMap<Integer,List<String>> bfsClusters(HashMap<String,List<String>> reqDeps, List<String> clusterRequirements, List<String> requirementIds) {
        HashMap<Integer,List<String>> candidateClusters = new HashMap<>();
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
        while(priorityQueue.size() > 0 && candidateClusters.size() > 1) {
            String requirement = priorityQueue.poll();
            for (String req2: reqDeps.get(requirement)) {
                mergeClusters(candidateClusters, reqCluster, requirement, req2, countIds);
            }
        }
        return candidateClusters;
    }

    /*private HashMap<String,List<String>> loadClusterDependencies(String organization, String responseId, int clusterId, List<String> clusterRequirements) throws InternalErrorException {
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        List<Dependency> dependencies = databaseOperations.getClusterDependencies(organization, responseId, clusterId);
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
            aux1.add(toid);
            aux2.add(fromid);
            reqDeps.put(fromid, aux1);
            reqDeps.put(toid, aux2);
        }
        return reqDeps;
    }*/
}
