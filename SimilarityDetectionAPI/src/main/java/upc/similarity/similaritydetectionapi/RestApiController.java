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
    @RequestMapping(value = "/SimReqReq", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between two requirements", notes = "")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=410, message = "Not Found"),
            @ApiResponse(code=411, message = "Bad request"),
            @ApiResponse(code=511, message = "Component Error")})
    public ResponseEntity<?> simReqReq(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                       @ApiParam(value="Id of the first requirement to compare", required = true, example = "SQ-132") @RequestParam("req1") String req1,
                                       @ApiParam(value="Id of the second requirement to compare", required = true, example = "SQ-98") @RequestParam("req2") String req2) {
        try {
            return new ResponseEntity<>(similarityService.simReqReq(organization,req1,req2), HttpStatus.OK);
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
    @RequestMapping(value = "/SimReqProject", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between a requirement and all the requirements of a specific project", notes = "")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=410, message = "Not Found"),
            @ApiResponse(code=411, message = "Bad request"),
            @ApiResponse(code=510, message = "Internal Error"),
            @ApiResponse(code=511, message = "Component Error")})
    public ResponseEntity<?> simReqProject(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                           @ApiParam(value="Id of the requirement to compare", required = true, example = "SQ-132") @RequestParam("req") String req,
                                           @ApiParam(value="Id of the project to compare", required = true, example = "SM") @RequestParam("project") String project,
                                           @ApiParam(value="OpenReqJson with the project", required = true) @RequestBody JsonProject input) {
        try {
            return new ResponseEntity<>(similarityService.simReqProj(organization,req,project,input), HttpStatus.OK);
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
    @RequestMapping(value = "/BuildModel", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Builds model", notes = "")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=410, message = "Not Found"),
            @ApiResponse(code=411, message = "Bad request"),
            @ApiResponse(code=511, message = "Component Error")})
    public ResponseEntity<?> buildModel(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                             @ApiParam(value="Use text attribute?", required = false, example = "true") @RequestParam(value = "compare",required = false) boolean compare,
                                             @ApiParam(value="OpenreqJson with requirements", required = true) @RequestBody Requirements input) {
        try {
            similarityService.buildModel(organization,compare,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (BadRequestException e) {
            return getResponseBadRequest(e);
        } catch (NotFoundException e) {
            return getResponseNotFound(e);
        } catch (InternalErrorException e) {
            return getInternalError(e);
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