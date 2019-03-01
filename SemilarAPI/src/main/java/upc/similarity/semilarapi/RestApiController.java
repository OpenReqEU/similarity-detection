package upc.similarity.semilarapi;


import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import upc.similarity.semilarapi.entity.input.PairReq;
import upc.similarity.semilarapi.entity.input.ProjOp;
import upc.similarity.semilarapi.entity.input.ReqProjOp;
import upc.similarity.semilarapi.entity.input.Requirements;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.InternalErrorException;
import upc.similarity.semilarapi.service.SemilarService;

import java.sql.SQLException;

@RestController
@RequestMapping(value = "/")
public class RestApiController {

    @Autowired
    SemilarService semilarService;

    //Similarity
    @RequestMapping(value = "/upc/Semilar/PairSim", method = RequestMethod.POST)
    public ResponseEntity<?> similarity(@RequestParam("compare") String compare,
                                        @RequestParam("filename") String filename,
                                        @RequestBody PairReq input) {
        try {
            semilarService.similarity(compare,filename,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(411));
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        } catch (InternalErrorException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(510));
        }
    }

    @RequestMapping(value = "/upc/Semilar/ReqProjSim", method = RequestMethod.POST)
    public ResponseEntity<?> similarityReqProj(@RequestParam("compare") String compare,
                                               @RequestParam("threshold") float threshold,
                                               @RequestParam("filename") String filename,
                                               @RequestBody ReqProjOp input) {
        try {
            semilarService.similarityReqProj(compare,threshold,filename,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(510));
        }
    }

    @RequestMapping(value = "/upc/Semilar/ProjSim", method = RequestMethod.POST)
    public ResponseEntity<?> similarityProj(@RequestParam("compare") String compare,
                                            @RequestParam("threshold") float threshold,
                                            @RequestParam("filename") String filename,
                                            @RequestBody ProjOp input) {
        try {
            /*if (input.getRequirements().size() < 1000)*/ semilarService.similarityProj(compare,threshold,filename,input);
            //else semilarService.similarityProj_Large(compare,threshold,filename,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(510));
        }
    }
    @RequestMapping(value = "/upc/Semilar/ClusterSim", method = RequestMethod.POST)
    public ResponseEntity<?> similarityCluster(@RequestParam("compare") String compare,
                                            @RequestParam("threshold") float threshold,
                                            @RequestParam("filename") String filename,
                                               @RequestParam("type") String type,
                                            @RequestBody ProjOp input) {
        try {
            semilarService.similarityCluster(compare,threshold,filename,type,input);
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (InternalErrorException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(510));
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        }
    }


    @RequestMapping(value = "/upc/Semilar/Testing", method = RequestMethod.POST)
    public ResponseEntity<?> Testing(@RequestParam("compare") String compare,
                                        @RequestParam("filename") String filename,
                                        @RequestBody PairReq input) {
        try {
            JSONObject result = semilarService.testing(compare,filename,input);
            return new ResponseEntity<>(result.toString(),HttpStatus.OK);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(411));
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(e.getStatus()));
        } catch (InternalErrorException e) {
            return new ResponseEntity<>(e,HttpStatus.valueOf(510));
        }
    }


    //Database
    @RequestMapping(value = "/upc/Semilar/Preprocess", method = RequestMethod.POST)
    public ResponseEntity<?> preprocess(@RequestBody Requirements input) {
        System.out.println("Preprocessing");
        try {
            semilarService.savePreprocessed(input.getRequirements());
            return new ResponseEntity<>(null,HttpStatus.OK);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e,HttpStatus.valueOf(411));
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