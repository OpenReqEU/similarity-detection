package upc.similarity.similaritydetectionapi;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String path = "../testing/integration/semilar/";

    /*@Before
    public void ClearDB() {
        //auxiliary operation
        HttpClient httpclient = HttpClients.createDefault();
        HttpDelete httpdelete = new HttpDelete("http://localhost:9405/upc/Semilar/Clear?organization=Test");

        //Execute and get the response.
        try {
            httpclient.execute(httpdelete);
        } catch (IOException e) {
            System.out.println("Error conecting with server");
        }
    }

    /*
    Simple endpoints
     */

    /*@Test
    public void aBuildModel() throws Exception {
        this.mockMvc.perform(post("/upc/similarity-detection/BuildModel").param("organization","Test").param("compare","true").contentType(MediaType.APPLICATION_JSON).content(read_file(path+"input_buildmodel.json")))
l                .andDo(print()).andExpect(status().isOk());
    }

    @Test
    public void asimReqReq() throws Exception {
        this.mockMvc.perform(post("/upc/similarity-detection/BuildModel").param("organization","Test").param("compare","true").contentType(MediaType.APPLICATION_JSON).content(read_file(path+"input_buildmodel.json")))
                .andDo(print()).andExpect(status().isOk());
        this.mockMvc.perform(post("/upc/similarity-detection/SimReqReq").param("organization","Test").param("req1","true").param("req2","true").contentType(MediaType.APPLICATION_JSON).content(read_file(path+"input_buildmodel.json")))
                .andDo(print()).andExpect(status().isOk()).andExpect(content().string(read_file(path+"output_simReqReq.json")));
    }*/






















    /*
    auxiliary methods
     */

    private String read_file(String path) throws Exception {
        String result = "";
        String line = "";
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(path);
            bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null) {
                result = result.concat(line);
            }
            bufferedReader.close();
            JSONObject aux = new JSONObject(result);
            return aux.toString();
        } finally {
            if (fileReader != null) fileReader.close();
            if (bufferedReader != null) bufferedReader.close();
        }
    }
}
