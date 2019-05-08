package upc.similarity.semilarapi.service;


import java.util.List;

import upc.similarity.semilarapi.entity.Dependency;
import upc.similarity.semilarapi.entity.Requirement;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.InternalErrorException;
import upc.similarity.semilarapi.exception.NotFinishedException;

public interface ComparerService {

    public void buildModel(String compare, String organization, List<Requirement> reqs) throws BadRequestException, InternalErrorException;

    public Dependency simReqReq(String organization, String req1, String req2) throws BadRequestException, InternalErrorException;

    public void simReqProject(String responseId, String organization, String req, double threshold, List<String> project_reqs) throws BadRequestException, InternalErrorException;

    public void simProject(String responseId, String organization, double threshold, List<String> project_reqs) throws BadRequestException, InternalErrorException;

    public void buildModelAndCompute(String responseId, String compare, String organization, double threshold, List<Requirement> reqs) throws BadRequestException, InternalErrorException;

    public String getResponsePage(String organization, String responseId) throws BadRequestException, InternalErrorException, NotFinishedException;

    public void clearDB(String organization) throws InternalErrorException;
}