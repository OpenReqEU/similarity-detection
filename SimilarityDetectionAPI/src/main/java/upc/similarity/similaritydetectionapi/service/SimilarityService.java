package upc.similarity.similaritydetectionapi.service;

import upc.similarity.similaritydetectionapi.entity.Dependency;
import upc.similarity.similaritydetectionapi.entity.input_output.JsonProject;
import upc.similarity.similaritydetectionapi.entity.input_output.Requirements;
import upc.similarity.similaritydetectionapi.exception.*;

import java.util.List;

public interface SimilarityService {

    public void buildModel(String organization, boolean compare, Requirements input) throws InternalErrorException, BadRequestException, NotFoundException;

    public List<Dependency> simReqReq(String organization, String req1, String req2) throws BadRequestException, InternalErrorException, NotFoundException;

    public List<Dependency> simReqProject(String organization, double threshold, int max_number, String req, String project, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException;

    public List<Dependency> simProject(String organization, double threshold, int max_number, String project, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException;

    public void clearDB() throws InternalErrorException, BadRequestException, NotFoundException;
}