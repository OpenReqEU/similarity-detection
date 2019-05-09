package upc.similarity.semilarapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import upc.similarity.semilarapi.entity.Requirement;
import upc.similarity.semilarapi.entity.input.ReqProject;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.InternalErrorException;
import upc.similarity.semilarapi.exception.NotFinishedException;
import upc.similarity.semilarapi.exception.NotFoundException;
import upc.similarity.semilarapi.service.ComparerService;

import java.util.List;

@RestController
@RequestMapping(value = "/upc/Comparer")
public class RestApiController {

    @Autowired
    ComparerService comparerService;

    @PostMapping(value = "/BuildModel")
    public ResponseEntity<?> buildModel(@RequestParam("organization") String organization,
                                        @RequestParam("compare") String compare,
                                        @RequestParam("filename") String responseId,
                                        @RequestBody List<Requirement> input) {
        try {
            comparerService.buildModel(responseId,compare,organization,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (BadRequestException e) {
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/SimReqReq")
    public ResponseEntity<?> simReqReq(@RequestParam("organization") String organization,
                                       @RequestParam("req1") String req1,
                                       @RequestParam("req2") String req2) {
        try {
            return new ResponseEntity<>(comparerService.simReqReq(organization,req1,req2),HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e,HttpStatus.NOT_FOUND);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/SimReqProject")
    public ResponseEntity<?> simReqProject(@RequestParam("organization") String organization,
                                           @RequestParam("filename") String responseId,
                                           @RequestParam("threshold") double threshold,
                                           @RequestBody ReqProject project_reqs) {
        try {
            comparerService.simReqProject(responseId,organization,threshold,project_reqs);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e,HttpStatus.NOT_FOUND);
        } catch (BadRequestException e) {
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/SimProject")
    public ResponseEntity<?> simProject(@RequestParam("organization") String organization,
                                        @RequestParam("filename") String responseId,
                                        @RequestParam("threshold") double threshold,
                                        @RequestBody List<String> project_reqs) {
        try {
            comparerService.simProject(responseId,organization,threshold,project_reqs,false);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e,HttpStatus.NOT_FOUND);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/BuildModelAndCompute")
    public ResponseEntity<?> buildModelAndCompute(@RequestParam("organization") String organization,
                                                  @RequestParam("compare") String compare,
                                                  @RequestParam("filename") String responseId,
                                                  @RequestParam("threshold") double threshold,
                                                  @RequestBody List<Requirement> input) {
        try {
            comparerService.buildModelAndCompute(responseId,compare,organization,threshold,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (BadRequestException e) {
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        } catch (InternalErrorException e) {
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/GetResponsePage")
    public ResponseEntity<?> getResponsePage(@RequestParam("organization") String organization,
                                        @RequestParam("responseId") String responseId) {
        try {
            return new ResponseEntity<>(comparerService.getResponsePage(organization,responseId),HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e,HttpStatus.NOT_FOUND);
        } catch (NotFinishedException e) {
            return new ResponseEntity<>(e,HttpStatus.LOCKED);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(value = "/ClearOrganizationResponses")
    public ResponseEntity<?> clearOrganizationResponses(@RequestParam("organization") String organization) {
        try {
            comparerService.clearOrganizationResponses(organization);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e,HttpStatus.NOT_FOUND);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}