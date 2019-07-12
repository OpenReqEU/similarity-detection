package upc.similarity.similaritydetectionapi;

import io.swagger.annotations.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import upc.similarity.similaritydetectionapi.config.Control;
import upc.similarity.similaritydetectionapi.config.TestConfig;
import upc.similarity.similaritydetectionapi.entity.Requirement;
import upc.similarity.similaritydetectionapi.entity.input_output.*;
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


    /*
    Main operations
     */

    @CrossOrigin
    @PostMapping(value = "/BuildModel", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Builds a model with the input requirements", notes = "Builds a model with the entry requirements. " +
            "The generated model is assigned to the specified organization and stored in an internal database. Each organization" +
            " only can have one model at a time. If at the time of generating a new model the corresponding organization already has" +
            " an existing model, it is replaced by the new one.", tags = "Model")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity buildModel(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                     @ApiParam(value="Use text attribute?", required = false, example = "true") @RequestParam(value = "compare",required = false) boolean compare,
                                     @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                     @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                     @ApiParam(value="OpenReqJson with requirements", required = true) @RequestBody Requirements input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.buildModel(url,organization,compare,threshold,input),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/BuildModelAndCompute", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Builds a model with the input requirements and computes them", notes = "<p>Builds a model with the entry requirements. " +
            "The generated model is assigned to the specified organization and stored in an internal database. Each organization" +
            " only can have one model at a time. If at the time of generating a new model the corresponding organization already has" +
            " an existing model, it is replaced by the new one.</p><br><p>Also, it returns an array of dependencies between all possible pairs of " +
            " requirements from the requirements received as input.</p>", tags = "Model")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity buildModelAndCompute(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                               @ApiParam(value="Use text attribute?", required = false, example = "true") @RequestParam(value = "compare",required = false) boolean compare,
                                               @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                               @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                               @ApiParam(value="OpenReqJson with requirements", required = true) @RequestBody Requirements input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.buildModelAndCompute(url,organization,compare,threshold,input),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/AddRequirements", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Add requirements to the model", notes = "<p>Given a list of requirements, the endpoint pre-processes them and adds them to a specified " +
            "organization’s model. If the model has clusters, the endpoint adds each input requirement as a cluster of one requirement. If some of the entry requirements " +
            "were already part of the model, the endpoint will update its information and compare them again as the other entry requirements.\n</p>", tags = "Model")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=404, message = "Not found"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity addRequirements(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                     @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                     @ApiParam(value="OpenReqJson with requirements", required = true) @RequestBody Requirements input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.addRequirements(url,organization,input),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/DeleteRequirements", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Delete requirements from the model", notes = "Given a list of requirements, the endpoint deletes them from a specified organization’s model. " +
            "If the model has clusters, the endpoint deletes each input requirement from his cluster and updates the cluster centroid if the deleted requirement was the " +
            "oldest one. \n", tags = "Model")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=404, message = "Not found"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity deleteRequirements(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                          @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                          @ApiParam(value="OpenReqJson with requirements", required = true) @RequestBody Requirements input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.deleteRequirements(url,organization,input),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/ReqReq", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between two requirements", notes = "Returns a dependency between the two input requirements. The similarity score is computed with the" +
            " model assigned to the specified organization. The two requirements must be in this model.", tags = "Compare")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=404, message = "Not found"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity simReqReq(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                    @ApiParam(value="Id of the first requirement to compare", required = true, example = "UPC-98") @RequestParam("req1") String req1,
                                    @ApiParam(value="Id of the second requirement to compare", required = true, example = "UPC-97") @RequestParam("req2") String req2) {
        try {
            String aux = similarityService.simReqReq(organization,req1,req2);
            return new ResponseEntity<>(aux, HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/ReqOrganization", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between a set of requirements and all the organization requirements", notes = "<p>Adds the input requirements to the organization model and returns " +
            "an array of dependencies between them and all the organization requirements. If any input requirement is already part of the organization's model, it will be overwritten with the new information.</p>", tags = "Compare")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity simReqOrganization(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                             @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                             @ApiParam(value="OpenReqJson with requirements", required = true) @RequestBody Requirements input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.simReqOrganization(url,organization,input),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/ReqProject", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between a list of requirements and all the requirements of a specific project", notes = "<p>Returns an array of dependencies " +
            "between the list of requirements and the project's requirements received as input. The similarity score is computed with the model assigned to the specified organization. " +
            "All the requirements must be inside this model.</p>", tags = "Compare")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=404, message = "Not found"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity simReqProject(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                        @ApiParam(value="Id of the requirements to compare", required = true) @RequestParam("req") List<String> req,
                                        @ApiParam(value="Id of the project to compare", required = true, example = "SM") @RequestParam("project") String project,
                                        @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                        @ApiParam(value="OpenReqJson with the project", required = true) @RequestBody Projects input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.simReqProject(url,organization,req,project,input), HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/Project", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between the requirements of one project", notes = "<p>Returns an array of dependencies between all possible pairs of " +
            "requirements from the project received as input. The similarity score is computed with the model assigned to the specified organization. All the requirements" +
            " must be inside this model.</p>", tags = "Compare")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=404, message = "Not found"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity simProject(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                     @ApiParam(value="Id of the project to compare", required = true, example = "SQ") @RequestParam("project") String project,
                                     @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                     @ApiParam(value="OpenReqJson with the project", required = true) @RequestBody Projects input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.simProject(url,organization,project,input), HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }


    /*
    Cluster operations
     */

    @CrossOrigin
    @PostMapping(value = "/BuildClusters", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Generates the clusters from the input requirements and dependencies", notes = "<p>This method computes the clusters using the existing duplicates. The entry duplicates relations are " +
            "defined by the dependencies with type equal to similar or duplicate and type equal to accepted. All the requirements that do not have duplicates relationships with other requirements" +
            " are considered to be in a cluster of just one requirement. All the requirements are pre-processed and stored in the database.</p>", tags = "Clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity buildClusters(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                        @ApiParam(value="Use text attribute?", required = false, example = "true") @RequestParam(value = "compare",required = false) boolean compare,
                                        @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                        @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                        @ApiParam(value="OpenReqJson with requirements and dependencies", required = true) @RequestBody ProjectWithDependencies input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.buildClusters(url,organization,compare,threshold,input),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/BuildClustersAndCompute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Generates the clusters from the input requirements and dependencies and computes the similarity score between them", notes = "<p>This method computes the clusters using" +
            " the existing duplicates. The entry duplicates relations are defined by the dependencies with type equal to similar or duplicate and type equal to accepted. All the requirements that do not have duplicates relationships with other requirements are considered to be in a cluster of just one requirement. " +
            "All the requirements are pre-processed and stored in the database. Then, we compare each orphan (cluster with only one requirement) with all the requirements of each cluster" +
            " and return the highest similarity score for all the comparisons that are bigger than the established threshold.</p>", tags = "Clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity buildClustersAndCompute(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                                         @ApiParam(value="Use text attribute?", required = false, example = "true") @RequestParam(value = "compare",required = false) boolean compare,
                                                         @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                                         @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                                         @ApiParam(value="OpenReqJson with requirements and dependencies", required = true) @RequestParam("file") MultipartFile file) {
        try {
            if(url != null) urlOk(url);
            List<Requirement> requirements = new ArrayList<>();
            requirements.add(new Requirement());
            ProjectWithDependencies aux = new ProjectWithDependencies(requirements,new ArrayList<>());
            ResultId resultId = new ResultId("12345_67");
            if (organization.equals("TEEEEST")) resultId = similarityService.buildClustersAndCompute(url, "UPC", true, 0.1, aux);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", resultId.getId());
            return new ResponseEntity<>(jsonObject.toString(),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/ReqClusters", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between a set of requirements and all the organization clusters", notes = "<p>The requirements received should already exist and" +
            " should have been already preprocessed, we only get the id of the req as a parameter. It returns a dependencies array with the highest similarity comparison between " +
            "each input requirement and all the requirements of each cluster (it returns both accepted and proposed dependencies, but not the rejected ones). The comparisons are done" +
            " with all the requirements in the database. If the number of maximum dependencies to be returned is received as parameter (maxNumber), we only return the maxNumber " +
            "dependencies with highest score. When maxNumber is equal to 0 only the accepted dependencies are returned and when maxNumber is equal to -1 all the accepted and proposed " +
            "dependencies of the specified requirement are returned. This method is synchronous.</p>", tags = "Clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity simReqClusters(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                         @ApiParam(value="Requirement id", required = true, example = "UPC-1") @RequestParam(value = "requirementId") String requirementId,
                                         @ApiParam(value="Max number of dependencies to return", required = false, example = "10") @RequestParam(value = "maxNumber", required = false) Integer maxNumber) {
        try {
            if (maxNumber == null) maxNumber = -1;
            List<String> aux = new ArrayList<>();
            aux.add(requirementId);
            return new ResponseEntity<>(similarityService.simReqClusters(organization,maxNumber,aux), HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/TreatAcceptedAndRejectedDependencies", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Updates the organization clusters with the input dependencies", notes = "<p>Given a set of accepted and rejected dependencies, updates the clusters and dependencies accordingly. This method is synchronous.</p>", tags = "Clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity treatAcceptedAndRejectedDependencies(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                                               @ApiParam(value="OpenReqJson with dependencies", required = true) @RequestBody Dependencies input) {

        try {
            similarityService.treatDependencies(organization, input);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/BatchProcess", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Updates the organization clusters with the input requirements and dependencies", notes = "<p>Given a set of updates done in the requirements (see next list), updates the clusters accordingly.</p>" +
            "<p><ul>" +
            "<li>New requirements: The input requirements that do not pertain to the organization's model are considered to be new requirements. The method stores the pre-processing of the new requirements and puts the new requirements as clusters of 1 requirement.</li>" +
            "<li>Updated requirements: The input requirements with a title or text different from the one stored in the database are considered updated requirements. The method updates their pre-process in the database and updates the organization clusters accordingly.</li>" +
            "<li>New dependencies: The input similarity dependencies that do not pertain to the organization's model are considered to be new dependencies. The method uses the accepted and rejected dependencies to update the organization clusters.</li>" +
            "<li>Removed dependencies: The organization dependencies that do not appear in the input similarity dependencies are considered removed dependencies. The method updates them as rejected and changes the clusters accordingly.</li>" +
            "</ul></p>", tags = "Clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity batchProcess(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                       @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                       @ApiParam(value="OpenReqJson with requirements and dependencies", required = true) @RequestBody ProjectWithDependencies input) {

        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.cronMethod(url, organization, input), HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }


    /*
    Auxiliary operations
     */

    @CrossOrigin
    @GetMapping(value = "/GetResponse", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Returns result dependencies of the other operations", notes = "<p>Returns the result json of the AddReqs, Project, ReqProject and AddReqsAndCompute methods. The result is a " +
            "json object formed by a status attribute. If the status is 200 the json also contains an array of dependencies which are returned in patches of 20,000 . Each time this operation is called, the following 20,000 dependencies of the indicated response will be" +
            " returned. An empty json will be returned when no more dependencies are left. Nevertheless, if the status attribute is not equal to 200, the json contains the exception message. </p>", tags = "Auxiliary methods")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=423, message = "The computation is not finished yet"),
            @ApiResponse(code=404, message = "Not found"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity getResponsePage(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                             @ApiParam(value="Response identifier", required = true, example = "12345678_89") @RequestParam("response") String responseId) {
        try {
            return new ResponseEntity<>(similarityService.getResponsePage(organization,responseId), HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @DeleteMapping(value = "/DeleteOrganizationResponses", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Deletes the organization responses", notes = "<p>Deletes the organization responses that are finished from the database.</p>", tags = "Auxiliary methods")
    @ApiResponses(value = {
            @ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=404, message = "Not found"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity deleteOrganizationResponses(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization) {
        try {
            similarityService.deleteOrganizationResponses(organization);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @DeleteMapping(value = "/ClearOrganization", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Clear organization data", notes = "<p>Clears all the data from an specified organization. If this method is called while a calculation is being carried out with the chosen organization, unforeseen results may occur.</p>", tags = "Auxiliary methods")
    @ApiResponses(value = {
            @ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=404, message = "Not found"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity clearOrganization(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization) {
        try {
            similarityService.clearOrganization(organization);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @DeleteMapping(value = "/ClearDatabase", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Clear all data from the database", notes = "<p>Clears all data from the database. If this method is called while a calculation is being carried out, unforeseen results may occur.</p>", tags = "Auxiliary methods")
    @ApiResponses(value = {
            @ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity clearDatabase() {
        try {
            similarityService.clearDatabase();
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/Test", produces = MediaType.APPLICATION_JSON_VALUE)
    public void test(@RequestBody String result) {
        JSONObject json = new JSONObject(result);
        TestConfig testConfig = TestConfig.getInstance();
        testConfig.setResult(json);
        testConfig.setComputationFinished(true);
    }


    /*
    Private operations
     */

    private void urlOk(String url) throws BadRequestException {
        try {
            new URL(url).toURI();
        } catch (Exception e) {
            throw new BadRequestException("Output server doesn't exist");
        }
    }

    private ResponseEntity getComponentError(ComponentException e) {
        if (e.getStatus() == 500) Control.getInstance().showStackTrace(e);
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("status", e.getStatus()+"");
        result.put("error", e.getError());
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.valueOf(e.getStatus()));
    }
}