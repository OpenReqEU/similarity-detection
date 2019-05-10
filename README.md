# Similarity Detection

_This service was created as a result of the OpenReq project funded by the European Union Horizon 2020 Research and Innovation programme under grant agreement No 732463._

## Introduction

The component is based in td-idf numerical statistic. The aim of the API is to calculate the similarity score between multiple pairs of requirements.

## Technical description

Next sections provide a general overview of the technical details of the similarity detection service.

### Main functionalities

There are five main methods:

    - AddReqs: Generates a model with the input requirements
    - ReqReq: Compares two requirements
    - ReqProject: Compares between a list of requirements and a set of requirements
    - Project: Compares all possible pairs of requirements from a set of requirements
    - AddReqsAndCompute: Is a mixture between AddReqs and Project methods

And three auxiliary operations:

    - GetResponse: Returns in patches the resulting dependencies of the other methods
    - DeleteOrganizationResponses: Deletes the organization responses from the database
    - DeleteDatabase: Deletes all data from the database

The component needs to preprocess the requirements before doing any comparison. The operation AddReqs is responsible for that work.

The API uses UTF-8 charset. Also, it uses the OpenReq format for input JSONs.


### Used technologies

The service is divided into two independent components: SimilarityDetectionAPI and ComparerAPI. The first one is the main component in charge of receiving the requests and returning the final results whereas Comparer computes the similarity dependencies between the input requirements. The TestingRestService is just a client example to test the service.

    - SimilarityDetectionAPI: main component
    - ComparerAPI: auxiliary component
    - TestingRestSercice: example of client
    - testing: directory with input and output examples used for testing
    - models.db: requirements database
    - LICENSE: license

### Asynchronous service

The operations AddReqs, ReqProject, Project and AddReqsAndCompute are asynchronous. It is necessary to write a server URL as parameter in all of them. The outcome of the operation will be returned to that url. All these operations follow the same pattern:

    1.The client calls the operation with all necessary parameters
    2.The service receives the request and checks the main conditions
    3.The service returns if the client request has been accepted or not and closes the connection
        3.1.(httpStatus!=200) The request has not been accepted. The message body contains the exception cause.
        3.2.(httpStatus==200) The request has been accepted. The similarity calculation runs in the background. The message body contains the request identifier i.e. {"id": "1548924677975_523"}
    4. When the calculation finishes (only if the request has been accepted) the service opens a connection with the server url specified as parameter. It sends a Json object that contains the outcome of the computation:
        4.1.(success) Example: {"code": 200,"id": "1557395889689_587","operation": "AddReqs"}.
        4.2.(!success) Example: {"code": 400,"id": "1557396039530_583","error": "Bad request","message": "The requirement with id QM-3 is already inside the project","operation": "ReqProject"}.
        4.3.The resulting dependencies can be obtained via the GetResponse method.

### How to install

Steps to configure the service:

1. Download and install 8 JDK and last Maven version.

Steps to run the service:

    1.Open a terminal and copy-paste "sh run_similarity_detection.sh". Wait for an exit like this: u.s.s.SimilaritydetectionapiApplication : Started SimilaritydetectionapiApplication

    2.Open a terminal and copy-paste "sh run_comparer.sh". Wait for an exit like this: u.s.comparerapi.ComparerApplication : Started ComparerapiApplication

    3.(only if you wanna use the auxiliary client) Open a terminal and copy-paste "sh run_restservice.sh". Wait for an exit like this: u.s.restserviceapi.RestServiceApplication : Started RestServiceApplication

    4.Go to http://localhost:9404/swagger-ui.html#/ to see the swagger generated. You can use the component through the swagger or through http connections to the endpoints indicated in the swagger documentation.


### How to use it

The service expects a JSON with OpenReqJson format.

Check API details [here](https://api.openreq.eu/#/services/similarity-detection).

### Notes for developers

### Sources

## How to contribute

See OpenReq project contribution [guidelines](https://github.com/OpenReqEU/OpenReq/blob/master/CONTRIBUTING.md)

## License

Free use of this software is granted under the terms of the [EPL version 2 (EPL2.0)](https://www.eclipse.org/legal/epl-2.0/)
