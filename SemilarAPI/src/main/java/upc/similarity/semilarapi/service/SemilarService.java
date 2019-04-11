package upc.similarity.semilarapi.service;

import java.sql.SQLException;
import java.util.List;

import upc.similarity.semilarapi.entity.Dependency;
import upc.similarity.semilarapi.entity.Requirement;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.InternalErrorException;

public interface SemilarService {

    public void buildModel(String compare, String organization, List<Requirement> reqs) throws BadRequestException, InternalErrorException;

    public List<Dependency> simReqReq(String organization, String req1, String req2) throws BadRequestException, InternalErrorException;

    public List<Dependency> simReqProject(String organization, String req, List<String> project_reqs) throws BadRequestException, InternalErrorException;

    public List<Dependency> simProject(String organization, List<String> project_reqs) throws BadRequestException, InternalErrorException;

    public void clearDB() throws InternalErrorException;
}