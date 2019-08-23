package upc.similarity.compareapi.service;


import java.util.List;

import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.input.Clusters;
import upc.similarity.compareapi.entity.input.ReqProject;
import upc.similarity.compareapi.entity.output.Dependencies;
import upc.similarity.compareapi.exception.BadRequestException;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.exception.NotFoundException;

public interface CompareService {


    /*
    Similarity without clusters
     */

    void buildModel(String responseId, boolean compare, double threshold, String organization, List<Requirement> requirements) throws BadRequestException, NotFinishedException, InternalErrorException;

    void buildModelAndCompute(String responseId, boolean compare, String organization, double threshold, List<Requirement> requirements) throws NotFinishedException, BadRequestException, InternalErrorException;

    void addRequirements(String responseId, String organization, List<Requirement> requirements) throws InternalErrorException, BadRequestException, NotFoundException, NotFinishedException;

    void deleteRequirements(String responseId, String organization, List<Requirement> requirements) throws InternalErrorException, BadRequestException, NotFoundException, NotFinishedException;

    Dependency simReqReq(String organization, String req1, String req2) throws NotFoundException, InternalErrorException;

    void simReqOrganization(String responseId, String organization, List<Requirement> requirements) throws NotFoundException, NotFinishedException, InternalErrorException, BadRequestException;

    void simReqProject(String responseId, String organization, ReqProject projectRequirements) throws NotFoundException, InternalErrorException, BadRequestException;

    void simProject(String responseId, String organization, List<String> projectRequirements) throws NotFoundException, InternalErrorException;


    /*
    Similarity with clusters
     */

    void buildClusters(String responseId, boolean compare, double threshold, String organization, Clusters requirements) throws BadRequestException, NotFinishedException, InternalErrorException;

    void buildClustersAndCompute(String responseId, boolean compare, String organization, double threshold, int maxNumber, Clusters requirements) throws BadRequestException, NotFinishedException, InternalErrorException;

    Dependencies simReqClusters(String organization, List<String> requirements, int maxNumber) throws NotFoundException, InternalErrorException;

    void treatAcceptedAndRejectedDependencies(String organization, List<Dependency> dependencies) throws NotFoundException, BadRequestException, NotFinishedException, InternalErrorException;

    void batchProcess(String responseId, String organization, Clusters input) throws BadRequestException, NotFoundException, NotFinishedException, InternalErrorException;


    /*
    Auxiliary methods
     */

    String getResponsePage(String organization, String responseId) throws NotFoundException, InternalErrorException, NotFinishedException;

    void clearOrganizationResponses(String organization) throws NotFoundException, InternalErrorException;

    void clearOrganization(String organization) throws NotFoundException, NotFinishedException, InternalErrorException;

    void clearDatabase() throws InternalErrorException;


    /*
    Test methods
     */

    void TestAccuracy(boolean compare, Clusters input);

    String extractModel(boolean compare, String organization, Clusters input);

}