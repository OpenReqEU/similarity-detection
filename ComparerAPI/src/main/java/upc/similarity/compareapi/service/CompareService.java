package upc.similarity.compareapi.service;


import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import upc.similarity.compareapi.entity.Dependency;
import upc.similarity.compareapi.entity.Organization;
import upc.similarity.compareapi.entity.Requirement;
import upc.similarity.compareapi.entity.input.Clusters;
import upc.similarity.compareapi.entity.input.ProjectProject;
import upc.similarity.compareapi.entity.input.ReqProject;
import upc.similarity.compareapi.entity.output.Dependencies;
import upc.similarity.compareapi.entity.exception.*;

public interface CompareService {


    /*
    Similarity without clusters
     */

    void buildModel(String responseId, boolean compare, String organization, List<Requirement> requirements) throws ComponentException;

    void buildModelAndCompute(String responseId, boolean compare, String organization, double threshold, List<Requirement> requirements, int maxNumDeps) throws ComponentException;

    void addRequirements(String responseId, String organization, List<Requirement> requirements) throws ComponentException;

    void deleteRequirements(String responseId, String organization, List<Requirement> requirements) throws ComponentException;

    Dependency simReqReq(String organization, String req1, String req2) throws ComponentException;

    void simReqOrganization(String responseId, String organization, double threshold, List<String> requirements, int maxNumDeps) throws ComponentException;

    void simNewReqOrganization(String responseId, String organization, double threshold, List<Requirement> requirements, int maxNumDeps) throws ComponentException;

    void simReqProject(String responseId, String organization, double threshold, ReqProject projectRequirements, int maxNumDeps) throws ComponentException;

    void simProject(String responseId, String organization, double threshold, List<String> projectRequirements, int maxNumDeps) throws ComponentException;

    void simProjectProject(String responseId, String organization, double threshold, ProjectProject projects, int maxNumDeps) throws ComponentException;


    /*
    Similarity with clusters
     */

    void buildClusters(String responseId, boolean compare, double threshold, String organization, MultipartFile file) throws ComponentException;

    void buildClustersAndCompute(String responseId, boolean compare, String organization, double threshold, int maxNumber, MultipartFile file) throws ComponentException;

    Dependencies simReqClusters(String organization, List<String> requirements, int maxNumber) throws ComponentException;

    void treatAcceptedAndRejectedDependencies(String organization, List<Dependency> dependencies) throws ComponentException;

    void batchProcess(String responseId, String organization, Clusters input) throws ComponentException;


    /*
    Auxiliary methods
     */

    String getResponsePage(String organization, String responseId) throws ComponentException;

    Organization getOrganizationInfo(String organization) throws ComponentException;

    void deleteOrganizationResponses(String organization) throws ComponentException;

    void deleteOrganization(String organization) throws ComponentException;

    void clearDatabase() throws ComponentException;

}