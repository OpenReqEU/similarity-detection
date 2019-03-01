package upc.similarity.semilarapi.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import semilar.config.ConfigManager;
import semilar.sentencemetrics.GreedyComparer;
import semilar.tools.StopWords;
import semilar.tools.semantic.WordNetSimilarity;
import semilar.wordmetrics.WNWordMetric;
import upc.similarity.semilarapi.config.Configuration;
import upc.similarity.semilarapi.dao.RequirementDAO;
import upc.similarity.semilarapi.dao.SQLiteDAO;
import upc.similarity.semilarapi.entity.*;
import upc.similarity.semilarapi.entity.input.PairReq;
import upc.similarity.semilarapi.entity.input.ProjOp;
import upc.similarity.semilarapi.entity.input.ReqProjOp;
import upc.similarity.semilarapi.entity.input.RequirementId;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.InternalErrorException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service("semilarService")
public class SemilarServiceImpl implements SemilarService {
    
    static {
        ConfigManager.setSemilarDataRootFolder("./Models/");
        wnMetricLin = new WNWordMetric(WordNetSimilarity.WNSimMeasure.LIN, false);
        stopWords = new StopWords();
        number_threads = Configuration.getInstance().getNumber_threads();
    }

    private static WNWordMetric wnMetricLin;
    private RequirementDAO requirementDAO = getValue();
    private String component = "Similarity-Semilar";

    private static int number_threads;
    //private static final GreedyComparer greedyComparerWNLin = new GreedyComparer(wnMetricLin, 0.3f, true);
    private static StopWords stopWords;
    private static final GreedyComparer greedyComparerWNLin = new GreedyComparer(wnMetricLin,stopWords,0.3f, true, "NONE","AVERAGE",false,true,false,true,true,false, false);

    private RequirementDAO getValue() {
        try {
            return new SQLiteDAO();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Similarity
    @Override
    public void similarity(String compare, String filename, PairReq input) throws SQLException, BadRequestException, InternalErrorException {

        RequirementId req1 = input.getReq1();
        RequirementId req2 = input.getReq2();
        if (req1.getId() == null || req2.getId() == null) throw new BadRequestException("One requirement has id equal to null");
        if (req1.getId().equals(req2.getId())) throw new BadRequestException("The requirements to be compared have the same id");
        Requirement r1 = requirementDAO.getRequirement(req1.getId());
        Requirement r2 = requirementDAO.getRequirement(req2.getId());
        ComparisonBetweenSentences comparer = new ComparisonBetweenSentences(greedyComparerWNLin,compare,0,false,component);
        if (comparer.existsDependency(r1.getId(),r2.getId(),input.getDependencies())) throw new BadRequestException("Already exists another similar or duplicate dependency between the same requirements");
        float result = comparer.compare_two_requirements(r1, r2);
        Dependency aux = new Dependency(result,req1.getId(),req2.getId(),"proposed","similar",component);

        //TODO improve this part
        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";

        write_to_file(s,p);

        s = System.lineSeparator() + aux.print_json();

        write_to_file(s,p);

        s = System.lineSeparator() + "]}";

        write_to_file(s,p);
    }

    @Override
    public void similarityReqProj(String compare, float threshold, String filename, ReqProjOp input) throws InternalErrorException {

        List<RequirementId> requirements = input.getRequirements();
        List<RequirementId> project_requirements = input.getProject_requirements();

        List<Requirement> requirements_loaded = new ArrayList<>();
        List<Requirement> project_requirements_loaded = new ArrayList<>();

        for (RequirementId aux: requirements) {
            try {
                requirements_loaded.add(requirementDAO.getRequirement(aux.getId()));
            } catch (SQLException e) {
                //nothing
            }
        }

        for (RequirementId aux: project_requirements) {
            try {
                project_requirements_loaded.add(requirementDAO.getRequirement(aux.getId()));
            } catch (SQLException e) {
                //nothing
            }
        }

        ComparisonBetweenSentences comparer = new ComparisonBetweenSentences(greedyComparerWNLin,compare,threshold,true,component);

        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";

        write_to_file(s,p);

        boolean firsttimeComa = true;
        int cont = 0;
        String result = "";

        for (int i = 0; i < requirements_loaded.size(); ++i) {
            System.out.println(requirements_loaded.size() - i);
            Requirement req1 = requirements_loaded.get(i);
            for (Requirement req2 : project_requirements_loaded) {
                Dependency aux = comparer.compare_two_requirements_dep(req1,req2);
                if (aux != null) {
                    if (aux.getDependency_score() >= threshold && !comparer.existsDependency(aux.getFromid(), aux.getToid(),input.getDependencies())) {
                        s = System.lineSeparator() + aux.print_json();
                        if (!firsttimeComa) s = "," + s;
                        firsttimeComa = false;
                        result = result.concat(s);
                        ++cont;
                        if (cont >= 5000) {
                            write_to_file(result,p);
                            result = "";
                            cont = 0;
                        }
                    }
                }
            }
            project_requirements_loaded.add(req1);
        }

        if (!result.equals("")) write_to_file(result,p);

        s = System.lineSeparator() + "]}";
        write_to_file(s,p);
    }

    @Override
    public void similarityProj(String compare, float threshold, String filename, ProjOp input) throws InternalErrorException {

        int cont_left = 0;
        int max = input.getRequirements().size()*input.getRequirements().size()/2;
        int per = 0;

        show_time("start");

        List<RequirementId> requirements = input.getRequirements();

        //load reqs from db
        List<Requirement> requirements_loaded = new ArrayList<>();
        for (RequirementId aux: requirements) {
            try {
                requirements_loaded.add(requirementDAO.getRequirement(aux.getId()));
            } catch (SQLException e) {
                //nothing
            }
        }

        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";

        write_to_file(s,p);

        ComparisonBetweenSentences comparer = new ComparisonBetweenSentences(greedyComparerWNLin,compare,threshold,true,component);

        boolean firsttimeComa = true;
        int cont = 0;
        String result = "";


        for (int i = 0; i < requirements_loaded.size(); i++) {
            cont_left += requirements_loaded.size() - i - 1;
            int aux_left = cont_left*100/max;
            if (aux_left >= per + 10) {
                per = aux_left;
                show_time(aux_left+"%");
            }
            System.out.println(requirements_loaded.size() - i);
            Requirement req1 = requirements_loaded.get(i);
            for (int j = i + 1; j < requirements_loaded.size(); j++) {
                Requirement req2 = requirements_loaded.get(j);
                Dependency aux = comparer.compare_two_requirements_dep(req1,req2);
                if (aux != null) {
                    if (aux.getDependency_score() >= threshold && !comparer.existsDependency(aux.getFromid(), aux.getToid(), input.getDependencies())) {
                        s = System.lineSeparator() + aux.print_json();
                        if (!firsttimeComa) s = "," + s;
                        firsttimeComa = false;
                        result = result.concat(s);
                        ++cont;
                        if (cont >= 5000) {
                            write_to_file(result, p);
                            result = "";
                            cont = 0;
                        }
                    }
                }
            }
        }

        if (!result.equals("")) write_to_file(result,p);

        s = System.lineSeparator() + "]}";
        write_to_file(s,p);

        show_time("finish");
    }

    @Override
    public void similarityProj_Large(String compare, float threshold, String filename, ProjOp input) throws InternalErrorException{

        show_time("start");

        List<RequirementId> requirements = input.getRequirements();

        //load reqs from db
        List<Requirement> requirements_loaded = new ArrayList<>();
        for (RequirementId aux: requirements) {
            try {
                requirements_loaded.add(requirementDAO.getRequirement(aux.getId()));
            } catch (SQLException e) {
                //nothing
            }
        }

        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";

        write_to_file(s,p);

        ForkJoinPool commonPool = new ForkJoinPool(number_threads);
        LargeProjTask customRecursiveTask = new LargeProjTask(number_threads,threshold,0,number_threads,compare,requirements_loaded,input.getDependencies(),new_comparer(),p);
        commonPool.execute(customRecursiveTask);
        customRecursiveTask.join();

        delete_last_comma("../testing/output/"+filename);

        s = System.lineSeparator() + "]}";
        write_to_file(s,p);

        show_time("finish");
    }

    @Override
    public void similarityCluster(String compare, float threshold, String filename, String type, ProjOp input) throws InternalErrorException, BadRequestException {

        show_time("start");

        ComparisonBetweenSentences comparer = new ComparisonBetweenSentences(greedyComparerWNLin,compare,threshold,false,component);
        List<Cluster> clusters = new ArrayList<>();
        List<RequirementId> requirements = input.getRequirements();

        //load reqs form db
        List<Requirement> requirements_loaded = new ArrayList<>();
        for (RequirementId aux: requirements) {
            try {
                requirements_loaded.add(requirementDAO.getRequirement(aux.getId()));
            } catch (SQLException e) {
                //e.printStackTrace();
                //nothing
            }
        }

        if (requirements_loaded.size() <= 0) throw new BadRequestException("Input without requirements or without preprocessed requirements");

        requirements = null;

        Path p = Paths.get("../testing/output/"+filename);
        String s = System.lineSeparator() + "{\"dependencies\": [";

        write_to_file(s,p);


        int i = 0;

        if (!exist_clusters()) {
            List<Req_with_score> aux_list = new ArrayList<>();
            aux_list.add(new Req_with_score(requirements_loaded.get(0),threshold));
            Cluster aux_cluster = new Cluster(requirements_loaded.get(0),aux_list);
            clusters.add(aux_cluster);
            ++i;
        }

        while (i < requirements_loaded.size()) {
            Requirement requirement = requirements_loaded.get(i);
            System.out.println(i);
            ++i;
            float max = 0.0f;
            Cluster max_cluster = null;
            for (Cluster cluster: clusters) {
                float value = 0.0f;
                if (type.equals("all")) value = compare_requirement_cluster(cluster,requirement,comparer);
                else value = compare_requirement_cluster_only_one(cluster,requirement,comparer);
                if (value > max) {
                    max = value;
                    max_cluster = cluster;
                }
            }
            if (max > threshold && max_cluster != null) {
                max_cluster.addReq(requirement,max);
            } else {
                List<Req_with_score> aux_list = new ArrayList<>();
                aux_list.add(new Req_with_score(requirement,0));
                Cluster new_cluster = new Cluster(requirement,aux_list);
                clusters.add(new_cluster);
            }
        }

        show_time("finish1");
        System.out.println("number of clusters: "+clusters.size());

        int cont = 0;
        int number_clusters = 0;
        String result = "";
        boolean write_something = false;
        for (Cluster cluster: clusters) {
            if (cluster.getSpecifiedRequirements().size() > 1) {
                ++number_clusters;
                List<Req_with_score> reqs = cluster.getSpecifiedRequirements();
                for (Req_with_score req : reqs) {
                    if (!req.getRequirement().getId().equals(cluster.getReq_older().getId())) {
                        result = result.concat(System.lineSeparator() + create_dependency(req, cluster.getReq_older().getId()) + ",");
                        ++cont;
                        write_something = true;
                        if (cont > 2500) write_to_file(result, p);
                    }
                }
            }
        }

        System.out.println("not empty clusters: " + number_clusters);

        if (cont > 0) {
            write_to_file(result,p);
            write_something = true;
        }

        if (write_something) delete_last_comma("../testing/output/"+filename);

        s = System.lineSeparator() + "]}";
        write_to_file(s,p);

        aux_return_cluster(clusters);


        show_time("finish2");
    }

    private void aux_return_cluster(List<Cluster> clusters) throws InternalErrorException {

        String filename = "result_aux_clusters";
        Path p = Paths.get("../testing/output/"+filename);

        for (Cluster cluster: clusters) {
            List<Req_with_score> req_with_scores = cluster.getSpecifiedRequirements();
            for (Req_with_score score: req_with_scores) {
                Requirement requirement = score.getRequirement();
                String write = requirement.getId() + " " + requirement.getName() + " " + requirement.getText() +  System.lineSeparator();
                write_to_file(write,p);
            }
            String write = "----------------------------------------------------------";
            write_to_file(write,p);
        }

        String filename1 = "result_aux_clusters.json";
        Path p1 = Paths.get("../testing/output/"+filename1);

        JSONObject json = new JSONObject();
        JSONArray clusters_json = new JSONArray();
        for (Cluster cluster: clusters) {
            JSONObject cluster_json = new JSONObject();
            JSONArray specifiedReqs = new JSONArray();
            List<Req_with_score> req_with_scores = cluster.getSpecifiedRequirements();
            for (Req_with_score req: req_with_scores) {
                specifiedReqs.put(req.getRequirement().getId());
            }
            cluster_json.put("specifiedReqs",specifiedReqs);
            clusters_json.put(cluster_json);
        }
        json.put("clusters",clusters_json);
        write_to_file(json.toString(),p1);
    }


    private void delete_last_comma(String path) throws InternalErrorException{
        try {
            RandomAccessFile f = new RandomAccessFile(path, "rw");
            long length = f.length() - 1;
            f.setLength(length);
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new InternalErrorException(e.getMessage());
        }
    }



    @Override
    public JSONObject testing(String compare, String filename, PairReq input) throws SQLException, BadRequestException {

        RequirementId req1 = input.getReq1();
        RequirementId req2 = input.getReq2();
        if (req1.getId() == null || req2.getId() == null) throw new BadRequestException("One requirement has id equal to null");
        if (req1.getId().equals(req2.getId())) throw new BadRequestException("The requirements to be compared have the same id");
        Requirement r1 = requirementDAO.getRequirement(req1.getId());
        Requirement r2 = requirementDAO.getRequirement(req2.getId());
        ComparisonBetweenSentences comparer = new ComparisonBetweenSentences(greedyComparerWNLin,compare, 0,false,component);
        if (comparer.existsDependency(r1.getId(),r2.getId(),input.getDependencies())) throw new BadRequestException("Already exists another similar or duplicate dependency between the same requirements");
        float result = comparer.compare_two_requirements(r1,r2);
        Dependency aux = new Dependency(result,req1.getId(),req2.getId(),"proposed","similar",component);

        JSONObject finish = new JSONObject();
        try {
            finish.put("result", aux.getDependency_score());
        } catch (Exception e) {
            finish.put("result", 0);
        }
        return finish;
    }

    //Database
    @Override
    public void savePreprocessed(List<Requirement> reqs) throws SQLException {
        show_time("start");
        int i = 0;
        int aux_main = 0;
        for (Requirement r : reqs) {
            System.out.println(reqs.size() - i);
            int aux = i * 100 / reqs.size();
            if (aux >= aux_main + 10) {
                aux_main = aux;
                show_time(aux+"%");
            }
            r.compute_sentence();
            requirementDAO.savePreprocessed(r);
            ++i;
        }
        show_time("finish");
    }

    @Override
    public void clearDB() throws SQLException {
        requirementDAO.clearDB();
    }





    /*
    auxiliary operations
     */

    private String create_dependency(Req_with_score req, String fromid) {

        JSONObject result = new JSONObject();
        result.put("dependency_score",req.getScore());
        result.put("fromid",fromid);
        result.put("toid",req.getRequirement().getId());
        result.put("status","proposed");
        result.put("dependency_type","similar");
        JSONArray description = new JSONArray();
        description.put(component);
        result.put("description",description);

        return result.toString();
    }

    private float compare_requirement_cluster(Cluster cluster, Requirement requirement, ComparisonBetweenSentences comparer) {

        List<Req_with_score> cluster_requirements = cluster.getSpecifiedRequirements();
        float max = 0.0f;
        for (Req_with_score aux_requirement: cluster_requirements) {
            Requirement cluster_requirement = aux_requirement.getRequirement();
            Dependency aux = comparer.compare_two_requirements_dep(requirement,cluster_requirement);
            float value = 0.0f;
            if (aux != null) value = aux.getDependency_score();
            if (value > max) max = value;
        }
        return max;
    }

    private float compare_requirement_cluster_only_one(Cluster cluster, Requirement requirement, ComparisonBetweenSentences comparer) {

        Requirement cluster_requirement = cluster.getReq_older();
        Dependency aux = comparer.compare_two_requirements_dep(requirement,cluster_requirement);
        float value = 0.0f;
        if (aux != null) value = aux.getDependency_score();
        return value;
    }

    private boolean exist_clusters() {
        return false;
    }

    private void show_time(String text) {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int hour = now.getHour();
        int minute = now.getMinute();
        System.out.println(text + " -- " + hour + ":" + minute + "  " + month + "/" + day + "/" + year);
    }

    private void write_to_file(String text, Path p) throws InternalErrorException {
        try (BufferedWriter writer = Files.newBufferedWriter(p, StandardOpenOption.APPEND)) {
            writer.write(text);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new InternalErrorException("Write start to file fail");
        }
    }

    private Path new_path(int number, String filename) throws InternalErrorException {
        try {
            File file = new File("../testing/output/"+filename+"_"+number);
            if (file.createNewFile()) {
                System.out.println("file.txt File Created in Project root directory");
            } else System.out.println("File file.txt already exists in project root directory");
        } catch (IOException e) {
            e.printStackTrace();
            throw new InternalErrorException("Error creating file");
        }
        return Paths.get("../testing/output/"+filename+"_"+number);
    }

    private GreedyComparer new_comparer() {

        GreedyComparer greedyComparerWNLin = new GreedyComparer(wnMetricLin, 0.3f, true);

        Requirement aux1 = new Requirement();
        aux1.setName("testing");
        Requirement aux2 = new Requirement();
        aux2.setName("just waiting for an answer");
        aux1.compute_sentence();
        aux2.compute_sentence();

        greedyComparerWNLin.computeSimilarity(aux1.getSentence_name(), aux2.getSentence_name());

        return greedyComparerWNLin;
    }
}
