package upc.similarity.similaritydetectionapi;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import upc.similarity.similaritydetectionapi.entity.input_output.JsonProject;
import upc.similarity.similaritydetectionapi.entity.input_output.Requirements;
import upc.similarity.similaritydetectionapi.exception.*;
import upc.similarity.similaritydetectionapi.service.SimilarityService;

import java.net.URL;
import java.util.*;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping(value = "upc/similarity-detection")
@Api(value = "SimilarityDetectionAPI", produces = MediaType.APPLICATION_JSON_VALUE)
public class RestApiController {

    @Autowired
    SimilarityService similarityService;

    //Model generator

    @CrossOrigin
    @PostMapping(value = "/AddReqs", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Builds a model with the input requirements", notes = "Builds a model with the entry requirements. " +
            "The generated model is assigned to the specified organization and stored in an internal database. Each organization" +
            " only can have one model at a time. If at the time of generating a new model the corresponding organization already has" +
            " an existing model, it is replaced by the new one.")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=404, message = "Not Found"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Component Error")})
    public ResponseEntity<?> buildModel(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                        @ApiParam(value="Use text attribute?", required = false, example = "true") @RequestParam(value = "compare",required = false) boolean compare,
                                        @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                        @ApiParam(value="OpenReqJson with requirements", required = true) @RequestBody Requirements input) {
        try {
            url_ok(url);
            return new ResponseEntity<>(similarityService.buildModel(url,organization,compare,input),HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/AddReqsAndCompute", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Builds a model with the input requirements and computes them", notes = "<p>Builds a model with the entry requirements. " +
            "The generated model is assigned to the specified organization and stored in an internal database. Each organization" +
            " only can have one model at a time. If at the time of generating a new model the corresponding organization already has" +
            " an existing model, it is replaced by the new one.</p><br><p>Also, it returns an array of dependencies between all possible pairs of " +
            " requirements from the requirements received as input.</p>")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Component Error")})
    public ResponseEntity<?> buildModelAndCompute(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                                  @ApiParam(value="Use text attribute?", required = false, example = "true") @RequestParam(value = "compare",required = false) boolean compare,
                                                  @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                                  @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                                  @ApiParam(value="OpenReqJson with requirements", required = true) @RequestBody Requirements input) {
        try {
            url_ok(url);
            return new ResponseEntity<>(similarityService.buildModelAndCompute(url,organization,compare,threshold,input),HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        }
    }

    // Req - Req

    @CrossOrigin
    @PostMapping(value = "/ReqReq", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between two requirements", notes = "Returns a dependency between the two input requirements. The similarity score is computed with the" +
            " model assigned to the specified organization. The two requirements must be in this model.")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=404, message = "Not Found"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Component Error")})
    public ResponseEntity<?> simReqReq(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                       @ApiParam(value="Id of the first requirement to compare", required = true, example = "SQ-132") @RequestParam("req1") String req1,
                                       @ApiParam(value="Id of the second requirement to compare", required = true, example = "SQ-98") @RequestParam("req2") String req2,
                                       @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url) {
        try {
            url_ok(url);
            return new ResponseEntity<>(similarityService.simReqReq(url,organization,req1,req2), HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        }
    }

    // Req - Project

    @CrossOrigin
    @PostMapping(value = "/ReqProject", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between a requirement and all the requirements of a specific project", notes = "<p>Returns an array of dependencies " +
            "between the requirement and the project's requirements received as input. The similarity score is computed with the model assigned to the specified organization. " +
            "All the requirements must be inside this model.</p>")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=404, message = "Not Found"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal Error")})
    public ResponseEntity<?> simReqProject(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                           @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                           /*@ApiParam(value="Maximum number of dependencies in the output", required = true, example = "5") @RequestParam("max_number") int max_number,*/
                                           @ApiParam(value="Id of the requirement to compare", required = true, example = "SQ-132") @RequestParam("req") String req,
                                           @ApiParam(value="Id of the project to compare", required = true, example = "SM") @RequestParam("project") String project,
                                           @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                           @ApiParam(value="OpenReqJson with the project", required = true) @RequestBody JsonProject input) {
        try {
            url_ok(url);
            return new ResponseEntity<>(similarityService.simReqProject(url,organization,threshold,0,req,project,input), HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        }
    }

    // Project

    @CrossOrigin
    @PostMapping(value = "/Project", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between the requirements of one project", notes = "<p>Returns an array of dependencies between all possible pairs of " +
            "requirements from the project received as input. The similarity score is computed with the model assigned to the specified organization. All the requirements" +
            " must be inside this model.</p>")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=404, message = "Not Found"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal Error")})
    public ResponseEntity<?> simProject(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                        @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                        @ApiParam(value="Id of the project to compare", required = true, example = "SQ") @RequestParam("project") String project,
                                        @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                        @ApiParam(value="OpenReqJson with the project", required = true) @RequestBody JsonProject input) {
        try {
            url_ok(url);
            return new ResponseEntity<>(similarityService.simProject(url,organization,threshold,0,project,input), HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        }
    }

    //Responses

    @CrossOrigin
    @GetMapping(value = "/GetResponse", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Returns result dependencies of the other operations", notes = "<p>Returns the result dependencies of the Project, ReqProject and AddReqsAndCompute methods. The result is a " +
            "json object formed by an array of dependencies which are returned in patches of 20,000. Each time this operation is called, the following 20,000 dependencies of the indicated response will be" +
            " returned. An empty json will be returned when no more dependencies are left. </p>")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=423, message = "The computation is not finished yet"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal Error")})
    public ResponseEntity<?> getResponsePage(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                             @ApiParam(value="Response identifier", required = true, example = "UPC") @RequestParam("response") String responseId) {
        try {
            return new ResponseEntity<>(similarityService.getResponsePage(organization,responseId), HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        } catch (NotFinishedException e) {
            return getNotFinishedError(e);
        }
    }



    /*@CrossOrigin
    @RequestMapping(value = "/DB/Clear", method = RequestMethod.DELETE)
    @ApiOperation(value = "Clear the Semilar library database", notes = "It's useful to clear the database of old requirements.")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=410, message = "Not Found"),
            @ApiResponse(code=411, message = "Bad request"),
            @ApiResponse(code=511, message = "Component Error")})
    public ResponseEntity<?> clearDB() {
        try {
            similarityService.clearDB();
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        }
    }*/


    /*
    auxiliary operations
     */

    private void url_ok(String url) throws BadRequestException {
        try {
            new URL(url).toURI();
            /*HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.connect();
            int code = connection.getResponseCode();
            if (code != 200) throw new BadRequestException("Output server doesn't return status code 200");*/
        } catch (Exception e) {
            throw new BadRequestException("Output server doesn't exist");
        }
    }


    private ResponseEntity<?> getResponseNotFound(NotFoundException e) {
        e.printStackTrace();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("status", "404");
        result.put("error", "Not Found");
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<?> getResponseBadRequest(BadRequestException e) {
        e.printStackTrace();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("status", "400");
        result.put("error", "Bad request");
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<?> getInternalError(InternalErrorException e) {
        e.printStackTrace();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("status", "500");
        result.put("error", "Server Error");
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<?> getNotFinishedError(NotFinishedException e) {
        e.printStackTrace();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("status", "423");
        result.put("error", "Locked");
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}