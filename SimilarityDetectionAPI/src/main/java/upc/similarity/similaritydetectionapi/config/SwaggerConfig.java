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

@Configuration
@PropertySource("classpath:swagger.properties")
@ComponentScan(basePackageClasses = RestApiController.class)
@EnableSwagger2
public class SwaggerConfig {

    private static final String	LICENSE_TEXT	    = "License";
    private static final String	title		    = "SIMILARITY DETECTION COMPONENT";
    private static final String	description	    = "" +
            "<p>The component is based in td-idf numerical statistic. The aim of the API is to calculate the similarity score between multiple pairs of requirements."+
            "</p>" +
            "<p>There are three different types of operations: </p>" +
            "<ul>" +
            "<li>ReqReq: Compares two requirements</li>" +
            "<li>ReqProj: Compares between a requirement and a set of requirements</li>" +
            "<li>Proj: Compares all possible pairs of requirements from a set of requirements</li></ul>" +
            "<p>The component needs to preprocess the requirements before doing any comparison. The operation BuildModel is responsible for that work.</p>" +
            "<p>All the operations in this service are asynchronous. It is necessary to write a server URL as parameter in all of them. The result of the operation will be returned to that url. All operations follow the same pattern:</p>" +
            "<ol><li>The client calls the operation with all necessary parameters</li>" +
            "<li>The service receives the request and checks the main conditions</li>" +
            "<li>The service returns if the client request has been accepted or not and closes the connection" +
            "<ul><li>(httpStatus!=200) The request has not been accepted. The message body contains the exception cause.</li>" +
            "<li>(httpStatus==200) The request has been accepted. The similarity calculation runs in the background. The message body contains the request identifier i.e. <em>{\"id\": \"1548924677975_523\"}</em></li></ul>" +
            "<li>When the calculation finishes (only if the request has been accepted) the service opens a connection with the server url specified as parameter.<br>" +
            "It sends a multipart file with a JSON object and an InputStream. The JSON object contains the information about the request and the InputStream contains the resulting body.<br>" +
            "Example of JSON object: <em> {\"success\":\"true\",\"id\":\"1548924677975_523\",\"operation\":\"Proj\"} </em>. Shows if the request has been successful, the request identifier and the name of the request operation.<ul>" +
            "<li>(success==false) The InputStream contains the exception cause.</li>" +
            "<li>(success==true) The InputStream contains the result of the operation.</li></ul></li></ol>" +
            "<p>The API uses UTF-8 charset. Also, it uses the OpenReq format for input and output JSONs (it is specified in the Models section).</p>";

    /**
     * API Documentation Generation.
     * @return
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .useDefaultResponseMessages(false)
                .apiInfo(apiInfo())
                .pathMapping("/")
                .select()
                .paths(PathSelectors.regex("/upc/similarity-detection/AddReqs|/upc/similarity-detection/Project|/upc/similarity-detection/ReqProject|/upc/similarity-detection/ReqReq"))
                .apis(RequestHandlerSelectors.basePackage("upc.similarity.similaritydetectionapi")).paths(PathSelectors.regex("/upc.*"))
                .build().tags(new Tag("Similarity detection Service", "API related to similarity detection"));
    }
    /**
     * Informtion that appear in the API Documentation Head.
     *
     * @return
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title(title).description(description).license(LICENSE_TEXT)
                .contact(new Contact("UPC-GESSI (OPENReq)", "http://openreq.eu/", ""))
                .build();
    }
}