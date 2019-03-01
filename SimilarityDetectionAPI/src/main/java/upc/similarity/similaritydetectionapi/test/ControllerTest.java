package upc.similarity.similaritydetectionapi.test;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ControllerTest {

    private static class First_Result {
        int httpStatus;
        String id;
        First_Result(int httpStatus, String id) {
            this.httpStatus = httpStatus;
            this.id = id;
        }
    }

    private static class Second_Result {
        String result_info;
        String result;
        Second_Result(String result_info, String result) {
            this.result_info = result_info;
            this.result = result;
        }
    }

    private String path = "../testing/integration/semilar/";
    private static boolean finished = false;
    private static Second_Result second_result = new Second_Result(null,null);
    @LocalServerPort
    private int port;

    /*
    Simple endpoints
     */

    @Test
    public void TestAddReqs() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/DB/AddReqs?url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"addreqs.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"output_addreqs.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"AddReqs","true"),second_result.result_info);
        finished = false;
    }

    @Test
    public void TestReqReq() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ReqReq?compare=true&req1=QM-1&req2=QM-2&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"input_reqreq_simple.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"output_reqreq_simple.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"ReqReq","true"),second_result.result_info);
        finished = false;
    }

    @Test
    public void TestProjReq() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ReqProject?compare=true&req=QM-1&project=QM&threshold=0&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"input_projreq_simple.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"output_projreq_simple.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"ReqProj","true"),second_result.result_info);
        finished = false;
    }

    @Test
    public void TestProject() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/Project?compare=true&project=QM&threshold=0&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"input_proj_simple.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"output_proj_simple.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"Proj","true"),second_result.result_info);
        finished = false;
    }

    @Test
    public void TestClusterSimple() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/Clusters?compare=true&project=QM&threshold=0.3&type=all&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"input_clusters_simple.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"output_clusters_simple.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"Clusters","true"),second_result.result_info);
        finished = false;
    }

    /*
    More complex endpoints
     */

    @Test
    public void TestProjReq_multiple() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ReqProject?compare=true&req=QM-1&req=QM-2&project=QM&threshold=0&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"input_projreq_multiple.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"output_projreq_multiple.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"ReqProj","true"),second_result.result_info);
        finished = false;
    }

    @Test
    public void TestProject_reqs_not_in_DB() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/Project?compare=true&project=QM&threshold=0&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"input_proj_notDB.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"output_proj_notDB.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"Proj","true"),second_result.result_info);
        finished = false;
    }

    @Test
    public void TestReqReq_not_otherEdep() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ReqReq?compare=true&req1=QM-1&req2=QM-2&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"input_reqreq_not_otherdepE.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"output_reqreq_not_otherdepE.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"ReqReq","true"),second_result.result_info);
        finished = false;
    }

    @Test
    public void TestCluster_not_duplicates() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/Clusters?compare=true&project=QM&threshold=0.6&type=all&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"input_clusters_notDup.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"output_clusters_notDup.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"Clusters","true"),second_result.result_info);
        finished = false;
    }

    @Test
    public void TestCluster_change_older() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/Clusters?compare=true&project=QM&threshold=0.3&type=all&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"input_clusters_change_older.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"output_clusters_change_older.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"Clusters","true"),second_result.result_info);
        finished = false;
    }

    @Test
    public void TestCluster_big_all() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/Clusters?compare=true&project=QM&threshold=0.4&type=all&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"input_clusters_big_all.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"output_clusters_big_all.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"Clusters","true"),second_result.result_info);
        finished = false;
    }

    @Test
    public void TestCluster_big_one() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/Clusters?compare=true&project=QM&threshold=0.3&type=one&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"input_clusters_big_one.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"output_clusters_big_one.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"Clusters","true"),second_result.result_info);
        finished = false;
    }

    /*
    Exceptions
     */

    @Test
    public void TestReqReq_otherEdep() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/ReqReq?compare=true&req1=QM-1&req2=QM-2&url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"input_reqreq_depE.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"output_reqreq_depE.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"ReqReq","false"),second_result.result_info);
        finished = false;
    }
    @Test
    public void AuxAddReqs() throws InterruptedException {
        First_Result first_result = connect_to_component("http://localhost:"+port+"/upc/similarity-detection/DB/AddReqs?url=http://localhost:"+port+"/upc/similarity-detection/Test",read_file(path+"addreqs.json"));
        assertEquals(200,first_result.httpStatus);
        while(!finished) {Thread.sleep(2000);}
        assertEquals(read_file(path+"output_addreqs.json"),second_result.result);
        assertEquals(create_json_info(first_result.id,"AddReqs","true"),second_result.result_info);
        finished = false;
    }




















    /*
    auxiliary methods
     */

    private String read_file(String path) {
        String result = "";
        String line = "";
        try {
            FileReader fileReader = new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null) {
                result = result.concat(line);
            }
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject aux = new JSONObject(result);
        return aux.toString();
    }

    public static void setResult(String result, String result_info) {
        second_result = new Second_Result(result_info,new JSONObject(result).toString());
        finished = true;
    }

    private First_Result connect_to_component(String url, String json) {

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(url);
        httppost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        int httpStatus = 500;
        String json_response = "";

        //Execute and get the response.
        try {
            HttpResponse response = httpclient.execute(httppost);
            httpStatus = response.getStatusLine().getStatusCode();
            json_response = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            System.out.println("Error conecting with server");
        }
        System.out.println(json_response);
        JSONObject aux = new JSONObject(json_response);
        String id = aux.getString("id");
        return new First_Result(httpStatus,id);
    }

    private String create_json_info(String id, String operation, String success) {
        JSONObject result = new JSONObject();
        result.put("id",id);
        result.put("success",success);
        result.put("operation",operation);
        return result.toString();
    }

}