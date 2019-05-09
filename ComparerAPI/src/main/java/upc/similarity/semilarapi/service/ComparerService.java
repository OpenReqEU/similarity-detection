package upc.similarity.semilarapi.service;


import java.util.List;

import upc.similarity.semilarapi.entity.Dependency;
import upc.similarity.semilarapi.entity.Requirement;
import upc.similarity.semilarapi.entity.input.ReqProject;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.InternalErrorException;
import upc.similarity.semilarapi.exception.NotFinishedException;
import upc.similarity.semilarapi.exception.NotFoundException;

public interface ComparerService {

    public void buildModel(String responseId, String compare, String organization, List<Requirement> reqs) throws BadRequestException, InternalErrorException;

    public Dependency simReqReq(String organization, String req1, String req2) throws NotFoundException, InternalErrorException;

    public void simReqProject(String responseId, String organization, double threshold, ReqProject project_reqs) throws NotFoundException, InternalErrorException, BadRequestException;

    public void simProject(String responseId, String organization, double threshold, List<String> project_reqs, boolean responseCreated) throws NotFoundException, InternalErrorException;

    public void buildModelAndCompute(String responseId, String compare, String organization, double threshold, List<Requirement> reqs) throws NotFoundException, BadRequestException, InternalErrorException;

    public String getResponsePage(String organization, String responseId) throws NotFoundException, InternalErrorException, NotFinishedException;

    public void clearOrganizationResponses(String organization) throws NotFoundException, InternalErrorException;

}