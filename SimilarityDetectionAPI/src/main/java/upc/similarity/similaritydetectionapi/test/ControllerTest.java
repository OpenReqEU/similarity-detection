package upc.similarity.similaritydetectionapi.test;


import org.json.JSONObject;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.FileReader;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class ControllerTest {

    private String path = "../testing/integration/semilar/";

    /*
    Simple endpoints
     */

    /*@Test
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
    }*/

    /*
    More complex endpoints
     */
/*
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
    }*/

    /*
    Exceptions
     */

    /*@Test
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
*/



















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

    private String create_json_info(String id, String operation, String success) {
        JSONObject result = new JSONObject();
        result.put("id",id);
        result.put("success",success);
        result.put("operation",operation);
        return result.toString();
    }

}