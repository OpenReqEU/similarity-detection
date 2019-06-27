package upc.similarity.compareapi.util;

import org.json.JSONArray;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.entity.ClusterAndDeps;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.exception.InternalErrorException;

import java.util.*;

public class ClusterOperations {

    private static ClusterOperations instance = new ClusterOperations();

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

        return new ClusterAndDeps(clusters, reqCluster, acceptedDependencies);
    }

    //se supone que todos los requisitos estan en el modelo y que los requisitos de entrada no estan en ningun cluster
    public void reqClusters(String organization, String responseId, List<String> requirements, Map<String, Map<String, Double>> docs, Map<Integer, List<String>> clusters, double threshold) throws InternalErrorException {
        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();
        Constants constants = Constants.getInstance();
        DatabaseOperations databaseOperations = DatabaseOperations.getInstance();
        int cont = 0;
        int pages = 0;
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
                        databaseOperations.generateResponsePage(responseId, organization, pages, array,constants.getDependenciesArrayName());
                        ++pages;
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
                        databaseOperations.generateResponsePage(responseId, organization, pages, array,constants.getDependenciesArrayName());
                        ++pages;
                        array = new JSONArray();
                        cont = 0;
                    }
                }
            }

            requirementsToCompare.add(req1);
        }

        if (array.length() > 0) {
            databaseOperations.generateResponsePage(responseId, organization, pages, array, constants.getDependenciesArrayName());
        }
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
}
