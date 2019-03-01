package upc.similarity.similaritydetectionapi;

import io.swagger.annotations.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
//import upc.similarity.similaritydetectionapi.test.ControllerTest;
import upc.similarity.similaritydetectionapi.entity.input_output.JsonProject;
import upc.similarity.similaritydetectionapi.entity.input_output.JsonReqReq;
import upc.similarity.similaritydetectionapi.entity.input_output.Requirements;
import upc.similarity.similaritydetectionapi.exception.*;
import upc.similarity.similaritydetectionapi.service.SimilarityService;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import upc.similarity.similaritydetectionapi.test.ControllerTest;

@RestController
@RequestMapping(value = "upc/similarity-detection")
@Api(value = "SimilarityDetectionAPI", produces = MediaType.APPLICATION_JSON_VALUE)
public class RestApiController {

    @Autowired
    SimilarityService similarityService;

    // Req - Req

    @CrossOrigin
    @RequestMapping(value = "/ReqReq", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between two requirements", notes = "<p>The resulting input stream contains an array of dependencies with the similarity dependency between the two selected requirements." +
            " The dependency is only returned if doesn't exist another similar or duplicate dependency between the two requirements.</p>" +
            "<p> <br> Example:<em> {\"dependencies\":[{\"toid\":\"QM-2\",\"dependency_type\":\"similar\",\"dependency_score\":0.6666667,\"description\":[\"Similarity-Semilar\"],\"fromid\":\"QM-1\",\"status\":\"proposed\"}]}</em> </p>")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=410, message = "Not Found"),
            @ApiResponse(code=411, message = "Bad request"),
            @ApiResponse(code=511, message = "Component Error")})
    public ResponseEntity<?> simReqReq(@ApiParam(value="Id of the first requirement to compare", required = true, example = "SQ-132") @RequestParam("req1") String req1,
                                       @ApiParam(value="Id of the second requirement to compare", required = true, example = "SQ-98") @RequestParam("req2") String req2,
                                       @ApiParam(value="Use text attribute in comparison?", required = false, example = "false") @RequestParam("compare") String compare,
                                       @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                       @ApiParam(value="OpenreqJson with the two requirements", required = true) @RequestBody JsonReqReq json) {
        try {
            url_ok(url);
            if (compare == null) compare = "false";
            return new ResponseEntity<>(similarityService.simReqReq(req1, req2, compare, url, json), HttpStatus.OK);
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
    @RequestMapping(value = "/ReqProject", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between a set of requirements and all the requirements of a specific project", notes = "The resulting input stream contains an array of dependencies with the similarity dependencies" +
            " between the selected requirements and all the requirements of the project specified. Every dependency will only be returned if doesn't exist another similar or duplicate dependency between the two requirements.")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=410, message = "Not Found"),
            @ApiResponse(code=411, message = "Bad request"),
            @ApiResponse(code=510, message = "Internal Error"),
            @ApiResponse(code=511, message = "Component Error")})
    public ResponseEntity<?> simReqProject(@ApiParam(value="Ids of the requirements to compare", required = true, example = "SQ-132") @RequestParam("req") List<String> req,
                                           @ApiParam(value="Id of the project to compare", required = true, example = "SM") @RequestParam("project") String project,
                                           @ApiParam(value="Use text attribute in comparison?", required = false, example = "false") @RequestParam("compare") String compare,
                                           @ApiParam(value="Float between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.3") @RequestParam("threshold") Float threshold,
                                           @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                           @ApiParam(value="Json with requirements", required = true) @RequestBody JsonProject json) {
        try {
            url_ok(url);
            if (compare == null) compare = "false";
            return new ResponseEntity<>(similarityService.simReqProj(req,project,compare,threshold,url,json), HttpStatus.OK);
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
    @RequestMapping(value = "/Project", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between the requirements of one project", notes = "The resulting input stream contains an array of dependencies with the similarity dependencies among all the pairs of the " +
            "requirements of the selected project. Every dependency will only be returned if doesn't exist another similar or duplicate dependency between the two requirements." +
            "<br> <br> Example: <em> {\"dependencies\":[{\"toid\":\"QM-2\",\"dependency_type\":\"similar\",\"dependency_score\":0.6666667,\"description\":[\"Similarity-Semilar\"],\"fromid\":\"QM-1\",\"status\":\"proposed\"},{\"toid\":\"QM-3\",\"dependency_type\":\"similar\",\"dependency_score\":0.4,\"description\":[\"Similarity-Semilar\"],\"fromid\":\"QM-1\",\"status\":\"proposed\"},{\"toid\":\"QM-3\",\"dependency_type\":\"similar\",\"dependency_score\":0.4,\"description\":[\"Similarity-Semilar\"],\"fromid\":\"QM-2\",\"status\":\"proposed\"}]} </em>")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=410, message = "Not Found"),
            @ApiResponse(code=411, message = "Bad request"),
            @ApiResponse(code=510, message = "Internal Error"),
            @ApiResponse(code=511, message = "Component Error")})
    public ResponseEntity<?> simProject(@ApiParam(value="Id of the project to compare", required = true, example = "SQ") @RequestParam("project") String project,
                                        @ApiParam(value="Use text attribute in comparison?", required = false, example = "false") @RequestParam("compare") String compare,
                                        @ApiParam(value="Float between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.3") @RequestParam("threshold") float threshold,
                                        @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                        @ApiParam(value="OpenreqJson with the project and the project's requirements", required = true, example = "SQ-132") @RequestBody JsonProject json) {
        try {
            url_ok(url);
            if (compare == null) compare = "false";
            return new ResponseEntity<>(similarityService.simProj(project,compare,threshold,url,json), HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
        }
    }


    // DB

    @CrossOrigin
    @RequestMapping(value = "/DB/AddReqs", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Preprocess a set of requirements", notes = "Processes the input requirements and saves them. It is necessary to compute the similarity score of any pair of requirements. The processing of large requirements can take a long time." +
            " The resulting input stream is not useful. If already exists another requirement with the same id in the database, it is replaced by the new one.<br>" +
            "<br> Example: <em> {\"result\":\"Success!\"} </em>")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=410, message = "Not Found"),
            @ApiResponse(code=411, message = "Bad request"),
            @ApiResponse(code=511, message = "Component Error")})
    public ResponseEntity<?> addRequirements(@ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                             @ApiParam(value="OpenreqJson with requirements", required = true, example = "SQ-132") @RequestBody Requirements input) {
        try {
            url_ok(url);
            return new ResponseEntity<>(similarityService.addRequirements(input,url),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
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
    Testing operations
     */

    @CrossOrigin
    @RequestMapping(value = "/Test", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "Testing result")
    public ResponseEntity<?> testing(@RequestParam("result") MultipartFile file,
                               @RequestParam("info") JSONObject json) {

        System.out.println("Enter");
        try {
            InputStream reader = file.getInputStream();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = reader.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            byte[] byteArray = buffer.toByteArray();

            String text = new String(byteArray, StandardCharsets.UTF_8);

            ControllerTest.setResult(text, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/Clusters", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Clusters")
    public ResponseEntity<?> clusters(@ApiParam(value="Id of the project to compare", required = true, example = "SQ") @RequestParam("project") String project,
                                      @ApiParam(value="Use text attribute in comparison?", required = false, example = "false") @RequestParam("compare") String compare,
                                      @ApiParam(value="Algorithm type", required = false, example = "all/one") @RequestParam("type") String type,
                                      @ApiParam(value="Float between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.3") @RequestParam("threshold") float threshold,
                                      @ApiParam(value="The url where the result of the operation will be returned", required = true, example = "http://localhost:9406/upload/Test") @RequestParam("url") String url,
                                      @ApiParam(value="OpenreqJson with the project and the project's requirements", required = true, example = "SQ-132") @RequestBody JsonProject json) {

        try {
            url_ok(url);
            if (compare == null) compare = "false";
            if (type == null) type = "all";
            return new ResponseEntity<>(similarityService.simCluster(project,compare,threshold,url,type,json),HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.INTERNAL_SERVER_ERROR);
        }
        //TODO improve exception handling
    }

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
        result.put("status", "410");
        result.put("error", "Not Found");
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<?> getResponseBadRequest(BadRequestException e) {
        e.printStackTrace();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("status", "411");
        result.put("error", "Bad request");
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<?> getInternalError(InternalErrorException e) {
        e.printStackTrace();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("status", "510");
        result.put("error", "Server Error");
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<?> getComponentError(ComponentException e) {
        e.printStackTrace();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("status", "511");
        result.put("error", "Component error");
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}