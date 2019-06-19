package upc.similarity.compareapi.service;


import java.util.List;

import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.input.Clusters;
import upc.similarity.compareapi.entity.input.ReqProject;
import upc.similarity.compareapi.exception.BadRequestException;
import upc.similarity.compareapi.exception.InternalErrorException;
import upc.similarity.compareapi.exception.NotFinishedException;
import upc.similarity.compareapi.exception.NotFoundException;

public interface CompareService {

    public void buildModel(String responseId, String compare, String organization, List<Requirement> requirements) throws BadRequestException, InternalErrorException;

    public Dependency simReqReq(String organization, String req1, String req2) throws NotFoundException, InternalErrorException;

    public void simReqOrganization(String responseId, String compare, String organization, double threshold, List<Requirement> requirements) throws NotFoundException, InternalErrorException, BadRequestException;

    public void simReqProject(String responseId, String organization, double threshold, ReqProject projectRequirements) throws NotFoundException, InternalErrorException, BadRequestException;

    public void simProject(String responseId, String organization, double threshold, List<String> projectRequirements) throws NotFoundException, InternalErrorException;

    public void buildModelAndCompute(String responseId, String compare, String organization, double threshold, List<Requirement> requirements) throws NotFoundException, BadRequestException, InternalErrorException;

    public String getResponsePage(String organization, String responseId) throws NotFoundException, InternalErrorException, NotFinishedException;

    public void clearOrganizationResponses(String organization) throws NotFoundException, InternalErrorException;

    public void clearDatabase() throws InternalErrorException;

    public void buildClustersAndComputeOrphans(String responseId, String compare, String organization, double threshold, Clusters requirements) throws BadRequestException, InternalErrorException;

    public void buildClusters(String responseId, String compare, String organization, Clusters requirements) throws BadRequestException, InternalErrorException;

}