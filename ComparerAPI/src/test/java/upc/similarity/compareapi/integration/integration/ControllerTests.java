package upc.similarity.compareapi.integration.integration;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import upc.similarity.compareapi.dao.SQLiteDatabase;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.util.Tfidf;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
public class ControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @LocalServerPort
    int port = 9405;

    private String path = "../testing/integration/compare_component/";
    private String url = "/upc/Compare/";
    private static int id = 0;

    @BeforeClass
    public static void createTestDB() throws Exception {
        SQLiteDatabase.setDbPath("../testing/integration/test_database/");
        SQLiteDatabase db = new SQLiteDatabase();
        db.clearDatabase();
        Tfidf.setCutOffDummy(true);
    }

    @AfterClass
    public static void deleteTestDB() throws Exception {
        SQLiteDatabase db = new SQLiteDatabase();
        db.clearDatabase();
        File file = new File("../testing/integration/test_database/main.db");
        boolean result = file.delete();
    }

    @Test
    public void buildModel() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("threshold", "0.0")
                .param("compare", "true").param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"buildModel/input.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "buildModel/output.json")));
        ++id;
    }

    @Test
    public void addRequirements() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("threshold","0")
                .param("compare", "true").param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"addRequirements/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "AddRequirements").param("organization", "UPC")
                .param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"addRequirements/input_reqs.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "addRequirements/outputAdd.json")));
        ++id;
        this.mockMvc.perform(post(url + "SimProject").param("organization", "UPC")
                .param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"addRequirements/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "addRequirements/outputProj.json")));
        ++id;
    }

    @Test
    public void deleteRequirements() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"deleteRequirements/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "DeleteRequirements").param("organization", "UPC")
                .param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"deleteRequirements/input_reqs.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "deleteRequirements/outputAdd.json")));
        ++id;
        this.mockMvc.perform(post(url + "SimProject").param("organization", "UPC")
                .param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"deleteRequirements/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "deleteRequirements/outputProj.json")));
        ++id;
    }

    @Test
    public void buildModelAndCompute() throws Exception {
        this.mockMvc.perform(post(url + "BuildModelAndCompute").param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"buildModelAndCompute/input.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "buildModelAndCompute/output.json")));
        ++id;
    }

    @Test
    public void simReqOrganization() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("threshold", "0.0")
                .param("compare", "true").param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqOrganization/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimReqOrganization").param("organization", "UPC")
                .param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqOrganization/input_reqs.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simReqOrganization/output.json")));
        ++id;
    }

    @Test
    public void simReqReq() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("threshold", "0.0")
                .param("compare", "true").param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqReq/input_model.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(post(url + "SimReqReq").param("organization", "UPC")
                .param("req1", "UPC-1").param("req2", "UPC-2"))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "simReqReq/output.json")));
        ++id;
    }

    @Test
    public void simReqProject() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("threshold", "0.0")
                .param("compare", "true").param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqProject/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimReqProject").param("organization", "UPC")
                .param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"simReqProject/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simReqProject/output.json")));
        ++id;
    }

    @Test
    public void simProject() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("threshold", "0.0")
                .param("compare", "true").param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simProject/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimProject").param("organization", "UPC")
                .param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simProject/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simProject/output.json")));
        ++id;
    }

    @Test
    public void addClusters() throws Exception {
        this.mockMvc.perform(post(url + "BuildClusters").param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"generateClusters/input.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "generateClusters/output_build.json")));
        ++id;
        SQLiteDatabase sqLiteDatabase = new SQLiteDatabase();
        List<Dependency> dependencies = sqLiteDatabase.getDependencies("UPC");
        assertEquals(read_file_array(path+"generateClusters/output_dependencies.json"),listDependenciesToJson(dependencies).toString());
    }

    @Test
    public void addClustersAndCompute() throws Exception {
        this.mockMvc.perform(post(url + "BuildClustersAndCompute").param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"generateClustersAndCompute/input.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "generateClustersAndCompute/output.json")));
        ++id;
        SQLiteDatabase sqLiteDatabase = new SQLiteDatabase();
        List<Dependency> dependencies = sqLiteDatabase.getDependencies("UPC");
        assertEquals(read_file_array(path+"generateClustersAndCompute/output_dependencies.json"),listDependenciesToJson(dependencies).toString());
    }

    @Test
    public void simReqClusters() throws Exception {
        this.mockMvc.perform(post(url + "BuildClusters").param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"simReqClusters/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimReqClusters").param("organization", "UPC").param("maxValue", "0")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqClusters/input_operation.json")))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "simReqClusters/output.json")));
    }

    @Test
    public void treatDependencies() throws Exception {
        this.mockMvc.perform(post(url + "BuildClusters").param("organization", "UPC").param("threshold", "1.1")
                .param("compare", "true").param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"treatDependencies/input_model.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(post(url + "TreatAcceptedAndRejectedDependencies").param("organization", "UPC")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"treatDependencies/input_treat.json")))
                .andExpect(status().isOk());
        SQLiteDatabase sqLiteDatabase = new SQLiteDatabase();
        List<Dependency> dependencies = sqLiteDatabase.getDependencies("UPC");
        assertEquals(read_file_array(path+"treatDependencies/output_dependencies.json"),listDependenciesToJson(dependencies).toString());
    }

    @Test
    public void clearOrganizationResponses() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("threshold", "0.0")
                .param("compare", "true").param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"deleteResponses/input_model.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(delete(url + "ClearOrganizationResponses").param("organization", "UPC"))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isNotFound());
        ++id;
    }

    @Test
    public void clearDatabase() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("filename", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"clearDatabase/input_model.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(delete(url + "ClearDatabase"))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isNotFound());
        ++id;
    }


    /*
    Auxiliary operations
     */

    private String read_file_json(String path) throws Exception {
        String result = "";
        String line = "";
        try(FileReader fileReader = new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            while ((line = bufferedReader.readLine()) != null) {
                result = result.concat(line);
            }
            JSONObject aux = new JSONObject(result);
            return aux.toString();
        }
    }

    private String read_file_array(String path) throws Exception {
        String result = "";
        String line = "";
        try(FileReader fileReader = new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            while ((line = bufferedReader.readLine()) != null) {
                result = result.concat(line);
            }
            JSONArray aux = new JSONArray(result);
            return aux.toString();
        }
    }

    private String read_file_raw(String path) throws Exception {
        String result = "";
        String line = "";
        try(FileReader fileReader =new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            while ((line = bufferedReader.readLine()) != null) {
                result = result.concat(line);
            }
            return result;
        }
    }

    private JSONArray listDependenciesToJson(List<Dependency> dependencies) throws Exception {

        JSONArray jsonDeps = new JSONArray();

        for (Dependency dep: dependencies) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("toid",dep.getToid());
            jsonObject.put("fromid", dep.getFromid());
            jsonObject.put("status", dep.getStatus());
            jsonObject.put("score", dep.getDependencyScore());
            jsonObject.put("clusterId", dep.getClusterId());
            jsonDeps.put(jsonObject);
        }

        return jsonDeps;
    }


}
