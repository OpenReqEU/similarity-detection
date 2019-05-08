package upc.similarity.semilarapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import upc.similarity.semilarapi.entity.Requirement;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.InternalErrorException;
import upc.similarity.semilarapi.exception.NotFinishedException;
import upc.similarity.semilarapi.service.ComparerService;

import java.util.List;

@RestController
@RequestMapping(value = "/upc/Comparer")
public class RestApiController {

    @Autowired
    ComparerService comparerService;

    @RequestMapping(value = "/BuildModel", method = RequestMethod.POST)
    public ResponseEntity<?> buildModel(@RequestParam("organization") String organization,
                                        @RequestParam("compare") String compare,
                                        @RequestBody List<Requirement> input) {
        try {
            comparerService.buildModel(compare,organization,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/SimReqReq", method = RequestMethod.POST)
    public ResponseEntity<?> simReqReq(@RequestParam("organization") String organization,
                                       @RequestParam("req1") String req1,
                                       @RequestParam("req2") String req2) {
        try {
            return new ResponseEntity<>(comparerService.simReqReq(organization,req1,req2),HttpStatus.OK);
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/SimReqProject", method = RequestMethod.POST)
    public ResponseEntity<?> simReqProject(@RequestParam("organization") String organization,
                                           @RequestParam("req") String req,
                                           @RequestParam("filename") String responseId,
                                           @RequestParam("threshold") double threshold,
                                           @RequestBody List<String> project_reqs) {
        try {
            comparerService.simReqProject(responseId,organization,req,threshold,project_reqs);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/SimProject", method = RequestMethod.POST)
    public ResponseEntity<?> simProject(@RequestParam("organization") String organization,
                                        @RequestParam("filename") String responseId,
                                        @RequestParam("threshold") double threshold,
                                        @RequestBody List<String> project_reqs) {
        try {
            comparerService.simProject(responseId,organization,threshold,project_reqs);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/BuildModelAndCompute", method = RequestMethod.POST)
    public ResponseEntity<?> buildModelAndCompute(@RequestParam("organization") String organization,
                                                  @RequestParam("compare") String compare,
                                                  @RequestParam("filename") String responseId,
                                                  @RequestParam("threshold") double threshold,
                                                  @RequestBody List<Requirement> input) {
        try {
            comparerService.buildModelAndCompute(responseId,compare,organization,threshold,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/GetResponsePage", method = RequestMethod.POST)
    public ResponseEntity<?> getResponsePage(@RequestParam("organization") String organization,
                                        @RequestParam("responseId") String responseId) {
        try {
            return new ResponseEntity<>(comparerService.getResponsePage(organization,responseId),HttpStatus.OK);
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NotFinishedException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.LOCKED);
        }
    }

    @RequestMapping(value = "/Clear", method = RequestMethod.DELETE)
    public ResponseEntity<?> clearDB(@RequestParam("organization") String organization) {
        try {
            comparerService.clearDB(organization);
            System.out.println("DB cleared");
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}