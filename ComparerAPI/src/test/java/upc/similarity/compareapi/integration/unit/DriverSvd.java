package upc.similarity.compareapi.integration.unit;




import edu.ucla.sspace.lsa.LatentSemanticAnalysis;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SVD;
import edu.ucla.sspace.matrix.SparseHashMatrix;
import upc.similarity.compareapi.util.CosineSimilarity;

import java.util.HashMap;
import java.util.Map;

public class DriverSvd {

    public static void main(String[] args) {
        Map<String, Map<String, Double>> docs = new HashMap<>();

        Map<String, Double> doc = new HashMap<>();
        doc.put("romeo", 1.0);
        doc.put("juliet", 1.0);
        docs.put("doc1", doc);

        doc = new HashMap<>();
        doc.put("juliet", 1.0);
        doc.put("happy", 1.0);
        doc.put("dagger", 1.0);
        docs.put("doc2", doc);

        doc = new HashMap<>();
        doc.put("romeo", 1.0);
        doc.put("dagger", 1.0);
        doc.put("die", 1.0);
        docs.put("doc3", doc);

        doc = new HashMap<>();
        doc.put("live", 1.0);
        doc.put("die", 1.0);
        doc.put("free", 1.0);
        doc.put("new-hampshire", 1.0);
        docs.put("doc4", doc);

        doc = new HashMap<>();
        doc.put("new-hampshire", 1.0);
        docs.put("doc5", doc);

        doc = new HashMap<>();
        doc.put("dagger", 1.0);
        doc.put("die", 1.0);
        docs.put("q", doc);

        Map<String, Integer> corpusFrequency = new HashMap<>();
        Map<String, Integer> wordIndex = new HashMap<>();
        corpusFrequency.put("romeo", 0);
        wordIndex.put("romeo",0);
        corpusFrequency.put("juliet", 0);
        wordIndex.put("juliet",1);
        corpusFrequency.put("happy", 0);
        wordIndex.put("happy",2);
        corpusFrequency.put("dagger", 0);
        wordIndex.put("dagger",3);
        corpusFrequency.put("live", 0);
        wordIndex.put("live",4);
        corpusFrequency.put("die", 0);
        wordIndex.put("die",5);
        corpusFrequency.put("free", 0);
        wordIndex.put("free",6);
        corpusFrequency.put("new-hampshire", 0);
        wordIndex.put("new-hampshire",7);

        SVD.Algorithm algorithm = SVD.Algorithm.SVDLIBC;
        Matrix matrix = new SparseHashMatrix(corpusFrequency.size(), docs.size());
        double[][] test = new double[corpusFrequency.size()][docs.size()];

        int docId = 0;
        for(Map.Entry<String, Map<String, Double>> entry : docs.entrySet()) {
            for(Map.Entry<String, Double> word : entry.getValue().entrySet()) {
                int index = wordIndex.get(word.getKey());
                matrix.set(index,docId,word.getValue());
                test[index][docId] = word.getValue();
            }
            ++docId;
        }

        test[0] = new double[]{1,0,1,0,0,0};
        test[1] = new double[]{1,1,0,0,0,0};
        test[2] = new double[]{0,1,0,0,0,0};
        test[3] = new double[]{0,1,1,0,0,1};
        test[4] = new double[]{0,0,0,1,0,0};
        test[5] = new double[]{0,0,1,1,0,1};
        test[6] = new double[]{0,0,0,1,0,0};
        test[7] = new double[]{0,0,0,1,1,0};

        for (int i = 0; i < corpusFrequency.size(); ++i) {
            for (int j = 0; j < docs.size(); ++j) {
                double value = test[i][j];
                matrix.set(i,j,value);
            }
        }


        Matrix[] matrices = SVD.svd(matrix,algorithm,2);

        Matrix S = matrices[0];
        Matrix Z = matrices[1];
        Matrix U = matrices[2];

        double[][] SM = S.toDenseArray();
        double[][] ZM = Z.toDenseArray();
        double[][] CM = U.toDenseArray();

        Matrix result = Matrices.multiply(Z,U);

        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();
        double q_d1 = cosineSimilarity.compute(result.getColumn(5), result.getColumn(0));
        double q_d2 = cosineSimilarity.compute(result.getColumn(5), result.getColumn(1));
        double q_d3 = cosineSimilarity.compute(result.getColumn(5), result.getColumn(2));
        double q_d4 = cosineSimilarity.compute(result.getColumn(5), result.getColumn(3));
        double q_d5 = cosineSimilarity.compute(result.getColumn(5), result.getColumn(4));

        System.out.println("meh");
    }
}
