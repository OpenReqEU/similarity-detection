package upc.similarity.similaritydetectionapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import upc.similarity.similaritydetectionapi.RestApiController;

import java.util.HashSet;

@Configuration
@PropertySource("classpath:swagger.properties")
@ComponentScan(basePackageClasses = RestApiController.class)
@EnableSwagger2
public class SwaggerConfig {

    private static final String	TITLE		    = "SIMILARITY DETECTION COMPONENT";
    private static final String	DESCRIPTION    = "" +
            "<p>The component is based on tf-idf numerical statistics. The aim of the API is to calculate the similarity score between multiple pairs of requirements."+
            "</p>" +
            "<p>There are three types of operations (each method has a more extensive description in its own operation box): </p>" +
            "<ul>" +
            "<li><strong>Similarity without clusters</strong>: These methods work only with a set of input requirements. There are two types of operations. The ones that generate and update a tf-idf model with the input requirements, and the ones that use these tf-idf models to compute the similarity between the requirements; therefore, before using the operations, is necessary to create a tf-idf model first for the organization. Each tf-idf model is assigned to an organization and can be used infinite times. </li>" +
            "<ul>" +
            "<li>BuildModel: Pre-processes the input requirements, generates a tf-idf model and assings it to an specified organization.</li>" +
            "<li>BuildModelAndCompute: Pre-processes the input requirements, generates a tf-idf model with the input requirements and computes and returns the similarity score between all the possible pairs of requirements.</li>" +
            "<li>AddRequirements: Pre-processes the input requirements and adds them to an existing tf-idf model.</li>" +
            "<li>DeleteRequirements: Deletes the input requirements from an existing tf-idf model.</li>" +
            "<li>ReqReq: Compares two requirements.</li>" +
            "<li>ReqProject: Compares all the requirements in the input list with a set of requirements given in the input.</li>" +
            "<li>Project: Compares all possible pairs of requirements of an organization.</li>" +
            "<li>ReqOrganization: Compares a set of requirements with all the requirements of an organization.</li>" +
            "<li>NewReqOrganization: Pre-processes the input requirements and adds them to an organization's tf-idf model. It also compares the input requirements with all the requirements of the organization.</li>" +
            "</ul>"+
            "<li><strong>Similarity with clusters</strong>: These methods work with a set of input requirements and dependencies. <u>The clusters are considered as graphs connected by similarity dependencies accepted by the user where the nodes are the requirements of the model</u>. We denominate orphans to the clusters with only one requirement. </li>" +
            "<ul>" +
            "<li>BuildClusters: Pre-processes the input requirements, generates a tf-idf model with the requirements information, the clusters architecture and the input similarity dependencies and assigns it to an specified organization. All the requirements are compared with all the requirements of other clusters of the organization, and the maximum score with each cluster for each requirement is stored in the database.</li>" +
            "<li>BuildClustersAndCompute: Pre-processes the input requirements, generates a tf-idf model with the requirements information, the clusters architecture and the input similarity dependencies and assings it to an specified organization. All the requirements are compared with all the requirements of other clusters of the organization, and the maximum score with each cluster for each requirement is stored in the database. The method returns the maximum similarity score between each requirement and all the requirements that make up each one of the existing clusters of the organization.</li>" +
            "<li>ReqClusters: Given a list of requirements ids, returns the maximum similarity score between each requirement and all the requirements that make up each of the existing clusters in the organization model.</li>" +
            "<li>BatchProcess: Given a set of updates done in the requirements, updates the clusters and dependencies accordingly.</li>"+
            "<li>TreatAcceptedAndRejectedDependencies: Given a set of accepted and rejected dependencies, updates the clusters and dependencies accordingly.</li>"+
            "</ul>"+
            "<li><strong>Auxiliary methods</strong>:</li>" +
            "<ul>" +
            "<li>GetResponse: Returns the output of the async methods</li>" +
            "<li>GetOrganizationInfo: Returns the main information of the specified organization including the current computations and the pending responses</li>" +
            "<li>DeleteOrganizationResponses: Deletes the organization responses from the database</li>" +
            "<li>ClearDatabase: Deletes all data from the database</li>" +
            "</ul>"+
            "</ul>" +
            "<p>All operations except <i>ReqReq</i>, <i>ReqClusters</i>, <i>TreatAcceptedAndRejectedDependencies</i> and the auxiliary methods are asynchronous. All these operations follow the same pattern:</p>" +
            "<ol><li>The client calls the operation with all necessary parameters</li>" +
            "<li>The service receives the request and checks the main conditions</li>" +
            "<li>The service returns if the client request has been accepted or not and closes the connection" +
            "<ul><li>(httpStatus!=200) The request has not been accepted. The message body contains the exception cause.</li>" +
            "<li>(httpStatus==200) The request has been accepted. The similarity calculation runs in the background. The message body contains the request identifier, i.e., <em>{\"id\": \"1548924677975_523\"}</em></li></ul>" +
            "<li>When the calculation finishes (only if the request has been accepted) the service opens a connection with the server url specified as parameter (optional). It sends a JSON object that contains the outcome of the computation:<br>" +
            "<ul>" +
            "<li>(success) Example: {\"code\": 200,\"id\": \"1557395889689_587\",\"operation\": \"AddReqs\"}.</li>" +
            "<li>(!success) Example: {\"code\": 400,\"id\": \"1557396039530_583\",\"error\": \"Bad request\",\"message\": \"The requirement with id QM-3 is already inside the project\",\"operation\": \"ReqProject\"}.</li>" +
            "</ul><li>The result of the operation can be obtained through the GetResponse method.</li></li></ol>" +
            "<p>The service uses locks to control the concurrency of the different operations in each organization. To guarantee the ACID properties the methods which write or update data can't be parallelized. For example, " +
            "the BuildClusters and BatchProcess methods cannot be parallelized, one has to wait for the other before starting. The service has a small queue of about 5 seconds to solve the small deviations. However, if one method " +
            "has to wait for more than 5 seconds it will return an error with code 423. This only happens with methods using the same organization. The methods like ReqClusters that only read data from the database are not affected.</p>" +
            "<p>The API uses UTF-8 charset. Also, it uses the OpenReq format for input JSONs (it is specified in the Models section).</p>";

    @Bean
    public Docket api() {
        HashSet<String> protocols = new HashSet<>();
        protocols.add("https");
        return new Docket(DocumentationType.SWAGGER_2)
                .useDefaultResponseMessages(false)
                .apiInfo(apiInfo())
                .pathMapping("/")
                .host("api.openreq.eu/similarity-detection")
                .protocols(protocols)
                .select()
                .paths(PathSelectors.regex("^((?!Test).)*$"))
                .apis(RequestHandlerSelectors.basePackage("upc.similarity.similaritydetectionapi")).paths(PathSelectors.regex("/upc.*"))
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title(TITLE).description(DESCRIPTION).version("2.0")
                .license("License").licenseUrl("https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.txt")
                .contact(new Contact("UPC-GESSI (OPENReq)", "http://openreq.eu/", ""))
                .build();
    }
}