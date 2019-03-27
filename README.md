# Similarity Detection

_This service was created as a result of the OpenReq project funded by the European Union Horizon 2020 Research and Innovation programme under grant agreement No 732463._

## Introduction

The component is based in Semilar semantic similarity library. The aim of the API is to calculate the similarity score between multiple pairs of requirements.

Semilar reference:
Rus, V., Lintean, M., Banjade, R., Niraula, N., and Stefanescu, D. (2013). SEMILAR: The Semantic Similarity Toolkit. Proceedings of the 51st Annual Meeting of the Association for Computational Linguistics, August 4-9, 2013, Sofia, Bulgaria.

## Technical description

Next sections provide a general overview of the technical details of the similarity detection service.

### Main functionalities

There are three different types of operations:

	- ReqReq: Compares two requirements
	- ReqProj: Compares between a list of requirements and a set of requirements
	- Proj: Compares all possible pairs of requirements from a set of requirements

Also there are two ways to compare requirements. The default method consists in comparing the name of the requirements. The other way lies in comparing the name and the text of the requirements and return the maximum of the two.

The component needs to preprocess the requirements before doing any comparison. The operation AddReqs is responsible for that work. We use opennlp library in preprocessing requirements.

The API uses UTF-8 charset. Also, it uses the OpenReq format for input and output JSONs.

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

### Used technologies

The service is divided into three independent components: SimilarityDetectionAPI, SemilarAPI and TestingRestService. The first one is the main component in charge of receiving the requests and returning the final results. Semilar creates the similarity dependencies between the input requirements. The TestingRestService is just a client example to test the service.

    - SimilarityDetectionAPI: main component
    - SemilarAPI: auxiliary component with semilar library
    - TestingRestSercice: example of client
    - testing: directory with input and output examples used for testing
    - semilar.db: requirements database for semilar library
    - LICENSE: license



### How to install

Steps to configure the service:

1. Download the next opennlp models from http://opennlp.sourceforge.net/models-1.5/ and save them in $ServiceDirectory/SemilarAPI/opennlp-tools-1.5.0/Models-1.5/ (create the directories first):

	- en-chunker.bin
	- en-parser-chunking.bin
	- en-pos-maxent.bin
	- en-pos-perceptron.bin
	- en-sent.bin
	- en-token.bin

2. Download the Semilar.jar from http://deeptutor2.memphis.edu/Semilar-Web/public/semilar-api.html and save it in $ServiceDirectory/SemilarApi/Semilar-1.0.jar

3. Download the JWI (MIT Java Wordnet Interface) from https://projects.csail.mit.edu/jwi/ and save it in $ServiceDirectory/SemilarApi/WordNet-JWI/edu.mit.jwi_2.1.5.jar (change filename if it is not the same)

4. Download and install 8 JDK and last Maven version.

4. Open a terminal in $ServiceDirectory and copy-paste "mvn install:install-file -Dfile=SemilarAPI/Semilar-1.0.jar -DgroupId=org.semilar -DartifactId=semilar -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true"

5. Open a terminal in  $ServiceDirectory and copy-paste "mvn install:install-file -Dfile=SemilarAPI/WordNet-JWI/edu.mit.jwi_2.1.5.jar -DgroupId=edu.mit -DartifactId=jwi -Dversion=2.1.5 -Dpackaging=jar -DgeneratePom=true"

Steps to run the service:

1. Open a terminal and copy-paste "sh run_similarity_detection.sh". Wait for an exit like this: u.s.s.SimilaritydetectionapiApplication  : Started SimilaritydetectionapiApplication

2. Open a terminal and copy-paste "sh run_semilar.sh". Wait for an exit like this: u.s.semilarapi.SemilarapiApplication  : Started SemilarapiApplication

3. (only if you wanna use the auxiliary client) Open a terminal and copy-paste "sh run_restservice.sh". Wait for an exit like this: u.s.restserviceapi.RestServiceApplication  : Started RestServiceApplication

4. Go to https://api.openreq.eu/similarity-detection/swagger-ui.html to see the swagger generated. You can use the component through the swagger or through http connections to the endpoints indicated in the documentation.


### How to use it

All requests must be sent to "https://api.openreq.eu/similarity-detection/". The service expects a JSON with OpenReqJson format. At this moment the service provides 3 different functionalities with 1 auxiliary operation.

Check API details [here](https://api.openreq.eu/similarity-detection/swagger-ui.html).

### Notes for developers

### Sources

- Rus, V., Lintean, M., Banjade, R., Niraula, N., and Stefanescu, D. (2013). SEMILAR: The Semantic Similarity Toolkit. Proceedings of the 51st Annual Meeting of the Association for Computational Linguistics, August 4-9, 2013, Sofia, Bulgaria. [Available here](http://deeptutor2.memphis.edu/Semilar-Web/public/downloads/ACL-13.SEMILAR.DEMO.pdf)

- Semantic Similarity tool. [Available Here](http://www.semanticsimilarity.org/)

## How to contribute

See OpenReq project contribution [guidelines](https://github.com/OpenReqEU/OpenReq/blob/master/CONTRIBUTING.md)

## License

Free use of this software is granted under the terms of the [EPL version 2 (EPL2.0)](https://www.eclipse.org/legal/epl-2.0/)
