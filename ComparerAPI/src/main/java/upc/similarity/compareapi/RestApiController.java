package upc.similarity.compareapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.input.Clusters;
import upc.similarity.compareapi.entity.input.ProjectProject;
import upc.similarity.compareapi.entity.input.ReqProject;
import upc.similarity.compareapi.entity.exception.*;
import upc.similarity.compareapi.service.CompareService;

import java.util.List;

@RestController
@RequestMapping(value = "/upc/Compare")
public class RestApiController {

    @Autowired
    CompareService compareService;


    /*
    Similarity without clusters
     */

    @PostMapping(value = "/BuildModel")
    public ResponseEntity buildModel(@RequestParam("organization") String organization,
                                     @RequestParam("compare") boolean compare,
                                     @RequestParam("responseId") String responseId,
                                     @RequestBody List<Requirement> input) {
        try {
            compareService.buildModel(responseId,compare,organization,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @PostMapping(value = "/BuildModelAndCompute")
    public ResponseEntity buildModelAndCompute(@RequestParam("organization") String organization,
                                               @RequestParam("compare") boolean compare,
                                               @RequestParam("responseId") String responseId,
                                               @RequestParam("threshold") double threshold,
                                               @RequestParam("maxDeps") int maxNumDeps,
                                               @RequestBody List<Requirement> input) {
        try {
            compareService.buildModelAndCompute(responseId,compare,organization,threshold,input,maxNumDeps);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @PostMapping(value = "/AddRequirements")
    public ResponseEntity addRequirements(@RequestParam("organization") String organization,
                                     @RequestParam("responseId") String responseId,
                                     @RequestBody List<Requirement> input) {
        try {
            compareService.addRequirements(responseId,organization,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @PostMapping(value = "/DeleteRequirements")
    public ResponseEntity deleteRequirements(@RequestParam("organization") String organization,
                                     @RequestParam("responseId") String responseId,
                                     @RequestBody List<Requirement> input) {
        try {
            compareService.deleteRequirements(responseId,organization,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @PostMapping(value = "/SimReqReq")
    public ResponseEntity simReqReq(@RequestParam("organization") String organization,
                                       @RequestParam("req1") String req1,
                                       @RequestParam("req2") String req2) {
        try {
            return new ResponseEntity<>(compareService.simReqReq(organization,req1,req2),HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @PostMapping(value = "/SimReqOrganization")
    public ResponseEntity simReqOrganization(@RequestParam("organization") String organization,
                                             @RequestParam("responseId") String responseId,
                                             @RequestParam("threshold") double threshold,
                                             @RequestParam("maxDeps") int maxNumDeps,
                                             @RequestBody List<String> requirements) {
        try {
            compareService.simReqOrganization(responseId,organization,threshold,requirements,maxNumDeps);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @PostMapping(value = "/SimNewReqOrganization")
    public ResponseEntity simNewReqOrganization(@RequestParam("organization") String organization,
                                             @RequestParam("responseId") String responseId,
                                             @RequestParam("threshold") double threshold,
                                             @RequestParam("maxDeps") int maxNumDeps,
                                             @RequestBody List<Requirement> requirements) {
        try {
            compareService.simNewReqOrganization(responseId,organization,threshold,requirements,maxNumDeps);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @PostMapping(value = "/SimReqProject")
    public ResponseEntity simReqProject(@RequestParam("organization") String organization,
                                        @RequestParam("responseId") String responseId,
                                        @RequestParam("threshold") double threshold,
                                        @RequestParam("maxDeps") int maxNumDeps,
                                        @RequestBody ReqProject projectRequirements) {
        try {
            compareService.simReqProject(responseId,organization,threshold,projectRequirements,maxNumDeps);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @PostMapping(value = "/SimProject")
    public ResponseEntity simProject(@RequestParam("organization") String organization,
                                     @RequestParam("responseId") String responseId,
                                     @RequestParam("threshold") double threshold,
                                     @RequestParam("maxDeps") int maxNumDeps,
                                     @RequestBody List<String> projectRequirements) {
        try {
            compareService.simProject(responseId,organization,threshold,projectRequirements,maxNumDeps);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @PostMapping(value = "/SimProjectProject")
    public ResponseEntity simProjectProject(@RequestParam("organization") String organization,
                                     @RequestParam("responseId") String responseId,
                                     @RequestParam("threshold") double threshold,
                                     @RequestParam("maxDeps") int maxNumDeps,
                                     @RequestBody ProjectProject projects) {
        try {
            compareService.simProjectProject(responseId,organization,threshold,projects,maxNumDeps);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }


    /*
    Similarity with clusters
     */

    @PostMapping(value = "/BuildClusters")
    public ResponseEntity buildClusters(@RequestParam("organization") String organization,
                                        @RequestParam("compare") boolean compare,
                                        @RequestParam("useComponent") boolean useComponent,
                                        @RequestParam("threshold") double threshold,
                                        @RequestParam("responseId") String responseId,
                                        @RequestParam("file") MultipartFile file) {
        try {
            compareService.buildClusters(responseId, compare, useComponent, threshold, organization, file);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @PostMapping(value = "/BuildClustersAndCompute")
    public ResponseEntity buildClustersAndCompute(@RequestParam("organization") String organization,
                                                  @RequestParam("compare") boolean compare,
                                                  @RequestParam("useComponent") boolean useComponent,
                                                  @RequestParam("responseId") String responseId,
                                                  @RequestParam("threshold") double threshold,
                                                  @RequestParam("maxNumber") int maxNumber,
                                                  @RequestParam("file") MultipartFile file) {
        try {
            compareService.buildClustersAndCompute(responseId,compare, useComponent, organization,threshold,maxNumber, file);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @PostMapping(value = "/SimReqClusters")
    public ResponseEntity simReqClusters(@RequestParam("organization") String organization,
                                         @RequestParam("maxValue") int maxValue,
                                         @RequestBody List<String> requirements) {
        try {
            return new ResponseEntity<>(compareService.simReqClusters(organization,requirements,maxValue),HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @PostMapping(value = "/TreatAcceptedAndRejectedDependencies")
    public ResponseEntity treatAcceptedAndRejectedDependencies(@RequestParam("organization") String organization,
                                         @RequestBody List<Dependency> dependencies) {
        try {
            compareService.treatAcceptedAndRejectedDependencies(organization, dependencies);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @PostMapping(value = "/BatchProcess")
    public ResponseEntity batchProcess(@RequestParam("organization") String organization,
                                       @RequestParam("responseId") String responseId,
                                       @RequestBody Clusters input) {
        try {
            compareService.batchProcess(responseId,organization,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }


    /*
    Auxiliary methods
     */

    @GetMapping(value = "/GetResponsePage")
    public ResponseEntity getResponsePage(@RequestParam("organization") String organization,
                                        @RequestParam("responseId") String responseId) {
        try {
            return new ResponseEntity<>(compareService.getResponsePage(organization,responseId),HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @GetMapping(value = "/GetOrganizationInfo")
    public ResponseEntity getResponsePage(@RequestParam("organization") String organization) {
        try {
            return new ResponseEntity<>(compareService.getOrganizationInfo(organization),HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @DeleteMapping(value = "/ClearOrganizationResponses")
    public ResponseEntity clearOrganizationResponses(@RequestParam("organization") String organization) {
        try {
            compareService.deleteOrganizationResponses(organization);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @DeleteMapping(value = "/ClearOrganization")
    public ResponseEntity clearOrganization(@RequestParam("organization") String organization) {
        try {
            compareService.deleteOrganization(organization);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

    @DeleteMapping(value = "/ClearDatabase")
    public ResponseEntity clearDatabase() {
        try {
            compareService.clearDatabase();
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ComponentException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }

}