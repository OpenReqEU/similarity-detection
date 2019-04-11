package upc.similarity.semilarapi.service;

import java.sql.SQLException;
import java.util.List;

import upc.similarity.semilarapi.entity.Dependency;
import upc.similarity.semilarapi.entity.Requirement;

public interface SemilarService {

    public void buildModel(String compare, String organization, List<Requirement> reqs) throws SQLException, Exception;

    public List<Dependency> simReqReq(String organization, String req1, String req2) throws Exception;

    public List<Dependency> simReqProject(String organization, String req, List<String> project_reqs) throws Exception;

    public void clearDB() throws SQLException;
}