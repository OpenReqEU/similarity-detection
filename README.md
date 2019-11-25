# Similarity Detection

_This service was created as a result of the OpenReq project funded by the European Union Horizon 2020 Research and Innovation programme under grant agreement No 732463._

## Introduction

The component is based in tf-idf numerical statistic. The aim of the API is to calculate the similarity score between multiple pairs of requirements.

## Technical description

Next sections provide a general overview of the technical details of the similarity detection service.

### Main functionalities

There are three types of operations (each method has a more extensive description in its own operation box):

- Similarity without clusters: These methods work only with a set of input requirements. There are two types of operations. The ones that generate and update a tf-idf model with the input requirements, and the ones that use these tf-idf models to compute the similarity between the requirements; therefore, before using the operations, is necessary to create a tf-idf model first for the organization. Each tf-idf model is assigned to an organization and can be used infinite times.
    - BuildModel: Pre-processes the input requirements, generates a tf-idf model and assings it to an specified organization.
    - BuildModelAndCompute: Pre-processes the input requirements, generates a tf-idf model with the input requirements and computes and returns the similarity score between all the possible pairs of requirements.
    - AddRequirements: Pre-processes the input requirements and adds them to an existing tf-idf model.
    - DeleteRequirements: Deletes the input requirements from an existing tf-idf model.
    - ReqReq: Compares two requirements.
    - ReqProject: Compares all the requirements in the input list with a set of requirements given in the input.
    - Project: Compares all possible pairs of requirements from a set of requirements.
    - ProjectProject: Compares each requirement of a project with all the requirements of another project
    - ReqOrganization: Compares a set of requirements with all the requirements of an organization.
    - NewReqOrganization: Pre-processes the input requirements and adds them to an organization's tf-idf model. It also compares the input requirements with all the requirements of the organization.
- Similarity with clusters: These methods work with a set of input requirements and dependencies. The clusters are considered as graphs connected by similarity dependencies accepted by the user where the nodes are the requirements of the model. We denominate orphans to the clusters with only one requirement.
    - BuildClusters: Pre-processes the input requirements, generates a tf-idf model with the requirements information, the clusters architecture and the input similarity dependencies and assigns it to an specified organization. All the requirements are compared with all the requirements of other clusters of the organization, and the maximum score with each cluster for each requirement is stored in the database.
    - BuildClustersAndCompute: Pre-processes the input requirements, generates a tf-idf model with the requirements information, the clusters architecture and the input similarity dependencies and assings it to an specified organization. All the requirements are compared with all the requirements of other clusters of the organization, and the maximum score with each cluster for each requirement is stored in the database. The method returns the maximum similarity score between each requirement and all the requirements that make up each one of the existing clusters of the organization.
    - ReqClusters: Given a list of requirements ids, returns the maximum similarity score between each requirement and all the requirements that make up each of the existing clusters in the organization model.
    - BatchProcess: Given a set of updates done in the requirements, updates the clusters and dependencies accordingly.
    - TreatAcceptedAndRejectedDependencies: Given a set of accepted and rejected dependencies, updates the clusters and dependencies accordingly.
- Auxiliary methods:
    - GetResponse: Returns the output of the async methods
    - GetOrganizationInfo: Returns the main information of the specified organization including the current computations and the pending responses
    - DeleteOrganizationResponses: Deletes the organization responses from the database
    - DeleteDatabase: Deletes all data from the database

The API uses UTF-8 charset. Also, it uses the OpenReq format for input JSONs


### Asynchronous service

All operations except ReqReq, ReqClusters, TreatAcceptedAndRejectedDependencies and the auxiliary methods are asynchronous. All these operations follow the same pattern:

- The client calls the operation with all necessary parameters
- The service receives the request and checks the main conditions
- The service returns if the client request has been accepted or not and closes the connection
    - (httpStatus!=200) The request has not been accepted. The message body contains the exception cause.
    - (httpStatus==200) The request has been accepted. The similarity calculation runs in the background. The message body contains the request identifier, i.e., {"id": "1548924677975_523"}
- When the calculation finishes (only if the request has been accepted) the service opens a connection with the server url specified as parameter (optional). It sends a JSON object that contains the outcome of the computation:
    - (success) Example: {"code": 200,"id": "1557395889689_587","operation": "AddReqs"}.
    - (!success) Example: {"code": 400,"id": "1557396039530_583","error": "Bad request","message": "The requirement with id QM-3 is already inside the project","operation": "ReqProject"}.
- The result of the operation can be obtained through the GetResponse method.

    
### Concurrency control

The service uses locks to control the concurrency of the different operations in each organization. To guarantee the ACID properties the methods which write or update data can't be parallelized. For example, the BuildClusters and BatchProcess methods cannot be parallelized, one has to wait for the other before starting. The service has a small queue of about 5 minutes to solve the small deviations. However, if one method has to wait for more than 5 minutes it will return an error with code 423. This only happens with methods using the same organization. The methods like ReqClusters that only read data from the database are not affected.


### Files structure

The service is divided into two independent components: SimilarityDetectionAPI(port=9404) and ComparerAPI(port=9405). The first one is the main component in charge of receiving the requests and returning the final results whereas Comparer computes the similarity dependencies between the input requirements. The TestingRestService(port=9406) is just a client example to test the service.

- SimilarityDetectionAPI: main component
- ComparerAPI: auxiliary component
- ComparerAPI/data: directory where the data files are located
- TestingRestSercice: example of client
- testing: directory with input and output examples used to test
- auxiliary_files: directory used for saving the files of the auxiliary client and temporary saving the input multipart files
- config_files: directory where the configuration files are stored
- LICENSE: license


### How to install

Steps to configure the service:

1. Download and install 8 JDK and last Maven version.

2. Run the service (Follow the steps explained in the next section)

3. Create the default database file calling the method ClearDatabase exposed in the API i.e. curl -X DELETE 'http://localhost:9404/upc/similarity-detection/ClearDatabase'

Steps to run the service:

1. Open a terminal and copy-paste "sh run_similarity_detection.sh". Wait for an exit like this: u.s.s.SimilaritydetectionapiApplication : Started SimilaritydetectionapiApplication

2. Open a terminal and copy-paste "sh run_comparer.sh". Wait for an exit like this: u.s.comparerapi.ComparerApplication : Started ComparerapiApplication

3. (only if you wanna use the auxiliary client) Open a terminal and copy-paste "sh run_restservice.sh". Wait for an exit like this: u.s.restserviceapi.RestServiceApplication : Started RestServiceApplication

4. Go to http://localhost:9404/swagger-ui.html#/ to see the swagger generated. You can use the component through the swagger or through http connections to the endpoints indicated in the swagger documentation.


### How to use it

The service expects a JSON with OpenReqJson format.

Check API details [here](https://api.openreq.eu/similarity-detection/swagger-ui.html#/).


## Used technologies

The similarity service uses SQLite as the database engine.


## Notes for developers


### Database structure

The database files are stored in the directory with path ComparerAPI/data. The file called main.db saves the names  and the responses of all the organizations (saving the responses of each organization in different files could be a further improvement). Each organization has a file to save its data (tfidf model and clusters architecture). Apart from that, there are other files with .wall or .sh terminations which are auxiliary files used by SQLite.


### Scheduled tasks

The service has one scheduled task called deleteOldResponses which deletes all responses from more than seven days ago. The task is executed once every week.


### Concurrency notes

There are two things to take into account. The first one is the ACID properties, is not possible to have two writing methods like BatchProcess working at the same time without control. Because the situation is very likely to finish with a lost update error or data corruption. Also, we have to take into account that SQLite is a file format database. This has a lot of advantages but has one important drawback: when lots of users are writing and reading data at the same time (is impossible to have two users writing at the same time). To get over these "issues" the service has been implemented in the following way:

- The [wal-mode](https://www.sqlite.org/wal.html) is activated in all the database files. This allows multiple users to read while another user is writing. So, methods like ReqClusters which only read data from the database can be executed at any time.
- Each organization has a different database file to improve parallelization.
- The methods that write or update data to the database always do atomic changes (sometimes this is done thanks to auxiliary tables). So ReqClusters or any other method never return temporary data.
- The service has two types of locks to avoid two users writing at the same time. The locks are implemented as RestraintLocks from java.util.concurrent.locks library. This library allows to order the threads like a priority queue and to define a max time that the threads can wait until holding the lock.
    - Main database lock: The main.db file has a unique lock. If one thread tries to hold this lock but has to wait longer than the defined time an InternalErrorException is thrown with the message "The database is busy".
    - Organization locks: Each organization has a lock saved in a ConcurrentHashMap. This lock allows to control ACID properties and to avoid two users writing at the same time. If one thread tries to hold this lock but has to wait longer than the defined time a LockedException is thrown with the message "The organization database is locked, another thread is using it".

### Description of each procedure/algorithm

- Preprocess Pipeline: It is composed of the following 6 steps:
    - Text cleaning (own preprocess step done with regex)
        - Substitute code fragments with a clause
        - Delete strange characters
        - Delete punctuation symbols
    - Tokenizer ([from apache lucene library](https://lucene.apache.org/core/))
    - Lowercase ([from apache lucene library](https://lucene.apache.org/core/))
    - Stop words removal ([from apache lucene library](https://lucene.apache.org/core/))
    - Stemmer ([from apache lucene library](https://lucene.apache.org/core/))
    - Bi-grams discovering ([from apache lucene library](https://lucene.apache.org/core/))

- Similarity Algorithm: There are two tf idf modalities:
    - tf_idf (default):  It is implemented as the well known term frequency–inverse document frequency algorithm. Apart from saving the tf-idf vector of each requirement, the service also saves the number of appearances in the input requirements of each word. Thanks to that, we are able to add or delete requirements without needing to reconstruct the entirety of the model. It has been modified to allow adding new requirements or deleting old ones. Whenever the input requirements are less than one hundred the smoothing technique is incorporated to return logical recommendations. The similarity score between two requirements is computed with the cosine similarity measure among their tf-idf vectors. The configurable parameters are:
        - *cut_off*: A double number. Determines the minimum tfidf word value accepted. It is used to delete the words that appear in the majority of the input requirements. Realize that increasing the value of this variable reduces the number of words used in the comparison between requirements (maybe decreasing the accuracy) and reduces the time consumed to compute the model and compute the similarity scores.
        - *smoothing*: A boolean value. Determines if the smoothing should be used. The smoothing consists of disabling the cut_off filter in the input models with less than 100 requirements and increasing the words value in a determined constant number. This can be used to compute the similarity in a little group of requirements. However, it is not recommended to use the tfIdf algorithm with little sets of requirements.
    - tf_idf_double: it is implemented as the prior algorithm except for the similarity score computation. It is first computed the score between the words with greater tfidf value (the topics) and if they are similar it is then computed the score between the words with lower values (the matter at hand). In this way, the requirements which have the same topic and talk about the same issue have a higher score than the requirements which only have the same topic. The configurable parameters are:
        - *cut_off*: as seen before.
        - *smoothing*: as seen before.
        - *topic_threshold*: An integer number. Determines the number of words selected as topics. We only select as topics the specified number of words that have the highest tf idf values.
        - *cut_off_topics*: A double number between 0 and 1. Determines the threshold of the topic comparison. When two requirements have a score above this threshold when comparing their topic words it is considered that they have the same topic. 
        - *importance_low*: A double number between 0 and 1. It is a percentage that determines the importance of the words with low tf_idf value against the topic words.

- Clusters Algorithm: Consists of saving the user feedback as graphs where the nodes are requirements and the edges are accepted or rejected dependencies. It recommends proposed pairs of similar requirements to the user taking into account the tf-idf value of the previous similarity algorithm and the existing clusters (i.e., it returns similar requirements with a single requirement of a cluster, the one having the highest similarity score). The user can accept or reject the proposed dependencies. This information is collected by the service in batches and used in preceding recommendations.

### Configuration files

The service has two config files that allow the developers to adapt the service to their needs. They are read at the start of the service building process. So any change in these files only take effect after the service is restarted.

- Main config file (/config_files/config.json):
    - *preprocess_pipeline*: Determines the preprocess pipeline. One of the next sections explains how to replace the default pipeline.
    - *similarity_algorithm*: Determines the similarity algorithm. One of the next sections explains how to replace the default algorithm.
    - *clusters_algorithm*: Determines the clusters algorithm.One of the next sections explains how to replace the default algorithm.
    - *database_path*: Determines the database path. If it is replaced by a relative path, it must come from inside the CompareAPI directory.
    - *max_dependencies_page*: Determines the size of the dependencies patches returned in the GetResponse method.
    - *max_waiting_time_seconds*: Determines the organization lock time. See the *Concurrency notes* section for more information. It is not recommended to set this variable to more than 10 minutes (be aware that the variable is expecting seconds not minutes).

- TfIdf config file (/config_files/config_tfidf.json):
    - *cut_off*: explained in the previous section.
    - *smoothing*: explained in the previous section.

- TfIdfDouble config file (/config_files/config_tfidf_double.json):
    - *cut_off*: explained in the previous section.
    - *smoothing*: explained in the previous section.
    - *topic_threshold*: explained in the previous section.
    - *cut_off_topics*: explained in the previous section.
    - *importance_low*: explained in the previous section.

### Replacing the algorithms

The similarity algorithm, the preprocess pipeline and the clusters algorithm are decoupled from the api code so they can be easily substituted. 

To change the preprocess pipeline it is necessary to do the following changes:
- Create a class that implements the interface named *PreprocessPipeline*.
- Change the variable called *preprocess_pipeline* in the main config file.
- Add your preprocess pipeline in the method named *selectPreprocessPipeline* inside the *Constants* class. Assign the right class to the variable *preprocessPipeline*.

To change the similarity algorithm it is necessary to do the following changes:
- Create a class that implements the interface named *SimilarityAlgorithm*. This module is in charge of building the model and compute the similarity between pairs of requirements.
- Create a class that implements the interface named *SimilarityModel*. This module is in charge of saving the model data structures of the similarity algorithm.
- Create a class that implements the interface named *SimilarityModelDatabase*. This module is in charge of saving and loading the similairty algorithm model from the database.
- Change the variable called *similarity_algorithm* from the main config file.
- Add your similarity algorithm in the method named *selectSimilarityAlgorithm* inside the *Constants* class. Assign the right classes to the variables *similarityAlgorithm* and *similarityModelDatabase*.

To change the clusters algorithm it is necessary to do the following changes:
- Create a class that implements the interface named *ClustersAlgorithm*. This module is in charge of building the clusters model.
- Create a class that implements the interface named *ClustersModel*. This module is in charge of saving the model data structures of the clusters algorithm.
- Create a class that implements the interface named *ClustersModelDatabase*. This module is in charge of saving and loading the clusters algorithm model from the database.
- Change the variable called *clusters_algorithm* from the main config file.
- Add your clusters algorithm in the method named *selectClustersAlgorithm* inside the *Constants* class. Assign the right classes to the variables *clustersAlgorithm* and *clustersModelDatabase*.


## How to contribute

See OpenReq project contribution [guidelines](https://github.com/OpenReqEU/OpenReq/blob/master/CONTRIBUTING.md)


## License

Free use of this software is granted under the terms of the [EPL version 2 (EPL2.0)](https://www.eclipse.org/legal/epl-2.0/)
