package upc.similarity.compareapi.util;


import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import upc.similarity.compareapi.config.Control;
//import cern.colt.matrix.linalg.SingularValueDecomposition;

import java.util.HashMap;
import java.util.Map;

public class Svd {

    //TODO try jblas library performance vs apache vs colt

    /*DoubleMatrix2D s; //middle --> eigen values
    DoubleMatrix2D v; //right --> docs
    DoubleMatrix2D u; //left --> words*/

    RealMatrix s; //middle --> eigen values
    RealMatrix v; //right --> docs
    //RealMatrix u; //left --> words
    RealMatrix mult;

    //int numWords;
    //int numDocs;

    public HashMap<String,Integer> compute(Map<String, Map<String, Double>> docs, Map<String, Integer> corpusFrequency) {
        int numWords = corpusFrequency.size();
        int numDocs = docs.size();
        //double[][] aux = new double[numWords][numDocs];
        HashMap<String,Integer> docId = new HashMap<>();

        //DoubleMatrix2D matrix = new SparseDoubleMatrix2D(numWords,numDocs);
        RealMatrix matrix = new OpenMapRealMatrix(numWords, numDocs);

        int id = 0;
        for (Map.Entry<String, Integer> entry : corpusFrequency.entrySet()) {
            entry.setValue(id);
            ++id;
        }

        int column = 0;
        //TODO do loop through rows and not columns
        for (Map.Entry<String, Map<String, Double>> doc : docs.entrySet()) {
            String docName = doc.getKey();
            docId.put(docName, column);
            Map<String, Double> words = doc.getValue();
            for (Map.Entry<String,Double> word: words.entrySet()) {
                String name = word.getKey();
                double value = word.getValue();
                int row = corpusFrequency.get(name);
                //matrix.setQuick(row,column,value);
                matrix.addToEntry(row, column, value);
                //aux[row][column] = value;
            }
            ++column;
        }

        /*int[][] meh = {{1,0,1,0,0,0},{1,1,0,0,0,0},{0,1,0,0,0,0},{0,1,1,0,0,1},{0,0,0,1,0,0},{0,0,1,1,0,1},{0,0,0,1,0,0},{0,0,0,1,1,0}};

        for (int i = 0; i < meh.length; ++i) {
            for (int j = 0; j < meh[i].length; ++j) {
                matrix.addToEntry(i,j,meh[i][j]);
            }
        }*/

        /*SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
        s = svd.getS();
        v = svd.getV();
        u = svd.getU();*/

        Control.getInstance().showInfoMessage("Start svd");
        SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
        Control.getInstance().showInfoMessage("Finish svd");
        s = svd.getS();
        v = svd.getVT();

        mult = s.multiply(v);

        return docId;
    }

    public void truncate(int k) {
        /*int[] kIndexes = new int[k];
        for (int i = 0; i < k; ++i) kIndexes[i] = i;

        int[] rowIndexes = new int[u.rows()];
        for (int i = 0; i < u.rows(); ++i) rowIndexes[i] = i;
        u = u.viewSelection(rowIndexes, kIndexes);

        s = s.viewSelection(kIndexes, kIndexes);

        int[] columnIndexes = new int[v.columns()];
        for (int i = 0; i < v.columns(); ++i) columnIndexes[i] = i;
        v = v.viewSelection(kIndexes, columnIndexes);*/

        //u = u.getSubMatrix(0, u.getRowDimension()-1, 0, k-1);
        s = s.getSubMatrix(0, k-1, 0, k-1);
        v = v.getSubMatrix(0, k-1, 0, v.getColumnDimension()-1);

        mult = s.multiply(v);
    }

    public double compare(int doc1, int doc2) {
        double[] doc1Values = mult.getColumn(doc1);
        double[] doc2Values = mult.getColumn(doc2);
        return CosineSimilarity.getInstance().compute(doc1Values, doc2Values);
    }
}
