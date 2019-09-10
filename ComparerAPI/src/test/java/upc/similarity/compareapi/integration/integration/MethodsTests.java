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
import upc.similarity.compareapi.entity.Model;
import upc.similarity.compareapi.util.DatabaseOperations;
import upc.similarity.compareapi.util.Tfidf;
import upc.similarity.compareapi.util.Time;

import static java.time.Instant.ofEpochMilli;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Clock;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
public class MethodsTests {

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
        Tfidf.getInstance().setCutOffDummy(true);
    }

    @AfterClass
    public static void deleteTestDB() throws Exception {
        SQLiteDatabase db = new SQLiteDatabase();
        db.clearDatabase();
        File file = new File("../testing/integration/test_database/main.db");
        boolean result = file.delete();
    }


    /*
    Similarity without clusters
     */

    @Test
    public void buildModel() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"buildModel/input.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "buildModel/output.json")));
        ++id;
    }

    @Test
    public void buildModelAndCompute() throws Exception {
        this.mockMvc.perform(post(url + "BuildModelAndCompute").param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"buildModelAndCompute/input.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "buildModelAndCompute/output.json")));
        ++id;
    }

    @Test
    public void addRequirements() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"addRequirements/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "AddRequirements").param("organization", "UPC")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"addRequirements/input_reqs.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "addRequirements/outputAdd.json")));
        ++id;
        this.mockMvc.perform(post(url + "SimProject").param("organization", "UPC").param("threshold", "0")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"addRequirements/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "addRequirements/outputProj.json")));
        ++id;
    }

    @Test
    public void deleteRequirements() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"deleteRequirements/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "DeleteRequirements").param("organization", "UPC")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"deleteRequirements/input_reqs.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "deleteRequirements/outputAdd.json")));
        ++id;
        this.mockMvc.perform(post(url + "SimProject").param("organization", "UPC").param("threshold", "0")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"deleteRequirements/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "deleteRequirements/outputProj.json")));
        ++id;
    }

    @Test
    public void simReqReq() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqReq/input_model.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(post(url + "SimReqReq").param("organization", "UPC")
                .param("req1", "UPC-1").param("req2", "UPC-2"))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "simReqReq/output.json")));
        ++id;
    }

    @Test
    public void simReqOrganization() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqOrganization/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimReqOrganization").param("organization", "UPC").param("threshold", "0")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqOrganization/input_reqs.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simReqOrganization/output_threshold_0.json")));
        ++id;
        this.mockMvc.perform(post(url + "SimReqOrganization").param("organization", "UPC").param("threshold", "0.04")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqOrganization/input_reqs.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simReqOrganization/output_threshold_004.json")));
        ++id;
    }
    @Test
    public void simNewReqOrganization() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simNewReqOrganization/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimNewReqOrganization").param("organization", "UPC").param("threshold", "0")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simNewReqOrganization/input_reqs.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simNewReqOrganization/output.json")));
        ++id;
    }


    @Test
    public void simReqProject() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqProject/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimReqProject").param("organization", "UPC").param("threshold", "0")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"simReqProject/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simReqProject/output_threshold_0.json")));
        ++id;
        this.mockMvc.perform(post(url + "SimReqProject").param("organization", "UPC").param("threshold", "0.6")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"simReqProject/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simReqProject/output_threshold_06.json")));
        ++id;
    }

    @Test
    public void simProject() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simProject/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimProject").param("organization", "UPC").param("threshold", "0")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simProject/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simProject/output_threshold_0.json")));
        ++id;
        this.mockMvc.perform(post(url + "SimProject").param("organization", "UPC").param("threshold", "0.2")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simProject/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simProject/output_threshold_02.json")));
        ++id;
    }


    /*
    Similarity with clusters
     */

    @Test
    public void buildClusters() throws Exception {
        this.mockMvc.perform(post(url + "BuildClusters").param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"generateClusters/input.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "generateClusters/output_build.json")));
        ++id;
        SQLiteDatabase sqLiteDatabase = new SQLiteDatabase();
        List<Dependency> dependencies = sqLiteDatabase.getDependencies("UPC");
        assertEquals(read_file_array(path+"generateClusters/output_dependencies.json"),listDependenciesToJson(dependencies).toString());
    }

    @Test
    public void buildClustersAndCompute() throws Exception {
        this.mockMvc.perform(post(url + "BuildClustersAndCompute").param("organization", "UPC").param("threshold", "0.2")
                .param("compare", "true").param("responseId", id+"").param("maxNumber", "-1").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"generateClustersAndCompute/input.json")))
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
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"simReqClusters/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimReqClusters").param("organization", "UPC").param("maxValue", "-1")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqClusters/input_operation.json")))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "simReqClusters/output_all.json")));
        this.mockMvc.perform(post(url + "SimReqClusters").param("organization", "UPC").param("maxValue", "0")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqClusters/input_operation.json")))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "simReqClusters/output_only_accepted.json")));
        this.mockMvc.perform(post(url + "SimReqClusters").param("organization", "UPC").param("maxValue", "1")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqClusters/input_operation.json")))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "simReqClusters/output_only_one.json")));
    }

    @Test
    public void simReqClustersReqNotExist() throws Exception {
        this.mockMvc.perform(post(url + "BuildClusters").param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"simReqClusters/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimReqClusters").param("organization", "UPC").param("maxValue", "-1")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqClusters/input_operation_not_exist.json")))
                .andExpect(status().isNotFound());
    }

    @Test
    public void treatDependencies() throws Exception {
        this.mockMvc.perform(post(url + "BuildClusters").param("organization", "UPC").param("threshold", "1.1")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"treatDependencies/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "TreatAcceptedAndRejectedDependencies").param("organization", "UPC")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"treatDependencies/input_treat.json")))
                .andExpect(status().isOk());
        SQLiteDatabase sqLiteDatabase = new SQLiteDatabase();
        List<Dependency> dependencies = sqLiteDatabase.getDependencies("UPC");
        assertEquals(read_file_array(path+"treatDependencies/output_dependencies.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"treatDependencies/output_model.json"), extractModel("UPC",false, false));
    }

    @Test
    public void treatDependenciesWithLoop() throws Exception {
        this.mockMvc.perform(post(url + "BuildClusters").param("organization", "UPC").param("threshold", "1.1")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"treatDependencies/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "TreatAcceptedAndRejectedDependencies").param("organization", "UPC")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"treatDependencies/input_treat_loop.json")))
                .andExpect(status().isOk());
        SQLiteDatabase sqLiteDatabase = new SQLiteDatabase();
        List<Dependency> dependencies = sqLiteDatabase.getDependencies("UPC");
        assertEquals(read_file_array(path+"treatDependencies/output_dependencies_loop.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"treatDependencies/output_model_loop.json"), extractModel("UPC",false, false));
    }

    @Test
    public void treatDependenciesWithProposed() throws Exception {
        this.mockMvc.perform(post(url + "BuildClusters").param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"treatDependencies/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "TreatAcceptedAndRejectedDependencies").param("organization", "UPC")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"treatDependencies/input_treat.json")))
                .andExpect(status().isOk());
        SQLiteDatabase sqLiteDatabase = new SQLiteDatabase();
        List<Dependency> dependencies = sqLiteDatabase.getDependencies("UPC");
        assertEquals(read_file_array(path+"treatDependencies/output_dependencies_with_proposed.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"treatDependencies/output_model_with_proposed.json"), extractModel("UPC",false, false));
    }

    @Test
    public void batchProcess() throws Exception {
        this.mockMvc.perform(post(url + "BuildClusters").param("organization", "UPC").param("threshold", "1.1")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"cronMethod/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "BatchProcess").param("organization", "UPC").param("responseId", id+"")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"cronMethod/input_cron.json")))
                .andExpect(status().isOk());
        ++id;
        SQLiteDatabase sqLiteDatabase = new SQLiteDatabase();
        List<Dependency> dependencies = sqLiteDatabase.getDependencies("UPC");
        assertEquals(read_file_array(path+"cronMethod/output_dependencies.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"cronMethod/output_model.json"), extractModel("UPC",false, false));
    }

    @Test
    public void batchProcessLoop() throws Exception {
        this.mockMvc.perform(post(url + "BuildClusters").param("organization", "UPC").param("threshold", "1.1")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"cronMethod/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "BatchProcess").param("organization", "UPC").param("responseId", id+"")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"cronMethod/input_cron_loop.json")))
                .andExpect(status().isOk());
        ++id;
        SQLiteDatabase sqLiteDatabase = new SQLiteDatabase();
        List<Dependency> dependencies = sqLiteDatabase.getDependencies("UPC");
        assertEquals(read_file_array(path+"cronMethod/output_dependencies_loop.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"cronMethod/output_model_loop.json"), extractModel("UPC",false, false));
    }

    @Test
    public void batchProcessWithProposed() throws Exception {
        this.mockMvc.perform(post(url + "BuildClusters").param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"cronMethod/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "BatchProcess").param("organization", "UPC").param("responseId", id+"")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"cronMethod/input_cron.json")))
                .andExpect(status().isOk());
        ++id;
        SQLiteDatabase sqLiteDatabase = new SQLiteDatabase();
        List<Dependency> dependencies = sqLiteDatabase.getDependencies("UPC");
        assertEquals(read_file_array(path+"cronMethod/output_dependencies_proposed.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"cronMethod/output_model_proposed.json"), extractModel("UPC",false, false));
    }


    /*
    Auxiliary methods
     */

    @Test
    public void getOrganizationInfo() throws Exception {
        Time.getInstance().setClock(Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault()));
        this.mockMvc.perform(post(url + "BuildClusters").param("organization", "UPCTest").param("threshold", "0.12")
                .param("compare", "true").param("responseId", "Test0").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"getOrganizationInfo/input_model_with.json")))
                .andExpect(status().isOk());
        DatabaseOperations.getInstance().generateResponse("UPCTest","Test3","TestMethod");
        Time.getInstance().setClock(Clock.fixed(ofEpochMilli(40), ZoneId.systemDefault()));
        this.mockMvc.perform(get(url + "GetOrganizationInfo").param("organization", "UPCTest"))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "getOrganizationInfo/output_with_clusters.json")));
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPCTest")
                .param("compare", "false").param("responseId", "Test1").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"getOrganizationInfo/input_model_without.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetOrganizationInfo").param("organization", "UPCTest"))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "getOrganizationInfo/output_without_clusters.json")));
        Time.getInstance().setClock(Clock.systemUTC());
    }

    @Test
    public void deleteOrganizationResponses() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("threshold", "0.0")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"deleteResponses/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(delete(url + "ClearOrganizationResponses").param("organization", "UPC"))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteOrganizationData() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"deleteOrganizationData/input_model.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(post(url + "SimReqReq").param("organization", "UPC").param("req1", "UPC-1").param("req2","UPC-2"))
                .andExpect(status().isOk());
        this.mockMvc.perform(delete(url + "ClearOrganization").param("organization", "UPC"))
                .andExpect(status().isOk());
        this.mockMvc.perform(post(url + "SimReqReq").param("organization", "UPC").param("req1", "UPC-1").param("req2","UPC-2"))
                .andExpect(status().isNotFound());
        ++id;
    }

    @Test
    public void clearDatabase() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"clearDatabase/input_model.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(delete(url + "ClearDatabase"))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isNotFound());
        ++id;
    }


    /*
    Private methods
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

    private String extractModel(String organization, boolean withDocs, boolean withFrequency) throws Exception {
        Model model = DatabaseOperations.getInstance().loadModel(organization, null, withFrequency);
        JSONArray reqsArray = new JSONArray();
        if (withDocs) {
            Iterator it = model.getDocs().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                String id = (String) pair.getKey();
                HashMap<String, Double> words = (HashMap<String, Double>) pair.getValue();
                Iterator it2 = words.entrySet().iterator();
                JSONArray wordsArray = new JSONArray();
                while (it2.hasNext()) {
                    Map.Entry pair2 = (Map.Entry) it2.next();
                    String word = (String) pair2.getKey();
                    double value = (double) pair2.getValue();
                    JSONObject auxWord = new JSONObject();
                    auxWord.put("word", word);
                    auxWord.put("tfIdf", value);
                    wordsArray.put(auxWord);
                    it2.remove();
                }
                it.remove();
                JSONObject auxReq = new JSONObject();
                auxReq.put("id", id);
                auxReq.put("words", wordsArray);
                reqsArray.put(auxReq);
            }
        }
        JSONArray wordsFreq = new JSONArray();
        if (withFrequency) {
            Iterator it = model.getCorpusFrequency().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                String word = (String) pair.getKey();
                int value = (int) pair.getValue();
                JSONObject auxWord = new JSONObject();
                auxWord.put("word", word);
                auxWord.put("corpusTf", value);
                wordsFreq.put(auxWord);
                it.remove();
            }
        }
        JSONArray clusters = new JSONArray();
        Iterator it = model.getClusters().entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            int clusterId = (int) pair.getKey();
            List<String> clusterRequirements = (List<String>) pair.getValue();
            JSONArray requirementsArray = new JSONArray();
            for (String requirement: clusterRequirements) requirementsArray.put(requirement);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("clusterId", clusterId);
            jsonObject.put("clusterRequirements", requirementsArray);
            clusters.put(jsonObject);
        }
        JSONObject result = new JSONObject();
        result.put("corpus", reqsArray);
        result.put("corpusFrequency", wordsFreq);
        result.put("clusters", clusters);
        result.put("threshold", model.getThreshold());
        result.put("compare", model.isCompare());
        result.put("isCluster", model.hasClusters());
        result.put("lastClusterId", model.getLastClusterId());
        return result.toString();
    }


}
