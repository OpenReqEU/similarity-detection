package upc.similarity.semilarapi.entity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Model {

    private Map<String, Map<String, Double>> model;
    private Map<String, Integer> corpusFrequency;

    public Model(Map<String, Map<String, Double>> model, Map<String, Integer> corpusFrequency) {
        this.model = model;
        this.corpusFrequency = corpusFrequency;
    }

    public Model(String model, String corpusFrequency) {
        this.model = model_toMap(model);
        this.corpusFrequency = corpus_toMap(corpusFrequency);
    }

    public Map<String, Map<String, Double>> getModel() {
        return model;
    }

    public Map<String, Integer> getCorpusFrequency() {
        return corpusFrequency;
    }



    /*
    Map -> String
     */

    public String corpusFrequency_toJSON() {
        JSONArray result = new JSONArray();
        Iterator it = corpusFrequency.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            JSONObject aux = new JSONObject();
            aux.put("id",pair.getKey());
            aux.put("value",pair.getValue());
            result.put(aux);
            it.remove();
        }
        return result.toString();
    }

    public String model_toJSON() {
        JSONArray result = new JSONArray();
        Iterator it = model.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            JSONObject aux = new JSONObject();
            aux.put("id",pair.getKey());
            aux.put("value",aux_conversion_toJSON((Map)pair.getValue()));
            result.put(aux);
            it.remove();
        }
        return result.toString();
    }

    private JSONArray aux_conversion_toJSON(Map<String, Double> map) {
        JSONArray result = new JSONArray();
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            JSONObject aux = new JSONObject();
            aux.put("id",pair.getKey());
            aux.put("value",pair.getValue());
            result.put(aux);
            it.remove();
        }
        return result;
    }

    /*
    String -> Map
     */

    private Map<String, Map<String, Double>> model_toMap(String model) {
        Map<String, Map<String, Double>> result = new HashMap<>();
        JSONArray json = new JSONArray(model);
        for (int i = 0; i < json.length(); ++i) {
            JSONObject aux = json.getJSONObject(i);
            String id = aux.getString("id");
            Map<String, Double> map = aux_conversion_toMap(aux.getJSONArray("value"));
            result.put(id,map);
        }
        return result;
    }

    private Map<String, Integer> corpus_toMap(String corpus) {
        Map<String, Integer> result = new HashMap<>();
        JSONArray json = new JSONArray(corpus);
        for (int i = 0; i < json.length(); ++i) {
            JSONObject aux = json.getJSONObject(i);
            String id = aux.getString("id");
            Integer value = aux.getInt("value");
            result.put(id,value);
        }
        return result;
    }

    private Map<String, Double> aux_conversion_toMap(JSONArray json) {
        Map<String, Double> result = new HashMap<>();
        for (int i = 0; i < json.length(); ++i) {
            JSONObject aux = json.getJSONObject(i);
            String id = aux.getString("id");
            Double value = aux.getDouble("value");
            result.put(id,value);
        }
        return result;
    }


}
