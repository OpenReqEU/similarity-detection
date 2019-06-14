package upc.similarity.similaritydetectionapi.service;

import upc.similarity.similaritydetectionapi.entity.input_output.JsonProject;
import upc.similarity.similaritydetectionapi.entity.input_output.Requirements;
import upc.similarity.similaritydetectionapi.entity.input_output.ResultId;
import upc.similarity.similaritydetectionapi.exception.*;

import java.util.List;

public interface SimilarityService {

    public ResultId buildModel(String url, String organization, boolean compare, Requirements input) throws InternalErrorException, BadRequestException, NotFoundException;

    public String simReqReq(String organization, String req1, String req2) throws ComponentException;

    public ResultId simReqProject(String url, String organization, double threshold, int max_number, List<String> req, String project, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException;

    public ResultId simProject(String url, String organization, double threshold, int max_number, String project, JsonProject input) throws BadRequestException, InternalErrorException, NotFoundException;

    public ResultId buildModelAndCompute(String url, String organization, boolean compare, double threshold, Requirements input) throws InternalErrorException, BadRequestException;

    public String getResponsePage(String organization, String responseId) throws ComponentException;

    public void deleteOrganizationResponses(String organization) throws ComponentException;

    public void deleteDatabase() throws ComponentException;

}