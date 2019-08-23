package upc.similarity.similaritydetectionapi.service;

import org.springframework.web.multipart.MultipartFile;
import upc.similarity.similaritydetectionapi.entity.input_output.*;
import upc.similarity.similaritydetectionapi.exception.*;

import java.util.List;

public interface SimilarityService {


    /*
    Main operations
     */

    public ResultId buildModel(String url, String organization, boolean compare, double threshold, Requirements input) throws BadRequestException, NotFoundException;

    public ResultId buildModelAndCompute(String url, String organization, boolean compare, double threshold, Requirements input) throws BadRequestException;

    public ResultId addRequirements(String url, String organization, Requirements input) throws BadRequestException;

    public ResultId deleteRequirements(String url, String organization, Requirements input) throws BadRequestException;

    public String simReqReq(String organization, String req1, String req2) throws ComponentException;

    public ResultId simReqOrganization(String url, String organization, Requirements input) throws InternalErrorException, BadRequestException;

    public ResultId simReqProject(String url, String organization, List<String> req, String project, Projects input) throws NotFoundException, BadRequestException;

    public ResultId simProject(String url, String organization, String project, Projects input) throws NotFoundException, BadRequestException;


    /*
    Cluster operations
     */

    public ResultId buildClusters(String url, String organization, boolean compare, double threshold, MultipartFile input) throws BadRequestException;

    public ResultId buildClustersAndCompute(String url, String organization, boolean compare, double threshold, int maxNumber, MultipartFile input) throws BadRequestException;

    public String simReqClusters(String organization, int maxValue, List<String> input) throws ComponentException;

    public void treatDependencies(String organization, Dependencies dependencies) throws ComponentException;

    public ResultId cronMethod(String url, String organization, ProjectWithDependencies input) throws ComponentException;

    public String getResponsePage(String organization, String responseId) throws ComponentException;


    /*
    Auxiliary operations
     */

    public void deleteOrganizationResponses(String organization) throws ComponentException;

    public void clearOrganization(String organization) throws ComponentException;

    public void clearDatabase() throws ComponentException;

}