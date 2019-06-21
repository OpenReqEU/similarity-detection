package upc.similarity.similaritydetectionapi.service;

import upc.similarity.similaritydetectionapi.entity.input_output.ProjectWithDependencies;
import upc.similarity.similaritydetectionapi.entity.input_output.Projects;
import upc.similarity.similaritydetectionapi.entity.input_output.Requirements;
import upc.similarity.similaritydetectionapi.entity.input_output.ResultId;
import upc.similarity.similaritydetectionapi.exception.*;

import java.util.List;

public interface SimilarityService {

    public ResultId buildModel(String url, String organization, boolean compare, Requirements input) throws InternalErrorException, BadRequestException, NotFoundException;

    public ResultId addRequirements(String url, String organization, boolean compare, Requirements input) throws InternalErrorException, BadRequestException;

    public ResultId deleteRequirements(String url, String organization, Requirements input) throws InternalErrorException, BadRequestException;

    public String simReqReq(String organization, String req1, String req2) throws ComponentException;

    public ResultId simReqProject(String url, String organization, double threshold, int maxNumber, List<String> req, String project, Projects input) throws BadRequestException, InternalErrorException, NotFoundException;

    public ResultId simProject(String url, String organization, double threshold, int maxNumber, String project, Projects input) throws BadRequestException, InternalErrorException, NotFoundException;

    public ResultId buildModelAndCompute(String url, String organization, boolean compare, double threshold, Requirements input) throws InternalErrorException, BadRequestException;

    public ResultId buildClustersAndComputeOrphans(String url, String organization, boolean compare, double threshold, ProjectWithDependencies input) throws InternalErrorException, BadRequestException;

    public ResultId buildClusters(String url, String organization, boolean compare, ProjectWithDependencies input) throws InternalErrorException, BadRequestException;

    public ResultId simReqOrganization(String url, String organization, boolean compare, double threshold, Requirements input) throws InternalErrorException, BadRequestException;

    public ResultId simReqClusters(String url, String organization, boolean compare, double threshold, Requirements input) throws InternalErrorException, BadRequestException;

    public String getResponsePage(String organization, String responseId) throws ComponentException;

    public void deleteOrganizationResponses(String organization) throws ComponentException;

    public void deleteDatabase() throws ComponentException;

}