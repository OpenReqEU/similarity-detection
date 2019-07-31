package upc.similarity.compareapi.integration.unit;




import java.util.HashMap;
import java.util.Map;

public class DriverSvd {

    public static void main(String[] args) {
        /*Map<String, Map<String, Double>> docs = new HashMap<>();

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
        corpusFrequency.put("romeo", 0);
        corpusFrequency.put("juliet", 0);
        corpusFrequency.put("happy", 0);
        corpusFrequency.put("dagger", 0);
        corpusFrequency.put("live", 0);
        corpusFrequency.put("die", 0);
        corpusFrequency.put("free", 0);
        corpusFrequency.put("new-hampshire", 0);

        SvdApache svd = new SvdApache();
        Map<String,Integer> map = svd.compute(docs, corpusFrequency);
        svd.truncate(2);

        double value1 = svd.compare(map.get("q"), map.get("doc3"));
        double value2 = svd.compare(map.get("q"), map.get("doc2"));
        double value3 = svd.compare(map.get("q"), map.get("doc4"));
        double value4 = svd.compare(map.get("q"), map.get("doc1"));
        double value5 = svd.compare(map.get("q"), map.get("doc5"));
        double value6 = svd.compare(map.get("q"), map.get("q"));

        /*double value1 = svd.compare(5,2);
        double value2 = svd.compare(5,1);
        double value3 = svd.compare(5,3);
        double value4 = svd.compare(5,0);
        double value5 = svd.compare(5,4);
        double value6 = svd.compare(5,5);*/

        System.out.println("meh");
    }
}
