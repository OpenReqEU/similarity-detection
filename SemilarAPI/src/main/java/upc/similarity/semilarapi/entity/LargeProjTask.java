package upc.similarity.semilarapi.entity;

import semilar.sentencemetrics.GreedyComparer;
import semilar.tools.semantic.WordNetSimilarity;
import semilar.wordmetrics.WNWordMetric;
import upc.similarity.semilarapi.service.ComparisonBetweenSentences;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class LargeProjTask extends RecursiveAction {

    static {
        wnMetricLin = new WNWordMetric(WordNetSimilarity.WNSimMeasure.LIN, false);
    }

    private int n;
    private int threshold_s;
    private float threshold;
    private int number_threads;
    private String compare;
    private List<Requirement> requirements_loaded;
    private List<Dependency> dependencies;
    private static WNWordMetric wnMetricLin;
    private ComparisonBetweenSentences comparer;
    private Path p;

    public LargeProjTask(int threshold_s, float threshold, int n, int number_threads, String compare, List<Requirement> requirements_loaded, List<Dependency> dependencies, GreedyComparer greedyComparerWNLin, Path p) {
        this.n = n;
        this.threshold_s = threshold_s;
        this.threshold = threshold;
        this.number_threads = number_threads;
        this.compare = compare;
        this.requirements_loaded = requirements_loaded;
        this.dependencies = dependencies;
        this.p = p;
        this.comparer = new ComparisonBetweenSentences(greedyComparerWNLin,compare,threshold,true,"Similarity_Semilar");
    }

    @Override
    protected void compute() {
        if (threshold_s > 1) {
            ForkJoinTask.invokeAll(createSubtasks());
        } else {
            processing();
        }
    }

    private List<LargeProjTask> createSubtasks() {
        List<LargeProjTask> subtasks = new ArrayList<>();
        for (int i = 0; i < number_threads; ++i) subtasks.add(new LargeProjTask(1,threshold,i,number_threads,compare,requirements_loaded,dependencies,new_comparer(),p));
        return subtasks;
    }

    private void processing() {
        //System.out.println("Enter");
        int cont = 0;
        String result = "";
        for (int i = 0; i < requirements_loaded.size(); i++) {
            if ((i % number_threads) == n) {
                System.out.println(requirements_loaded.size() - i);
                Requirement req1 = requirements_loaded.get(i);
                for (int j = i + 1; j < requirements_loaded.size(); j++) {
                    Requirement req2 = requirements_loaded.get(j);
                    Dependency aux = comparer.compare_two_requirements_dep(req1,req2);
                    if (aux != null) {
                        if (aux.getDependency_score() >= threshold && !comparer.existsDependency(aux.getFromid(), aux.getToid(), dependencies)) {
                            String s = System.lineSeparator() + aux.print_json();
                             s = s + ",";
                            result = result.concat(s);
                            ++cont;
                            if (cont >= 2500) {
                                write_to_file(result, p);
                                result = "";
                                cont = 0;
                            }
                        }
                    }
                }
                i += number_threads - 1;
            }
        }
        if (!result.equals("")) write_to_file(result,p);
    }


    private void write_to_file(String text, Path p) throws InternalError {
        try (BufferedWriter writer = Files.newBufferedWriter(p, StandardOpenOption.APPEND)) {
            writer.write(text);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new InternalError("Write start to file fail");
        }
    }

    private GreedyComparer new_comparer() {

        /*GreedyComparer greedyComparerWNLin = new GreedyComparer(wnMetricLin, 0.3f, true);

        /*Requirement aux1 = new Requirement();
        aux1.setName("testing");
        Requirement aux2 = new Requirement();
        aux2.setName("just waiting for an answer");
        aux1.compute_sentence();
        aux2.compute_sentence();

        greedyComparerWNLin.computeSimilarity(aux1.getSentence_name(), aux2.getSentence_name());*/

        return new GreedyComparer(wnMetricLin, 0.3f, true);
    }

}
