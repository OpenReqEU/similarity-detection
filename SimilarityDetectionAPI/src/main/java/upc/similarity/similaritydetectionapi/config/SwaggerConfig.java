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
import springfox.documentation.service.Tag;
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

    private static final String	LICENSE_TEXT	    = "License";
    private static final String	TITLE		    = "SIMILARITY DETECTION COMPONENT";
    private static final String	DESCRIPTION    = "" +
            "<p>The component is based in tf-idf numerical statistic. The aim of the API is to calculate the similarity score between multiple pairs of requirements."+
            "</p>" +
            "<p>There are four types of operations (each method has a more extensive description in its own operation box): </p>" +
            "<ul>" +
            "<li><strong>Model</strong>: These methods are responsible for pre-processing the input requirements, generating a model that saves the requirements information and assigning it to an organization. Generating these models is mandatory before making any comparison.</li>" +
            "<ul>" +
            "<li>BuildModel: Pre-processes the input requirements, generates a model and assings it to an specified organization.</li>" +
            "<li>AddRequirements: Pre-processes the input requirements and adds them to an existing model.</li>" +
            "<li>DeleteRequirements: Deletes the input requirements from an existing model.</li>" +
            "</ul>"+
            "<li><strong>Compare</strong>: These methods are in charge of comparing and returning the corresponding similarity dependencies between the specified requirements of an organization’s model.</li>" +
            "<ul>" +
            "<li>ReqReq: Compares two requirements.</li>" +
            "<li>ReqProject: Compares between a list of requirements and a set of requirements.</li>" +
            "<li>ReqOrganization: Pre-processes the input requirements and adds them to an organization's model. Also it compares the input requirements with all the requirements of the organization's model.</li>" +
            "<li>Project: Compares all possible pairs of requirements from a set of requirements.</li>" +
            "<li>AddReqsAndCompute: Generates a model with the input requirements and computes the similarity score between all the possible pairs of requirements.</li>" +
            "</ul>"+
            "<li><strong>Clusters</strong>: These methods are responsible for pre-processing the input requirements and dependencies, generating a model that saves the requirements information and the clusters architecture and assigning it to an organization. <u>The clusters are considered as graphs connected by similarity dependencies accepted by the user where the nodes are the requirements of the model</u>. We denominate orphans to the clusters with only one requirement. </li>" +
            "<ul>" +
            "<li>AddClusters: Pre-processes the input requirements, generates a model with the requirements information, the clusters architecture and the input similarity dependencies and assings it to an specified organization.</li>" +
            "<li>AddClustersAndCompute: Pre-processes the input requirements, generates a model with the requirements information, the clusters architecture and the input similarity dependencies and assings it to an specified organization. All the resulting orphans are compared with all the requirements of each cluster and the maximum score is returned for each one as long as it is above the specified threshold.</li>" +
            "<li>ReqClusters: Given a list of requirements ids, returns the maximum similarity score between each requirement and all the requirements that make up each of the existing clusters in the organization model.</li>" +
            "<li>BatchProcess: Given a set of updates done in the requirements, updates the clusters and dependencies accordingly.</li>"+
            "<li>TreatAcceptedAndRejectedDependencies: Given a set of accepted and rejected dependencies, updates the clusters and dependencies accordingly.</li>"+
            "</ul>"+
            "<li><strong>Auxiliary methods</strong>:</li>" +
            "<ul>" +
            "<li>GetResponse: Returns in patches the resulting dependencies of the other methods</li>" +
            "<li>DeleteOrganizationResponses: Deletes the organization responses from the database</li>" +
            "<li>DeleteDatabase: Deletes all data from the database</li>" +
            "</ul>"+
            "</ul>" +
            "<p>All operations except ReqClusters, TreatAcceptedAndRejectedDependencies, GetResponse, DeleteOrganizationResponses and DeleteDatabase are asynchronous. It is necessary to write a server URL as parameter in all of them. The outcome of the operation will be returned to that url. All these operations follow the same pattern:</p>" +
            "<ol><li>The client calls the operation with all necessary parameters</li>" +
            "<li>The service receives the request and checks the main conditions</li>" +
            "<li>The service returns if the client request has been accepted or not and closes the connection" +
            "<ul><li>(httpStatus!=200) The request has not been accepted. The message body contains the exception cause.</li>" +
            "<li>(httpStatus==200) The request has been accepted. The similarity calculation runs in the background. The message body contains the request identifier i.e. <em>{\"id\": \"1548924677975_523\"}</em></li></ul>" +
            "<li>When the calculation finishes (only if the request has been accepted) the service opens a connection with the server url specified as parameter. It sends a Json object that contains the outcome of the computation:<br>" +
            "<ul>" +
            "<li>(success) Example: {\"code\": 200,\"id\": \"1557395889689_587\",\"operation\": \"AddReqs\"}.</li>" +
            "<li>(!success) Example: {\"code\": 400,\"id\": \"1557396039530_583\",\"error\": \"Bad request\",\"message\": \"The requirement with id QM-3 is already inside the project\",\"operation\": \"ReqProject\"}.</li>" +
            "<li>The resulting dependencies can be obtained via the GetResponse method.</li></ul></li></ol>" +
            "<p>The API uses UTF-8 charset. Also, it uses the OpenReq format for input JSONs (it is specified in the Models section).</p>";

    /**
     * API Documentation Generation.
     * @return
     */
    @Bean
    public Docket api() {
        HashSet<String> protocols = new HashSet<>();
        protocols.add("https");
        return new Docket(DocumentationType.SWAGGER_2)
                .useDefaultResponseMessages(false)
                .apiInfo(apiInfo())
                .pathMapping("/")
                //.host("api.openreq.eu/similarity-detection")
                //.protocols(protocols)
                .select()
                .paths(PathSelectors.regex("^((?!Test).)*$"))
                .apis(RequestHandlerSelectors.basePackage("upc.similarity.similaritydetectionapi")).paths(PathSelectors.regex("/upc.*"))
                .build().tags(new Tag("Similarity detection Service", "API related to similarity detection"));
    }
    /**
     * Informtion that appear in the API Documentation Head.
     *
     * @return
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title(TITLE).description(DESCRIPTION).license(LICENSE_TEXT)
                .contact(new Contact("UPC-GESSI (OPENReq)", "http://openreq.eu/", ""))
                .build();
    }
}