# Similarity Detection

_This service was created as a result of the OpenReq project funded by the European Union Horizon 2020 Research and Innovation programme under grant agreement No 732463._

## Introduction

The component is based in td-idf numerical statistic. The aim of the API is to calculate the similarity score between multiple pairs of requirements.

## Technical description

Next sections provide a general overview of the technical details of the similarity detection service.

### Main functionalities

There are four different types of operations:

    - AddReqs: Generates a model with the input requirements
	- ReqReq: Compares two requirements
	- ReqProject: Compares between a list of requirements and a set of requirements
	- Project: Compares all possible pairs of requirements from a set of requirements
	- AddReqsAndCompute: Is a mixture between AddReqs and Project methods

The component needs to preprocess the requirements before doing any comparison. The operation AddReqs is responsible for that work.

The API uses UTF-8 charset. Also, it uses the OpenReq format for input and output JSONs.


### Used technologies

The service is divided into two independent components: SimilarityDetectionAPI and SemilarAPI. The first one is the main component in charge of receiving the requests and returning the final results whereas Semilar computes the similarity dependencies between the input requirements. The TestingRestService is just a client example to test the service.

    - SimilarityDetectionAPI: main component
    - SemilarAPI: auxiliary component
    - TestingRestSercice: example of client
    - testing: directory with input and output examples used for testing
    - models.db: requirements database for semilar library
    - LICENSE: license

### Asynchronous service

All the operations in this service are asynchronous. It is necessary to write a server URL as parameter in all of them. The result of the operation will be returned to that url. All operations follow the same pattern:

	1.The client calls the operation with all necessary parameters
	2.The service receives the request and checks the main conditions
	3.The service returns if the client request has been accepted or not and closes the connection
		3.1.(httpStatus!=200) The request has not been accepted. The message body contains the exception cause.
		3.2.(httpStatus==200) The request has been accepted. The similarity calculation runs in the background. The message body contains the request identifier i.e. {"id": "1548924677975_523"}
	4.When the calculation finishes (only if the request has been accepted) the service opens a connection with the server url specified as parameter.
	It sends a multipart file with a JSON object (named "info") and an InputStream (named "result"). The JSON object contains the information about the request and the InputStream contains the resulting body.
	Example of JSON object: {"success":"true","id":"1548924677975_523","operation":"Proj"} . Shows if the request has been successful, the request identifier and the name of the request operation.
		4.1.(success==false) The InputStream contains the exception cause.
		4.2.(success==true) The InputStream contains the result of the operation.

### How to install

Steps to configure the service:

1. Download and install 8 JDK and last Maven version.

Steps to run the service:

1. Open a terminal and copy-paste "cd jars"

2. Open a terminal and copy-paste "java -jar jars/similaritydetectionapi.jar". Wait for an exit like this: u.s.s.SimilaritydetectionapiApplication  : Started SimilaritydetectionapiApplication

3. Open a terminal and copy-paste "java -jar jars/semilarapi.jar". Wait for an exit like this: u.s.semilarapi.SemilarapiApplication  : Started SemilarapiApplication

4. (only if you wanna use the auxiliary client) Open a terminal and copy-paste "java -jar jars/testrestservice.jar". Wait for an exit like this: u.s.restserviceapi.RestServiceApplication  : Started RestServiceApplication

5. Go to http://localhost:9404/swagger-ui.html#/ to see the swagger generated. You can use the component through the swagger or through http connections to the endpoints indicated in the swagger documentation.


### How to use it

The service expects a JSON with OpenReqJson format. At this moment the service provides 3 different functionalities with 1 auxiliary operation.

Check API details [here](https://api.openreq.eu/#/services/similarity-detection).

### Notes for developers

### Sources

## How to contribute

See OpenReq project contribution [guidelines](https://github.com/OpenReqEU/OpenReq/blob/master/CONTRIBUTING.md)

## License

Free use of this software is granted under the terms of the [EPL version 2 (EPL2.0)](https://www.eclipse.org/legal/epl-2.0/)
