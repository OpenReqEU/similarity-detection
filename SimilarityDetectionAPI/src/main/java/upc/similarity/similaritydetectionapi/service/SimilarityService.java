package upc.similarity.similaritydetectionapi.service;

import upc.similarity.similaritydetectionapi.entity.input_output.Result_id;
import upc.similarity.similaritydetectionapi.entity.input_output.JsonProject;
import upc.similarity.similaritydetectionapi.entity.input_output.JsonReqReq;
import upc.similarity.similaritydetectionapi.entity.input_output.Requirements;
import upc.similarity.similaritydetectionapi.exception.*;

import java.util.List;

public interface SimilarityService {

    // Req - Req
    public Result_id simReqReq(String req1, String req2, String compare, String url, JsonReqReq input) throws BadRequestException, InternalErrorException, NotFoundException;

    // Req - Project
    public Result_id simReqProj(List<String> req, String project, String compare, float threshold, String url, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException;

    // Project
    public Result_id simProj(String project, String compare, float threshold, String url, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException;

    // Clusters
    public Result_id simCluster(String project, String compare, float threshold, String url, String type, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException;

    // DB
    public Result_id addRequirements(Requirements input, String url) throws ComponentException, BadRequestException, NotFoundException;

    void clearDB() throws SemilarException, BadRequestException, NotFoundException;
}