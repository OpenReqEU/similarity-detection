package upc.similarity.similaritydetectionapi.service;

import org.springframework.web.multipart.MultipartFile;
import upc.similarity.similaritydetectionapi.entity.input_output.*;
import upc.similarity.similaritydetectionapi.exception.*;

import java.util.List;

public interface SimilarityService {


    /*
    Main operations
     */

    ResultId buildModel(String url, String organization, boolean compare, RequirementsModel input) throws BadRequestException, NotFoundException;

    ResultId buildModelAndCompute(String url, String organization, boolean compare, double threshold, RequirementsModel input) throws BadRequestException;

    ResultId addRequirements(String url, String organization, RequirementsModel input) throws BadRequestException;

    ResultId deleteRequirements(String url, String organization, RequirementsModel input) throws BadRequestException;

    String simReqReq(String organization, String req1, String req2) throws ComponentException;

    ResultId simReqOrganization(String url, String organization, double threshold, List<String> input) throws InternalErrorException, BadRequestException;

    ResultId simNewReqOrganization(String url, String organization, double threshold, RequirementsModel input) throws InternalErrorException, BadRequestException;

    ResultId simReqProject(String url, String organization, List<String> req, String project, double threshold, ProjectsModel input) throws NotFoundException, BadRequestException;

    ResultId simProject(String url, String organization, String project, double threshold, ProjectsModel input) throws NotFoundException, BadRequestException;


    /*
    Cluster operations
     */

    ResultId buildClusters(String url, String organization, boolean compare, double threshold, MultipartFile input) throws BadRequestException;

    ResultId buildClustersAndCompute(String url, String organization, boolean compare, double threshold, int maxNumber, MultipartFile input) throws BadRequestException;

    String simReqClusters(String organization, int maxValue, List<String> input) throws ComponentException;

    void treatDependencies(String organization, DependenciesModel dependencies) throws ComponentException;

    ResultId batchProcess(String url, String organization, ProjectWithDependencies input) throws ComponentException;


    /*
    Auxiliary operations
     */

    String getResponsePage(String organization, String responseId) throws ComponentException;

    String getOrganizationInfo(String organization) throws ComponentException;

    void deleteOrganizationResponses(String organization) throws ComponentException;

    void clearOrganization(String organization) throws ComponentException;

    void clearDatabase() throws ComponentException;

}