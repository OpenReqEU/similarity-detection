package upc.similarity.semilarapi.config;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Configuration {

    private static Configuration instance = new Configuration();

    private int number_threads = 1;

    private Configuration() {
        String result = "";
        try (BufferedReader br = new BufferedReader(new FileReader("semilar_variables"))) {
            String line;
            while ((line = br.readLine()) != null) {
                result = result.concat(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject json = new JSONObject(result);
        number_threads = json.getInt("number_threads");
        if (number_threads <= 0 || number_threads > 32) number_threads = 1;
    }

    public static Configuration getInstance() {
        return instance;
    }

    public int getNumber_threads() {
        return number_threads;
    }
}
