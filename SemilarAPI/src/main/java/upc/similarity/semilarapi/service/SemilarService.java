package upc.similarity.semilarapi.service;

import java.sql.SQLException;
import java.util.List;

import org.json.JSONObject;
import upc.similarity.semilarapi.entity.input.PairReq;
import upc.similarity.semilarapi.entity.input.ProjOp;
import upc.similarity.semilarapi.entity.input.ReqProjOp;
import upc.similarity.semilarapi.entity.output.Dependencies;
import upc.similarity.semilarapi.entity.Requirement;
import upc.similarity.semilarapi.exception.BadRequestException;
import upc.similarity.semilarapi.exception.InternalErrorException;

public interface SemilarService {

    //Similarity
    public void similarity(String compare, String filename, PairReq input) throws SQLException, BadRequestException, InternalErrorException;

    public JSONObject testing(String compare, String filename, PairReq input) throws SQLException, BadRequestException, InternalErrorException;

    public void similarityReqProj(String compare, float threshold, String filename, ReqProjOp input) throws InternalErrorException;

    public void similarityProj(String compare, float threshold, String filename, ProjOp input) throws InternalErrorException;

    public void similarityProj_Large(String compare, float threshold, String filename, ProjOp input) throws InternalErrorException;

    public void similarityCluster(String compare, float threshold, String filename, String type, ProjOp input) throws InternalErrorException, BadRequestException;


    //Database
    public void savePreprocessed(List<Requirement> reqs) throws SQLException;

    public void clearDB() throws SQLException;
}