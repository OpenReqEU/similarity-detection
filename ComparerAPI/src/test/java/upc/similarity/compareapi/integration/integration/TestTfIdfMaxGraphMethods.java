package upc.similarity.compareapi.integration.integration;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import upc.similarity.compareapi.algorithms.clusters_algorithm.ClustersAlgorithm;
import upc.similarity.compareapi.algorithms.clusters_algorithm.max_graph.ClustersAlgorithmMaxGraph;
import upc.similarity.compareapi.algorithms.clusters_algorithm.max_graph.ClustersModelMaxGraph;
import upc.similarity.compareapi.config.Constants;
import upc.similarity.compareapi.dao.DatabaseModel;
import upc.similarity.compareapi.dao.SQLiteDatabase;
import upc.similarity.compareapi.dao.algorithm_models_dao.clusters_algorithm.max_graph.ClustersModelDatabaseMaxGraph;
import upc.similarity.compareapi.dao.algorithm_models_dao.similarity_algorithm.SimilarityModelDatabase;
import upc.similarity.compareapi.dao.algorithm_models_dao.similarity_algorithm.tf_idf.SimilarityModelDatabaseTfIdf;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.OrganizationModels;
import upc.similarity.compareapi.algorithms.preprocess.PreprocessPipeline;
import upc.similarity.compareapi.algorithms.preprocess.PreprocessPipelineDefault;
import upc.similarity.compareapi.algorithms.similarity_algorithm.SimilarityAlgorithm;
import upc.similarity.compareapi.algorithms.similarity_algorithm.tf_idf.SimilarityAlgorithmTfIdf;
import upc.similarity.compareapi.algorithms.similarity_algorithm.tf_idf.SimilarityModelTfIdf;
import upc.similarity.compareapi.util.Time;

import static java.time.Instant.ofEpochMilli;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
public class TestTfIdfMaxGraphMethods {

    @Autowired
    private MockMvc mockMvc;

    @LocalServerPort
    int port = 9405;

    private String path = "../testing/integration/compare_component/";
    private String url = "/upc/Compare/";
    private static int id = 0;
    private static Constants constants = null;
    private static ClustersModelDatabaseMaxGraph clustersModelDatabaseMaxGraph = null;

    @BeforeClass
    public static void createTestDB() throws Exception {
        PreprocessPipeline preprocessPipeline = new PreprocessPipelineDefault();
        SimilarityAlgorithm similarityAlgorithm = new SimilarityAlgorithmTfIdf(-1,false,false);
        SimilarityModelDatabase similarityModelDatabase = new SimilarityModelDatabaseTfIdf();
        clustersModelDatabaseMaxGraph = new ClustersModelDatabaseMaxGraph();
        ClustersAlgorithm clustersAlgorithm = new ClustersAlgorithmMaxGraph(clustersModelDatabaseMaxGraph);
        DatabaseModel databaseModel = new SQLiteDatabase("../testing/integration/test_database/",1,similarityModelDatabase, clustersModelDatabaseMaxGraph);
        constants = Constants.getInstance();
        constants.changeConfiguration(20000, 300, preprocessPipeline, similarityAlgorithm,clustersAlgorithm, databaseModel);
        constants.getDatabaseModel().clearDatabase();
    }

    @AfterClass
    public static void deleteTestDB() throws Exception {
        constants.getDatabaseModel().clearDatabase();
        File file = new File("../testing/integration/test_database/main.db");
        boolean result = file.delete();
    }

    @Before
    public void deleteUPCModel() throws Exception {
        this.mockMvc.perform(delete(url + "ClearOrganization").param("organization", "UPC"));
    }

    /*
    Similarity without clusters
     */

    @Test
    public void buildModel() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"buildModel/input.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "buildModel/output.json")));
        ++id;
    }

    @Test
    public void buildModelForbidden() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"buildModel/input.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"buildModel/input.json")))
                .andExpect(status().isForbidden());
        ++id;
    }

    @Test
    public void buildModelAndCompute() throws Exception {
        this.mockMvc.perform(post(url + "BuildModelAndCompute").param("useComponent","false").param("organization", "UPC").param("threshold", "0").param("maxDeps", "0")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"buildModelAndCompute/input.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "buildModelAndCompute/output.json")));
        ++id;
    }

    @Test
    public void addRequirements() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"addRequirements/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "AddRequirements").param("organization", "UPC")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"addRequirements/input_reqs.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "addRequirements/output_add.json")));
        ++id;
        assertEquals(read_file_json(path+"addRequirements/output_model.json"), extractModel("UPC",true, true));
    }

    @Test
    public void deleteRequirements() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"deleteRequirements/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "DeleteRequirements").param("organization", "UPC")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"deleteRequirements/input_reqs.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "deleteRequirements/output_delete.json")));
        ++id;
        assertEquals(read_file_json(path+"deleteRequirements/output_model.json"), extractModel("UPC",true, true));
    }

    @Test
    public void simReqReq() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqReq/input_model.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(post(url + "SimReqReq").param("organization", "UPC")
                .param("req1", "UPC-1").param("req2", "UPC-2"))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "simReqReq/output.json")));
        ++id;
    }

    @Test
    public void simReqReqWithComponent() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","true")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqReqWithComponent/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimReqReq").param("organization", "UPC")
                .param("req1", "UPC-1").param("req2", "UPC-2"))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "simReqReqWithComponent/output_1-2.json")));
        this.mockMvc.perform(post(url + "SimReqReq").param("organization", "UPC")
                .param("req1", "UPC-1").param("req2", "UPC-3"))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "simReqReqWithComponent/output_1-3.json")));
        this.mockMvc.perform(post(url + "SimReqReq").param("organization", "UPC")
                .param("req1", "UPC-1").param("req2", "UPC-4"))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "simReqReqWithComponent/output_1-4.json")));
        this.mockMvc.perform(post(url + "SimReqReq").param("organization", "UPC")
                .param("req1", "UPC-1").param("req2", "UPC-5"))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "simReqReqWithComponent/output_1-5.json")));
        this.mockMvc.perform(post(url + "SimReqReq").param("organization", "UPC")
                .param("req1", "UPC-1").param("req2", "UPC-6"))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "simReqReqWithComponent/output_1-6.json")));
    }

    @Test
    public void simReqOrganization() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqOrganization/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimReqOrganization").param("organization", "UPC").param("threshold", "0").param("maxDeps", "0")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqOrganization/input_reqs.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simReqOrganization/output_threshold_0.json")));
        ++id;
        this.mockMvc.perform(post(url + "SimReqOrganization").param("organization", "UPC").param("threshold", "0.04").param("maxDeps", "0")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqOrganization/input_reqs.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simReqOrganization/output_threshold_004.json")));
        ++id;
    }

    @Test
    public void simReqOrganizationMaxDeps() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqOrganization/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimReqOrganization").param("organization", "UPC").param("threshold", "0").param("maxDeps", "5")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqOrganization/input_reqs.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simReqOrganization/output_max_5.json")));
        ++id;
    }

    @Test
    public void simNewReqOrganization() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simNewReqOrganization/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimNewReqOrganization").param("organization", "UPC").param("threshold", "0").param("maxDeps", "0")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simNewReqOrganization/input_reqs.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simNewReqOrganization/output.json")));
        ++id;
    }


    @Test
    public void simReqProject() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqProject/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimReqProject").param("organization", "UPC").param("threshold", "0").param("maxDeps", "0")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"simReqProject/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simReqProject/output_threshold_0.json")));
        ++id;
        this.mockMvc.perform(post(url + "SimReqProject").param("organization", "UPC").param("threshold", "0.6").param("maxDeps", "0")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"simReqProject/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simReqProject/output_threshold_06.json")));
        ++id;
    }

    @Test
    public void simProject() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simProject/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimProject").param("organization", "UPC").param("threshold", "0").param("maxDeps", "0")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simProject/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simProject/output_threshold_0.json")));
        ++id;
        this.mockMvc.perform(post(url + "SimProject").param("organization", "UPC").param("threshold", "0.2").param("maxDeps", "0")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simProject/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simProject/output_threshold_02.json")));
        ++id;
    }

    @Test
    public void simProjectMaxDeps() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simProject/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimProject").param("organization", "UPC").param("threshold", "0.03").param("maxDeps", "2")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simProject/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simProject/output_max.json")));
        ++id;
    }

    @Test
    public void simProjectProject() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
                .param("compare", "true").param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simProjectProject/input_model.json")))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimProjectProject").param("organization", "UPC").param("threshold", "0").param("maxDeps", "0")
                .param("responseId", id+"").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"simProjectProject/input_operation.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "simProjectProject/output.json")));
        ++id;
    }


    /*
    Similarity with clusters
     */

    @Test
    public void buildClusters() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", new FileInputStream(new File(path+"buildClusters/input.json")));
        this.mockMvc.perform(multipart(url + "BuildClusters").file(multipartFile).param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("responseId", id+"").param("useComponent","false")
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "buildClusters/output_build.json")));
        ++id;
        List<Dependency> dependencies = clustersModelDatabaseMaxGraph.getDependencies("UPC");
        assertEquals(read_file_array(path+"buildClusters/output_dependencies.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"buildClusters/output_model.json"), extractModel("UPC",true, false));
    }

    @Test
    public void buildClustersWithRejected() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", new FileInputStream(new File(path+"buildClusters/input_rejected.json")));
        this.mockMvc.perform(multipart(url + "BuildClusters").file(multipartFile).param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("responseId", id+"").param("useComponent","false")
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "buildClusters/output_build.json")));
        ++id;
        List<Dependency> dependencies = clustersModelDatabaseMaxGraph.getDependencies("UPC");
        assertEquals(read_file_array(path+"buildClusters/output_dependencies_rejected.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"buildClusters/output_model_rejected.json"), extractModel("UPC",true, false));
    }

    @Test
    public void buildClustersAndCompute() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", new FileInputStream(new File(path+"buildClustersAndCompute/input.json")));
        this.mockMvc.perform(multipart(url + "BuildClustersAndCompute").file(multipartFile).param("organization", "UPC").param("threshold", "0.2")
                .param("compare", "true").param("responseId", id+"").param("maxNumber", "-1").param("useComponent","false"))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetResponsePage").param("organization", "UPC").param("responseId", id+""))
                .andExpect(status().isOk()).andExpect(content().string(read_file_json(path + "buildClustersAndCompute/output.json")));
        ++id;
        List<Dependency> dependencies = clustersModelDatabaseMaxGraph.getDependencies("UPC");
        assertEquals(read_file_array(path+"buildClustersAndCompute/output_dependencies.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"buildClustersAndCompute/output_model.json"), extractModel("UPC",true, false));
    }

    @Test
    public void simReqClusters() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", new FileInputStream(new File(path+"simReqClusters/input_model.json")));
        this.mockMvc.perform(multipart(url + "BuildClusters").file(multipartFile).param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("responseId", id+"").param("useComponent","false"))
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
    public void simReqClustersWithComponent() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", new FileInputStream(new File(path+"simReqClustersWithComponent/input_model.json")));
        this.mockMvc.perform(multipart(url + "BuildClusters").file(multipartFile).param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("responseId", id+"").param("useComponent","true"))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimReqClusters").param("organization", "UPC").param("maxValue", "-1")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqClustersWithComponent/input_operation.json")))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "simReqClustersWithComponent/output_all.json")));
    }

    @Test
    public void simReqClustersReqNotExist() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", new FileInputStream(new File(path+"simReqClusters/input_model.json")));
        this.mockMvc.perform(multipart(url + "BuildClusters").file(multipartFile).param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("responseId", id+"").param("useComponent","false"))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "SimReqClusters").param("organization", "UPC").param("maxValue", "-1")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"simReqClusters/input_operation_not_exist.json")))
                .andExpect(status().isNotFound());
    }

    @Test
    public void treatDependencies() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", new FileInputStream(new File(path+"treatDependencies/input_model.json")));
        this.mockMvc.perform(multipart(url + "BuildClusters").file(multipartFile).param("organization", "UPC").param("threshold", "1.1")
                .param("compare", "true").param("responseId", id+"").param("useComponent","false"))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "TreatAcceptedAndRejectedDependencies").param("organization", "UPC")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"treatDependencies/input_treat.json")))
                .andExpect(status().isOk());
        List<Dependency> dependencies = clustersModelDatabaseMaxGraph.getDependencies("UPC");
        assertEquals(read_file_array(path+"treatDependencies/output_dependencies.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"treatDependencies/output_model.json"), extractModel("UPC",false, false));
    }

    @Test
    public void treatDependenciesWithLoop() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", new FileInputStream(new File(path+"treatDependencies/input_model.json")));
        this.mockMvc.perform(multipart(url + "BuildClusters").file(multipartFile).param("organization", "UPC").param("threshold", "1.1")
                .param("compare", "true").param("responseId", id+"").param("useComponent","false"))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "TreatAcceptedAndRejectedDependencies").param("organization", "UPC")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"treatDependencies/input_treat_loop.json")))
                .andExpect(status().isOk());
        List<Dependency> dependencies = clustersModelDatabaseMaxGraph.getDependencies("UPC");
        assertEquals(read_file_array(path+"treatDependencies/output_dependencies_loop.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"treatDependencies/output_model_loop.json"), extractModel("UPC",false, false));
    }

    @Test
    public void treatDependenciesWithProposed() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", new FileInputStream(new File(path+"treatDependencies/input_model.json")));
        this.mockMvc.perform(multipart(url + "BuildClusters").file(multipartFile).param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("responseId", id+"").param("useComponent","false"))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "TreatAcceptedAndRejectedDependencies").param("organization", "UPC")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"treatDependencies/input_treat.json")))
                .andExpect(status().isOk());
        List<Dependency> dependencies = clustersModelDatabaseMaxGraph.getDependencies("UPC");
        assertEquals(read_file_array(path+"treatDependencies/output_dependencies_with_proposed.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"treatDependencies/output_model_with_proposed.json"), extractModel("UPC",false, false));
    }

    @Test
    public void batchProcess() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", new FileInputStream(new File(path+"batchProcess/input_model.json")));
        this.mockMvc.perform(multipart(url + "BuildClusters").file(multipartFile).param("organization", "UPC").param("threshold", "1.1")
                .param("compare", "true").param("responseId", id+"").param("useComponent","false"))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "BatchProcess").param("organization", "UPC").param("responseId", id+"")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"batchProcess/input_cron.json")))
                .andExpect(status().isOk());
        ++id;
        List<Dependency> dependencies = clustersModelDatabaseMaxGraph.getDependencies("UPC");
        assertEquals(read_file_array(path+"batchProcess/output_dependencies.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"batchProcess/output_model.json"), extractModel("UPC",true, false));
    }

    @Test
    public void batchProcessLoop() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", new FileInputStream(new File(path+"batchProcess/input_model.json")));
        this.mockMvc.perform(multipart(url + "BuildClusters").file(multipartFile).param("organization", "UPC").param("threshold", "1.1")
                .param("compare", "true").param("responseId", id+"").param("useComponent","false"))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "BatchProcess").param("organization", "UPC").param("responseId", id+"")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"batchProcess/input_cron_loop.json")))
                .andExpect(status().isOk());
        ++id;
        List<Dependency> dependencies = clustersModelDatabaseMaxGraph.getDependencies("UPC");
        assertEquals(read_file_array(path+"batchProcess/output_dependencies_loop.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"batchProcess/output_model_loop.json"), extractModel("UPC",true, false));
    }

    @Test
    public void batchProcessWithProposed() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", new FileInputStream(new File(path+"batchProcess/input_model.json")));
        this.mockMvc.perform(multipart(url + "BuildClusters").file(multipartFile).param("organization", "UPC").param("threshold", "0")
                .param("compare", "true").param("responseId", id+"").param("useComponent","false"))
                .andExpect(status().isOk());
        ++id;
        this.mockMvc.perform(post(url + "BatchProcess").param("organization", "UPC").param("responseId", id+"")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_json(path+"batchProcess/input_cron.json")))
                .andExpect(status().isOk());
        ++id;
        List<Dependency> dependencies = clustersModelDatabaseMaxGraph.getDependencies("UPC");
        assertEquals(read_file_array(path+"batchProcess/output_dependencies_proposed.json"),listDependenciesToJson(dependencies).toString());
        assertEquals(read_file_json(path+"batchProcess/output_model_proposed.json"), extractModel("UPC",true, false));
    }


    /*
    Auxiliary methods
     */

    @Test
    public void getOrganizationInfo() throws Exception {
        Time.getInstance().setClock(Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault()));
        MockMultipartFile multipartFile = new MockMultipartFile("file", new FileInputStream(new File(path+"getOrganizationInfo/input_model_with.json")));
        this.mockMvc.perform(multipart(url + "BuildClusters").file(multipartFile).param("organization", "UPCTest").param("threshold", "0.12")
                .param("compare", "true").param("responseId", "Test0").param("useComponent","false"))
                .andExpect(status().isOk());
        Constants.getInstance().getDatabaseModel().saveResponse("UPCTest","Test3","TestMethod");
        Time.getInstance().setClock(Clock.fixed(ofEpochMilli(40), ZoneId.systemDefault()));
        this.mockMvc.perform(get(url + "GetOrganizationInfo").param("organization", "UPCTest"))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "getOrganizationInfo/output_with_clusters.json")));
        this.mockMvc.perform(delete(url + "ClearOrganization").param("organization", "UPCTest"));
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPCTest").param("useComponent","false")
                .param("compare", "false").param("responseId", "Test1").contentType(MediaType.APPLICATION_JSON_VALUE).content(read_file_array(path+"getOrganizationInfo/input_model_without.json")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(url + "GetOrganizationInfo").param("organization", "UPCTest"))
                .andExpect(status().isOk()).andExpect(content().string(read_file_raw(path + "getOrganizationInfo/output_without_clusters.json")));
        Time.getInstance().setClock(Clock.systemUTC());
    }

    @Test
    public void deleteOrganizationResponses() throws Exception {
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("threshold", "0.0").param("useComponent","false")
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
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
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
        this.mockMvc.perform(post(url + "BuildModel").param("organization", "UPC").param("useComponent","false")
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
        OrganizationModels organizationModels = Constants.getInstance().getDatabaseModel().getOrganizationModels(organization,!withFrequency);
        SimilarityModelTfIdf similarityModelTfIdf = (SimilarityModelTfIdf) organizationModels.getSimilarityModel();
        ClustersModelMaxGraph clustersModelMaxGraph = (ClustersModelMaxGraph) organizationModels.getClustersModel();
        JSONObject auxSimilarity = similarityModelTfIdf.extractModel(withDocs,withFrequency);
        JSONObject auxClusters = null;
        if (organizationModels.hasClusters()) {
            auxClusters = clustersModelMaxGraph.extractModel();
        }
        JSONObject result = new JSONObject();
        result.put("corpus", auxSimilarity.get("corpus"));
        result.put("corpusFrequency", auxSimilarity.get("corpusFrequency"));
        if (organizationModels.hasClusters()) {
            result.put("clusters", auxClusters.get("clusters"));
        } else result.put("clusters", new JSONArray());
        result.put("threshold", organizationModels.getThreshold());
        result.put("compare", organizationModels.isCompare());
        result.put("isCluster", organizationModels.hasClusters());
        if (organizationModels.hasClusters()) {
            result.put("lastClusterId",auxClusters.get("lastClusterId"));
        } else result.put("lastClusterId",0);
        return result.toString();
    }
}
