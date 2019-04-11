# Similarity Detection

_This service was created as a result of the OpenReq project funded by the European Union Horizon 2020 Research and Innovation programme under grant agreement No 732463._

## Introduction

The component is based in td-idf numerical statistic. The aim of the API is to calculate the similarity score between multiple pairs of requirements.

## Technical description

Next sections provide a general overview of the technical details of the similarity detection service.

### Main functionalities

There are three different types of operations:

	- ReqReq: Compares two requirements
	- ReqProj: Compares between a list of requirements and a set of requirements
	- Proj: Compares all possible pairs of requirements from a set of requirements

The component needs to preprocess the requirements before doing any comparison. The operation BuildModel is responsible for that work.

The API uses UTF-8 charset. Also, it uses the OpenReq format for input and output JSONs.


### Used technologies

The service is divided into two independent components: SimilarityDetectionAPI and SemilarAPI. The first one is the main component in charge of receiving the requests and returning the final results whereas Semilar computes the similarity dependencies between the input requirements.

    - SimilarityDetectionAPI: main component
    - SemilarAPI: auxiliary component
    - testing: directory with input and output examples used for testing
    - models.db: requirements database for semilar library
    - LICENSE: license



### How to install

Steps to configure the service:

1. Download and install 8 JDK and last Maven version.

Steps to run the service:

1. Open a terminal and copy-paste "sh run_similarity_detection.sh". Wait for an exit like this: u.s.s.SimilaritydetectionapiApplication  : Started SimilaritydetectionapiApplication

2. Open a terminal and copy-paste "sh run_semilar.sh". Wait for an exit like this: u.s.semilarapi.SemilarapiApplication  : Started SemilarapiApplication

4. Go to http://localhost:9404/swagger-ui.html#/ to see the swagger generated. You can use the component through the swagger or through http connections to the endpoints indicated in the swagger documentation.


### How to use it

The service expects a JSON with OpenReqJson format. At this moment the service provides 3 different functionalities with 1 auxiliary operation.

Check API details [here](https://api.openreq.eu/similarity-detection/swagger-ui.html).

### Notes for developers

### Sources

## How to contribute

See OpenReq project contribution [guidelines](https://github.com/OpenReqEU/OpenReq/blob/master/CONTRIBUTING.md)

## License

Free use of this software is granted under the terms of the [EPL version 2 (EPL2.0)](https://www.eclipse.org/legal/epl-2.0/)
