package upc.similarity.semilarapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import upc.similarity.semilarapi.entity.Requirement;
import upc.similarity.semilarapi.service.SemilarService;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping(value = "/")
public class RestApiController {

    @Autowired
    SemilarService semilarService;

    @RequestMapping(value = "/upc/Semilar/BuildModel", method = RequestMethod.POST)
    public ResponseEntity<?> buildModel(@RequestParam("organization") String organization,
                                        @RequestParam("compare") String compare,
                                        @RequestBody List<Requirement> input) {
        System.out.println("Enter");
        try {
            semilarService.buildModel(compare,organization,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(411));
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(500));
        }
    }

    @RequestMapping(value = "/upc/Semilar/SimReqReq", method = RequestMethod.POST)
    public ResponseEntity<?> simReqReq(@RequestParam("organization") String organization,
                                       @RequestParam("req1") String req1,
                                       @RequestParam("req2") String req2) {
        try {
            return new ResponseEntity<>(semilarService.simReqReq(organization,req1,req2),HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(500));
        }
    }

    @RequestMapping(value = "/upc/Semilar/SimReqProject", method = RequestMethod.POST)
    public ResponseEntity<?> simReqProject(@RequestParam("organization") String organization,
                                           @RequestParam("req") String req,
                                           @RequestBody List<String> project_reqs) {
        try {
            return new ResponseEntity<>(semilarService.simReqProject(organization,req,project_reqs),HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(500));
        }
    }

    @RequestMapping(value = "/upc/Semilar/Clear", method = RequestMethod.DELETE)
    public ResponseEntity<?> clearDB() {
        try {
            semilarService.clearDB();
            System.out.println("DB cleared");
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(411));
        }
    }

}