package upc.similarity.semilarapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import upc.similarity.semilarapi.entity.Requirement;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.InternalErrorException;
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
        try {
            semilarService.buildModel(compare,organization,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/upc/Semilar/SimReqReq", method = RequestMethod.POST)
    public ResponseEntity<?> simReqReq(@RequestParam("organization") String organization,
                                       @RequestParam("req1") String req1,
                                       @RequestParam("req2") String req2,
                                       @RequestParam("filename") String filename) {
        try {
            semilarService.simReqReq(filename,organization,req1,req2);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/upc/Semilar/SimReqProject", method = RequestMethod.POST)
    public ResponseEntity<?> simReqProject(@RequestParam("organization") String organization,
                                           @RequestParam("req") String req,
                                           @RequestParam("filename") String filename,
                                           @RequestParam("threshold") double threshold,
                                           @RequestBody List<String> project_reqs) {
        try {
            semilarService.simReqProject(filename,organization,req,threshold,project_reqs);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/upc/Semilar/SimProject", method = RequestMethod.POST)
    public ResponseEntity<?> simProject(@RequestParam("organization") String organization,
                                        @RequestParam("filename") String filename,
                                        @RequestParam("threshold") double threshold,
                                        @RequestBody List<String> project_reqs) {
        try {
            semilarService.simProject(filename,organization,threshold,project_reqs);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/upc/Semilar/BuildModelAndCompute", method = RequestMethod.POST)
    public ResponseEntity<?> buildModelAndCompute(@RequestParam("organization") String organization,
                                                  @RequestParam("compare") String compare,
                                                  @RequestParam("filename") String filename,
                                                  @RequestParam("threshold") double threshold,
                                                  @RequestBody List<Requirement> input) {
        try {
            semilarService.buildModelAndCompute(filename,compare,organization,threshold,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/upc/Semilar/Clear", method = RequestMethod.DELETE)
    public ResponseEntity<?> clearDB(@RequestParam("organization") String organization) {
        try {
            semilarService.clearDB(organization);
            System.out.println("DB cleared");
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}