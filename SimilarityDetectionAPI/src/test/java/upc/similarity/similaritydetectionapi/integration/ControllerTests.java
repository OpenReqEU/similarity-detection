package upc.similarity.similaritydetectionapi.integration;


import org.json.JSONObject;
import org.junit.Rule;
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
import org.springframework.test.web.servlet.MvcResult;

import java.io.BufferedReader;
import java.io.FileReader;

import static org.junit.Assert.assertEquals;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import upc.similarity.similaritydetectionapi.config.TestConfig;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
public class ControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @LocalServerPort
    int port = 9404;

    private String path = "../testing/integration/main_component/";
    private String callback = "http://localhost:9404/upc/similarity-detection/Test";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9405);


    /*
    Similarity without clusters
     */

    @Test
    public void buildModel() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        MvcResult result = this.mockMvc.perform(post("/upc/similarity-detection/BuildModel").param("organization", "UPC").param("url", callback)
                .param("compare", "true")
                .contentType(MediaType.APPLICATION_JSON).content(read_file(path+"addReqs/input_addReqs.json")))
                .andExpect(status().isOk()).andReturn();
        TestConfig testConfig = TestConfig.getInstance();
        while(!testConfig.isComputationFinished()) {Thread.sleep(1000);}
        testConfig.setComputationFinished(false);
        assertEquals(createJsonResult(200, aux_getResponseId(result), "BuildModel"), testConfig.getResult().toString());

    }

    @Test
    public void buildModelNotRequirements() throws Exception {
        this.mockMvc.perform(post("/upc/similarity-detection/BuildModel").param("organization", "UPC").param("url", callback)
                .param("compare", "true")
                .contentType(MediaType.APPLICATION_JSON).content(read_file(path+"addReqs/input_requirements_empty.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void buildModelAndCompute() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        MvcResult result = this.mockMvc.perform(post("/upc/similarity-detection/BuildModelAndCompute").param("organization", "UPC").param("url", callback)
                .param("compare", "true").param("threshold", "0.12").contentType(MediaType.APPLICATION_JSON).content(read_file(path+"addReqs/input_addReqsAndCompute.json")))
                .andExpect(status().isOk()).andReturn();
        TestConfig testConfig = TestConfig.getInstance();
        while(!testConfig.isComputationFinished()) {Thread.sleep(1000);}
        testConfig.setComputationFinished(false);
        assertEquals(createJsonResult(200, aux_getResponseId(result), "AddReqsAndCompute"), testConfig.getResult().toString());
    }

    @Test
    public void buildModelAndComputeNotRequirements() throws Exception {
        this.mockMvc.perform(post("/upc/similarity-detection/BuildModelAndCompute").param("organization", "UPC").param("url", callback)
                .param("compare", "true").param("threshold", "0.12").contentType(MediaType.APPLICATION_JSON).content(read_file(path+"addReqs/input_requirements_empty.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void addRequirements() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        MvcResult result = this.mockMvc.perform(post("/upc/similarity-detection/AddRequirements").param("organization", "UPC").param("url", callback)
                .contentType(MediaType.APPLICATION_JSON).content(read_file(path+"addReqs/input_addReqs.json")))
                .andExpect(status().isOk()).andReturn();
        TestConfig testConfig = TestConfig.getInstance();
        while(!testConfig.isComputationFinished()) {Thread.sleep(1000);}
        testConfig.setComputationFinished(false);
        assertEquals(createJsonResult(200, aux_getResponseId(result), "AddRequirements"), testConfig.getResult().toString());

    }

    @Test
    public void deleteRequirements() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        MvcResult result = this.mockMvc.perform(post("/upc/similarity-detection/DeleteRequirements").param("organization", "UPC").param("url", callback)
                .contentType(MediaType.APPLICATION_JSON).content(read_file(path+"addReqs/input_addReqs.json")))
                .andExpect(status().isOk()).andReturn();
        TestConfig testConfig = TestConfig.getInstance();
        while(!testConfig.isComputationFinished()) {Thread.sleep(1000);}
        testConfig.setComputationFinished(false);
        assertEquals(createJsonResult(200, aux_getResponseId(result), "DeleteRequirements"), testConfig.getResult().toString());

    }

    @Test
    public void reqReq() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(read_file(path + "reqReq/output.json"))));
        this.mockMvc.perform(post("/upc/similarity-detection/ReqReq").param("organization", "UPC")
                .param("req1", "UPC-1").param("req2", "UPC-2")).andDo(print())
                .andExpect(status().isOk()).andExpect(status().isOk()).andExpect(content().string(read_file(path + "reqReq/output.json")));
    }

    @Test
    public void simReqOrganization() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/SimReqOrganization"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        MvcResult result = this.mockMvc.perform(post("/upc/similarity-detection/ReqOrganization").param("organization", "UPC")
                .param("threshold", "0").param("url", callback).param("req", "UPC-1").param("req", "UPC-2"))
                .andExpect(status().isOk()).andReturn();
        TestConfig testConfig = TestConfig.getInstance();
        while(!testConfig.isComputationFinished()) {Thread.sleep(1000);}
        testConfig.setComputationFinished(false);
        assertEquals(createJsonResult(200, aux_getResponseId(result), "ReqOrganization"), testConfig.getResult().toString());
    }

    @Test
    public void simNewReqOrganization() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/SimNewReqOrganization"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        MvcResult result = this.mockMvc.perform(post("/upc/similarity-detection/NewReqOrganization").param("organization", "UPC").param("threshold", "0").param("url", callback)
                .contentType(MediaType.APPLICATION_JSON).content(read_file(path+"simNewReqOrganization/input_reqs.json")))
                .andExpect(status().isOk()).andReturn();
        TestConfig testConfig = TestConfig.getInstance();
        while(!testConfig.isComputationFinished()) {Thread.sleep(1000);}
        testConfig.setComputationFinished(false);
        assertEquals(createJsonResult(200, aux_getResponseId(result), "NewReqOrganization"), testConfig.getResult().toString());
    }

    @Test
    public void reqProject() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/SimReqProject*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        MvcResult result = this.mockMvc.perform(post("/upc/similarity-detection/ReqProject").param("organization", "UPC").param("url", callback)
                .param("project", "UPC-P1").param("req", "UPC-1").param("threshold", "0")
                .contentType(MediaType.APPLICATION_JSON).content(read_file(path+"reqProject/input_ReqProject.json")))
                .andExpect(status().isOk()).andReturn();
        TestConfig testConfig = TestConfig.getInstance();
        while(!testConfig.isComputationFinished()) {Thread.sleep(1000);}
        testConfig.setComputationFinished(false);
        assertEquals(createJsonResult(200, aux_getResponseId(result), "ReqProject"), testConfig.getResult().toString());
    }

    @Test
    public void reqProjectNotExist() throws Exception {
        this.mockMvc.perform(post("/upc/similarity-detection/ReqProject").param("organization", "UPC").param("url", callback)
                .param("project", "UPC-P2").param("req", "UPC-1").param("threshold", "0")
                .contentType(MediaType.APPLICATION_JSON).content(read_file(path+"reqProject/input_ReqProject.json")))
                .andExpect(status().isNotFound());
    }

    @Test
    public void project() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/SimProject"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        MvcResult result = this.mockMvc.perform(post("/upc/similarity-detection/Project").param("organization", "UPC").param("url", callback).param("threshold", "0")
                .param("project", "UPC-P1").contentType(MediaType.APPLICATION_JSON).content(read_file(path+"project/input_Project.json")))
                .andExpect(status().isOk()).andReturn();
        TestConfig testConfig = TestConfig.getInstance();
        while(!testConfig.isComputationFinished()) {Thread.sleep(1000);}
        testConfig.setComputationFinished(false);
        assertEquals(createJsonResult(200, aux_getResponseId(result), "Project"), testConfig.getResult().toString());
    }

    @Test
    public void projectNotExist() throws Exception {
        this.mockMvc.perform(post("/upc/similarity-detection/Project").param("organization", "UPC").param("url", callback).param("threshold", "0")
                .param("project", "UPC-P2").contentType(MediaType.APPLICATION_JSON).content(read_file(path+"project/input_Project.json")))
                .andExpect(status().isNotFound());
    }

    @Test
    public void projectProject() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/SimProjectProject"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        MvcResult result = this.mockMvc.perform(post("/upc/similarity-detection/ProjectProject").param("organization", "UPC").param("url", callback).param("threshold", "0")
                .param("firstProject", "UPC-P1").param("secondProject", "UPC-P2").contentType(MediaType.APPLICATION_JSON).content(read_file(path+"projectProject/input_project.json")))
                .andExpect(status().isOk()).andReturn();
        TestConfig testConfig = TestConfig.getInstance();
        while(!testConfig.isComputationFinished()) {Thread.sleep(1000);}
        testConfig.setComputationFinished(false);
        assertEquals(createJsonResult(200, aux_getResponseId(result), "ProjectProject"), testConfig.getResult().toString());
    }

    @Test
    public void projectProjectSame() throws Exception {
        this.mockMvc.perform(post("/upc/similarity-detection/ProjectProject").param("organization", "UPC").param("url", callback).param("threshold", "0")
                .param("firstProject", "UPC-P1").param("secondProject", "UPC-P1").contentType(MediaType.APPLICATION_JSON).content(read_file(path+"projectProject/input_project.json")))
                .andExpect(status().isBadRequest());
    }


    /*
    Similarity with clusters
     */

    @Test
    public void buildClusters() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "test.txt",
                "text/plain", "Spring Framework".getBytes());
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        MvcResult result = this.mockMvc.perform(multipart("/upc/similarity-detection/BuildClusters").file(multipartFile).param("organization", "UPC").param("url", callback)
                .param("compare", "true").param("threshold", "0.12")
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk()).andReturn();
        TestConfig testConfig = TestConfig.getInstance();
        while(!testConfig.isComputationFinished()) {Thread.sleep(1000);}
        testConfig.setComputationFinished(false);
        assertEquals(createJsonResult(200, aux_getResponseId(result), "BuildClusters"), testConfig.getResult().toString());
    }

    @Test
    public void buildClustersAndCompute() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "test.txt",
                "text/plain", "Spring Framework".getBytes());
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        MvcResult result = this.mockMvc.perform(multipart("/upc/similarity-detection/BuildClustersAndCompute").file(multipartFile).param("organization", "TEEEEST").param("url", callback)
                .param("compare", "true").param("threshold", "0.12")
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk()).andReturn();
        TestConfig testConfig = TestConfig.getInstance();
        while(!testConfig.isComputationFinished()) {Thread.sleep(1000);}
        testConfig.setComputationFinished(false);
        assertEquals(createJsonResult(200, aux_getResponseId(result), "BuildClustersAndCompute"), testConfig.getResult().toString());
    }

    @Test
    public void simReqClusters() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(read_file(path + "simReqClusters/output.json"))));
        this.mockMvc.perform(post("/upc/similarity-detection/ReqClusters").param("organization", "UPC")
                .param("requirementId", "UPC-1").param("maxNumber", "10"))
                .andExpect(status().isOk()).andExpect(status().isOk()).andExpect(content().string(read_file(path + "simReqClusters/output.json")));
    }

    @Test
    public void treatDependencies() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        this.mockMvc.perform(post("/upc/similarity-detection/TreatAcceptedAndRejectedDependencies").param("organization", "UPC")
                .contentType(MediaType.APPLICATION_JSON).content(read_file(path+"treatDependencies/input.json")))
                .andExpect(status().isOk()).andExpect(status().isOk());
    }

    @Test
    public void batchProcess() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        MvcResult result = this.mockMvc.perform(post("/upc/similarity-detection/BatchProcess").param("organization", "UPC").param("url", callback)
                .contentType(MediaType.APPLICATION_JSON).content(read_file(path+"cronMethod/input.json")))
                .andExpect(status().isOk()).andReturn();
        TestConfig testConfig = TestConfig.getInstance();
        while(!testConfig.isComputationFinished()) {Thread.sleep(1000);}
        testConfig.setComputationFinished(false);
        assertEquals(createJsonResult(200, aux_getResponseId(result), "BatchProcess"), testConfig.getResult().toString());
    }


    /*
    Auxiliary methods
     */

    @Test
    public void getResponse() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(read_file(path + "getResponse/output.json"))));
        this.mockMvc.perform(get("/upc/similarity-detection/GetResponse").param("organization", "UPC")
                .param("response", "11323324_566")).andDo(print())
                .andExpect(status().isOk()).andExpect(status().isOk()).andExpect(content().string(read_file(path + "getResponse/output.json")));
    }

    @Test
    public void getOrganizationInfo() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(read_file(path + "getOrganizationInfo/output.json"))));
        this.mockMvc.perform(get("/upc/similarity-detection/GetOrganizationInfo").param("organization", "UPC")).andDo(print())
                .andExpect(status().isOk()).andExpect(status().isOk()).andExpect(content().string(read_file(path + "getOrganizationInfo/output.json")));
    }

    @Test
    public void deleteResponses() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.delete(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        this.mockMvc.perform(delete("/upc/similarity-detection/DeleteOrganizationResponses").param("organization", "UPC")).andDo(print())
                .andExpect(status().isOk()).andExpect(status().isOk());
    }

    @Test
    public void clearOrganization() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.delete(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        this.mockMvc.perform(delete("/upc/similarity-detection/DeleteOrganizationData").param("organization", "UPC")).andDo(print())
                .andExpect(status().isOk()).andExpect(status().isOk());
    }

    @Test
    public void clearDatabase() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.delete(urlPathMatching("/upc/Compare/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));
        this.mockMvc.perform(delete("/upc/similarity-detection/ClearDatabase")).andDo(print())
                .andExpect(status().isOk()).andExpect(status().isOk());
    }


    /*
    Private methods
     */

    private String createJsonResult(int code, String id, String operation) {
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("id",id);
        json.put("operation", operation);
        return json.toString();
    }

    private String aux_getResponseId(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        JSONObject json = new JSONObject(content);
        return json.getString("id");
    }

    private String read_file(String path) throws Exception {
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
}
