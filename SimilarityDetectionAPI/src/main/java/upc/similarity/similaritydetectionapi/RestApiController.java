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
import upc.similarity.similaritydetectionapi.entity.input_output.*;
import upc.similarity.similaritydetectionapi.exception.*;
import upc.similarity.similaritydetectionapi.service.SimilarityService;

import java.net.URL;
import java.util.*;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping(value = "upc/similarity-detection")
public class RestApiController {

    //TODO update README and yaml file

    @Autowired
    SimilarityService similarityService;


    /*
    Similarity without clusters
     */

    @CrossOrigin
    @PostMapping(value = "/BuildModel", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Builds a tf-idf model with the input requirements.", notes = "<p><i>Asynchronous</i> method.</p><p>Builds a tf-idf model with the requirements specified in the JSON object. " +
            "The generated model is assigned to the specified organization and stored in an internal database. Each organization" +
            " can have only one model. If at the time of generating a new model the corresponding organization already has" +
            " an existing model, it is replaced by the new one. The user can choose whether to use only the name of the requirements for constructing the model, or to use also the text of the requirements.</p>", tags = "Similarity without clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity buildModel(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                     @ApiParam(value="Use the text field of the requirements to construct the model", required = false, example = "true") @RequestParam(value = "compare",required = false) boolean compare,
                                     @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                     @ApiParam(value="OpenReq JSON with requirements", required = true) @RequestBody RequirementsModel input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.buildModel(url,organization,compare,input),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/BuildModelAndCompute", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Builds a tf-idf model with the input requirements and returns the similarity scores among all the possible pairs of requirements.", notes = "<p><i>Asynchronous</i> method.</p><p>Builds a tf-idf model with the requirements specified in the JSON object." +
            "The generated model is assigned to the specified organization and stored in an internal database. Each organization" +
            " can have only one model. If at the time of generating a new model the corresponding organization already has" +
            " an existing model, it is replaced by the new one. The user can choose whether to use only the name of the requirements for constructing the model, or to use also the text of the requirements. </p><br><p>This method returns an array of dependencies containing the similarities that are greater than the established threshold between all possible pairs of " +
            " requirements received as input.</p>", tags = "Similarity without clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity buildModelAndCompute(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                               @ApiParam(value="Use the text field of the requirements to construct the model", required = false, example = "true") @RequestParam(value = "compare",required = false) boolean compare,
                                               @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                               @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                               @ApiParam(value="OpenReq JSON with requirements", required = true) @RequestBody RequirementsModel input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.buildModelAndCompute(url,organization,compare,threshold,input),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/AddRequirements", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Adds requirements to the tf-idf model of an organization.", notes = "<p><i>Asynchronous</i> method.</p><p>Given a list of requirements, the endpoint pre-processes them and adds them to a specified " +
            "organization’s tf-idf model. If some of the entry requirements " +
            "were already part of the model, the endpoint will update its information.\n</p>", tags = "Similarity without clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=404, message = "Not found"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity addRequirements(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                          @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                          @ApiParam(value="OpenReq JSON with requirements", required = true) @RequestBody RequirementsModel input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.addRequirements(url,organization,input),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/DeleteRequirements", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Deletes requirements from the tf-idf model of an organization", notes = "<p><i>Asynchronous</i> method.</p><p>Given a list of requirements, the endpoint deletes them from a specified organization’s tf-idf model. \n</p>", tags = "Similarity without clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=404, message = "Not found"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity deleteRequirements(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                             @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                             @ApiParam(value="OpenReq JSON with requirements", required = true) @RequestBody RequirementsModel input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.deleteRequirements(url,organization,input),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/ReqReq", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between two requirements. ", notes = "<p><i>Synchronous</i> method.</p><p>Returns a dependency between the two input requirements. The similarity score is computed with the" +
            " tf-idf model assigned to the specified organization. The two requirements must be in this model.</p>", tags = "Similarity without clusters")
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
    @PostMapping(value = "/ReqOrganization", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between a set of requirements and all the requirements of the specified organization. ", notes = "<p><i>Asynchronous</i> method.</p><p>Compares the organization's requirements that appear in the input array of ids with all the organization's requirements. " +
            "Returns an array with all the similarity dependencies that are above the specified threshold. Also the requirements of the input list are compared between each other. The input ids that do not belong to any organization requirement are ignored.</p>", tags = "Similarity without clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity simReqOrganization(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                             @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                             @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                             @ApiParam(value="OpenReq JSON with requirements", required = true, example = "UPC-1") @RequestParam("req") List<String> input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.simReqOrganization(url,organization,threshold,input),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/NewReqOrganization", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between a set of input requirements and all the requirements of the specified organization. ", notes = "<p><i>Asynchronous</i> method.</p><p>Adds the input requirements to the organization's tf-idf model and returns " +
            "an array of similarity dependencies with a score above the specified threshold between them and all the organization requirements. Also the requirements of the input list are compared between each other. If any input requirement is already part of the organization's model, it will be overwritten with the new information.</p>", tags = "Similarity without clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity simNewReqOrganization(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                             @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                             @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                             @ApiParam(value="OpenReq JSON with requirements", required = true) @RequestBody RequirementsModel input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.simNewReqOrganization(url,organization,threshold,input),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/ReqProject", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between a list of requirements and all the requirements of an specific project.", notes = "<p><i>Asynchronous</i> method.</p><p>Returns an array of similarity dependencies " +
            "with a score above the specified threshold between the list of requirements and the project's requirements received as input. Also the requirements of the input list are compared between each other. The similarity score is computed with the tf-idf model assigned to the specified organization. " +
            "The input ids that do not belong to any organization requirement are ignored.</p>", tags = "Similarity without clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=404, message = "Not found"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity simReqProject(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                        @ApiParam(value="Id of the requirements to compare", required = true) @RequestParam("req") List<String> req,
                                        @ApiParam(value="Id of the project to compare", required = true, example = "UPC-P1") @RequestParam("project") String project,
                                        @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                        @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                        @ApiParam(value="OpenReq JSON with the project", required = true) @RequestBody ProjectsModel input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.simReqProject(url,organization,req,project,threshold,input), HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/Project", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between the requirements of one project.", notes = "<p><i>Asynchronous</i> method.</p><p>Returns an array of similarity dependencies with a score above the specified threshold" +
            " between all possible pairs of requirements from the project received as input. The similarity score is computed with the tf-idf model assigned to the specified organization. The input ids that do not belong to any organization requirement are ignored" +
            ".</p>", tags = "Similarity without clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=404, message = "Not found"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity simProject(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                     @ApiParam(value="Id of the project to compare", required = true, example = "UPC-P1") @RequestParam("project") String project,
                                     @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the added dependencies should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                     @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                     @ApiParam(value="OpenReq JSON with the project specifying the id of the requirements the project has", required = true) @RequestBody ProjectsModel input) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.simProject(url,organization,project,threshold,input), HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }


    /*
    Similarity with clusters
     */


    @CrossOrigin
    @PostMapping(value = "/BuildClusters", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Generates the clusters using the input requirements and dependencies.", notes = "<p><i>Asynchronous</i> method.</p><p>This method computes the clusters using the existing duplicates. These duplicates relations are defined by " +
            "the dependencies with type equal to <i>similar</i> or <i>duplicates</i> and status equal to <i>accepted</i>. All the requirements that do not have duplicates relationships with other requirements are considered to be in a cluster of just one " +
            "requirement. All the requirements are pre-processed and stored in the database, together with their corresponding tf-idf model and the clusters information. The user can choose whether to use only the name of the requirements for constructing the tf-idf model," +
            " or to use also the text of the requirements. Finally, all the requirements are compared with all the requirements of other clusters of the organization, and those similarities with score greater than the established threshold for each requirement are stored in the database.</p>", tags = "Similarity with clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity buildClusters(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                        @ApiParam(value="Use the text field of the requirements to construct the model", required = false, example = "true") @RequestParam(value = "compare",required = false) boolean compare,
                                        @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the proposed dependencies should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                        @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                        @ApiParam(value="OpenReq JSON with requirements and dependencies", required = true) @RequestParam("file") MultipartFile file) {
        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.buildClusters(url,organization,compare,threshold,file),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/BuildClustersAndCompute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Generates the clusters using the input requirements and dependencies, and returns the similarity score between them.", notes =
            "<p><i>Asynchronous</i> method.</p><p>This method computes the clusters using the existing duplicates. These duplicates relations are defined by the dependencies with type equal to <i>similar</i> or <i>duplicates</i> and type equal to <i>accepted</i>." +
                    " All the requirements that do not have duplicates relationships with other requirements are considered to be in a cluster of just one requirement. All the requirements are pre-processed and stored in the database, together with their corresponding " +
                    "tf-idf model and the clusters information. The user can choose whether to use only the name of the requirements for constructing the tf-idf model, or to use also the text of the requirements. Finally, all the requirements are compared with all the " +
                    "requirements of other clusters of the organization, and those similarities with score greater than the established threshold for each requirement are stored in the database. It returns for each requirement the highest similarity score for each cluster " +
                    "(only if they are greater than the established threshold). If the number of maximum dependencies to be returned is received as parameter (maxNumber), the method only returns the <i>maxNumber</i>" +
                    " of dependencies with highest score for each requirement. When <i>maxNumber</i> is equal to 0 only the accepted dependencies are returned and when maxNumber is lower than 0 or not specified, all the accepted and proposed" +
                    " dependencies of each requirement are returned.</p>", tags = "Similarity with clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity buildClustersAndCompute(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                                  @ApiParam(value="Use the text field of the requirements to construct the model", required = false, example = "true") @RequestParam(value = "compare",required = false) boolean compare,
                                                  @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the proposed dependencies should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                                  @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                                  @ApiParam(value="Max number of dependencies to return for each requirement", required = false, example = "10") @RequestParam(value = "maxNumber", required = false) Integer maxNumber,
                                                  @ApiParam(value="OpenReq JSON with requirements and dependencies", required = true) @RequestParam("file") MultipartFile file) {
        try {
            if (maxNumber == null) maxNumber = -1;
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.buildClustersAndCompute(url, organization, compare, threshold, maxNumber, file),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/ReqClusters", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Similarity comparison between a requirement and all the organization's clusters.", notes = "<p><i>Synchronous</i> method.</p><p>The requirements received should already exist for the organization and" +
            " should have already been preprocessed; the method only needs the id of the requirement (received as a parameter). It returns a dependencies array with the highest similarity comparison between " +
            "each input requirement and all the requirements of each cluster (it returns both accepted and proposed dependencies, but not the rejected ones). The comparisons are done" +
            " with all the requirements (i.e.m clusters) in the database for this organization. If the number of maximum dependencies to be returned is received as parameter (maxNumber), the method only returns the <i>maxNumber</i> " +
            "of dependencies with highest score. When <i>maxNumber</i> is equal to 0 only the accepted dependencies are returned and when maxNumber is lower than 0 or not specified, all the accepted and proposed " +
            "dependencies of the requirement received as input are returned.</p>", tags = "Similarity with clusters")
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
    @ApiOperation(value = "Updates the organization clusters with the input dependencies.", notes = "<p><i>Synchronous</i> method.</p><p>Given a set of <i>accepted</i> and <i>rejected</i> dependencies, updates the clusters " +
            "and dependencies accordingly. </p>", tags = "Similarity with clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity treatAcceptedAndRejectedDependencies(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                                               @ApiParam(value="OpenReqJson with dependencies", required = true) @RequestBody DependenciesModel input) {

        try {
            similarityService.treatDependencies(organization, input);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @PostMapping(value = "/BatchProcess", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Updates the organization clusters with the input requirements and dependencies.", notes = "<p><i>Asynchronous</i> method.</p><p>Given a set of updates done in the requirements (see next list), updates the clusters accordingly.</p>" +
            "<p><ul>" +
            "<li>New requirements: The input requirements that do not pertain to the organization's model are considered to be new requirements. The method stores the pre-processing of the new requirements, adds them to the tf-idf model, and puts the new requirements as clusters of one requirement.</li>" +
            "<li>Updated requirements: The input requirements with a title or text different from the one stored in the database are considered updated requirements. The method updates their pre-processing in the database and updates the organization clusters accordingly.</li>" +
            "<li>New dependencies: The input similarity dependencies that do not pertain to the organization's model are considered to be new dependencies. The method uses the accepted and rejected dependencies to update the organization clusters.</li>" +
            "<li>Removed dependencies: The organization dependencies that do not appear in the input similarity dependencies are considered removed dependencies. The method updates them as rejected and changes the clusters accordingly.</li>" +
            "</ul></p>", tags = "Similarity with clusters")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity batchProcess(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                       @ApiParam(value="The url where the result of the operation will be returned", required = false, example = "http://localhost:9406/upload/PostResult") @RequestParam(value = "url", required = false) String url,
                                       @ApiParam(value="OpenReq JSON with requirements and dependencies", required = true) @RequestBody ProjectWithDependencies input) {

        try {
            if(url != null) urlOk(url);
            return new ResponseEntity<>(similarityService.batchProcess(url, organization, input), HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    /*
    Auxiliary methods
     */

    @CrossOrigin
    @GetMapping(value = "/GetResponse", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Returns the dependencies that are the result of the other operations.", notes = "<p><i>Synchronous</i> method.</p><p>Returns the result JSON of the asynchronous methods of this API (i.e., all the methods except <i>ReqReq</i>, <i>ReqClusters</i>, <i>TreatAcceptedAndRejectedDependencies</i> and the auxiliary methods). The result is a " +
            "JSON object formed by a status attribute. If the status is 200 the JSON also contains an array of dependencies which are returned in batches of 20,000 . Each time this operation is called, the following 20,000 dependencies of the indicated response will be" +
            " returned. An empty JSON will be returned when no more dependencies are left. Nevertheless, if the status attribute is not equal to 200, the json contains the exception message. </p>", tags = "Auxiliary methods")
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
    @GetMapping(value = "/GetOrganizationInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Returns the main organization information", notes = "<p><i>Synchronous</i> method.</p>Returns the main information of the specified organization " +
            "(name, threshold, if it uses the text attribute and if it has clusters). Also the method returns the async operations that have not finished yet and the pending responses to read.<p></p>", tags = "Auxiliary methods")
    @ApiResponses(value = {@ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=404, message = "Not found"),
            @ApiResponse(code=500, message = "Internal error")})
    public ResponseEntity getOrganizationInfo(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization) {
        try {
            return new ResponseEntity<>(similarityService.getOrganizationInfo(organization), HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @CrossOrigin
    @DeleteMapping(value = "/DeleteOrganizationResponses", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Deletes the organization's responses. ", notes = "<p><i>Synchronous</i> method.</p><p>Deletes the organization responses that are finished (i.e., their computation is finished) from the database.</p>", tags = "Auxiliary methods")
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
    @DeleteMapping(value = "/DeleteOrganizationData", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Deletes the organization's requirements data.", notes = "<p><i>Synchronous</i> method.</p><p>Deletes all the models data (tf-idf and/or clusters) of the specified organization. If this method is called while a calculation is being carried out with the chosen organization, " +
            "unforeseen results may occur.</p>", tags = "Auxiliary methods")
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
    @ApiOperation(value = "Clears all data from the database.", notes = "<p><i>Synchronous</i> method.</p><p>Clears all data from the database. If this method is called while a calculation is being carried out, unforeseen results may occur.</p>" +
            "", tags = "Auxiliary methods")
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

    //Testing accuracy
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
        if (e.getStatus() == 500) Control.getInstance().showErrorMessage(e.getMessage());
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("status", e.getStatus()+"");
        result.put("error", e.getError());
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.valueOf(e.getStatus()));
    }
}