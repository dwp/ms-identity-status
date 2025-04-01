# ms-identity-status

Identity Status microservice stores identity details supplied. Application id verification is also present to make sure
the details are stored properly.

## Dependency

the API stores and queries user's application details from a Mongo database. To be able to successfully start the
application, the application must be able to connect to a Mongo instance at start up.

the API consumes from a Queue and posts response to Topic. Both these channels needs to be configured appropriately.

## Rest API

the api is built from the [openapi-spec.yaml](api-spec/openapi-spec-registration.yaml)

## Running the application

this is a standard SpringBoot application with all the configuration items held in `src/main/resources/application.yml`
and bundled into the project at build.

## Running ms-identity-status locally (IntelliJ)

To run the application locally, first generate the application jar using maven:
```shell
mvn clean install
```

## Then run the docker compose file:

```shell
docker compose up -d --build 
```

### Gitlab registry 

If any issues are faced pulling images using the docker compose command above, ensure you are logged in to gitlab registry 

```shell
docker login registry.gitlab.com
```

After that, you should have the base for running the app.

To run the spring boot application,

Execute the Run Configuration called 'ms-identity-status (Local Run)' in IntelliJ.

```shell
ms-identity-status (Local Run)
```

To edit any Run Configuration settings, the config is found under .run directory under project root

## Running API TESTS

To run the api tests, first generate the application jar using maven:
```shell
mvn clean install
```
## If running the local profile, make sure to run docker compose down

```shell
docker compose down 
```

## Then run the docker compose file with api-test profile:

```shell
docker compose --profile api-test up -d --build 
```

### Gitlab registry

If any issues are faced pulling images using the docker compose command above, ensure you are logged in to gitlab registry

```shell
docker login registry.gitlab.com
```
## .env file

the `.env` file can be used to inject the docker-compose environment variables. they carry defaults for the main
variables except for the `ms-identity-status` container, this is intentionally left blank. Please fill this with a local
container (or a gitlab container-registry container value) to run the tests locally.

## Environment Variables

| environment variable                                                   | description                                                  | example                             |
|------------------------------------------------------------------------|--------------------------------------------------------------|-------------------------------------|
| `LOGGING_LEVEL_ROOT`                                                   | the log level                                                | INFO                                |
| `SPRING_DATA_MONGODB_URI`                                              | the mongodb connection string                                | mongodb://user:pass@localhost:27017 |
| `SPRING_DATA_MONGODB_DATABASE`                                         | the mongodb database                                         | example-db                          |
| `SERVER_PORT`                                                          | the running port for the server                              | 8080                                |
| `UK_GOV_DWP_HEALTH_EVENT_CRYPTO_CONFIG_CACHE_KMS_DATA_KEY`             | cache kms keys in memory                                     | true                                |
| `UK_GOV_DWP_HEALTH_EVENT_CRYPTO_CONFIG_DATA_KEY_ID`                    | the kms alias for event payload encryption                   | alias/example_kms                   |
| `UK_GOV_DWP_HEALTH_EVENT_CRYPTO_CONFIG_KMS_ENDPOINT_OVERRIDE`          | the endpoint override (only for local running)               | http://localhost:4566               |
| `UK_GOV_DWP_HEALTH_INTEGRATION_OUTBOUND_TOPIC_EXCHANGE`                | the publish topic                                            | example-topic                       |
| `UK_GOV_DWP_HEALTH_INTEGRATION_SNS_ENDPOINT_OVERRIDE`                  | the sns endpoint override (only for local running)           | http://localhost:4566               |
| `UK_GOV_DWP_HEALTH_INTEGRATION_SQS_ENDPOINT_OVERRIDE`                  | the sqs endpoint override (only for local running)           | http://localhost:4566               |
| `SNS_MSG_ROUTING_KEY`                                                  | the routing key for publish messages                         | new.event                           |
| `SQS_MSG_QUEUE_NAME`                                                   | the SQS queue name                                           | example-queue                       |
| `SQS_MSG_ROUTING_KEY_PREFIX`                                           | the routing key prefix (for queue subscription)              | new                                 |
| `FEATURE_ENCRYPTION_MESSAGING_ENABLED`                                 | to encryption event payloads (true/false)                    | false                               |                                                          
| `UK_GOV_DWP_HEALTH_INBOUND_TOPIC`                                      | inbound topic name for Identity message                      | pip_identity_event_nino             |
| `UK_GOV_DWP_HEALTH_INBOUND_QUEUE_NAME_IDENTITY_RESPONSE`               | inbound queue name for Identity message                      | pip_idv_outcome                     |
| `UK_GOV_DWP_HEALTH_INBOUND_ROUTING_KEY_IDENTITY_RESPONSE`              | routing key to access the inbound message (to be confirmed)  | pip_identity_inbound_routing        |
| `UK_GOV_DWP_HEALTH_PIPCS_OUTBOUND_ROUTING_KEY_IDENTITY_RESPONSE`       | routing key to access the outbound message (to be confirmed) | pip_identity_outbound_routing       |
| `UK_GOV_DWP_HEALTH_PIPCS_OUTBOUND_QUEUE_NAME_IDENTITY_RESPONSE`        | identity response outbound queue                             | update_pipcs_idv_queue              |
| `UK_GOV_DWP_HEALTH_PIPCS_OUTBOUND_TOPIC`                               | identity response response topic                             | update_pipcs_idv_topic              |
| `UK_GOV_DWP_HEALTH_COORDINATOR_OUTBOUND_TOPIC`                         | coordinator outbound identity verification topic             |                                     |
| `UK_GOV_DWP_HEALTH_COORDINATOR_OUTBOUND_ROUTING_KEY_IDENTITY_RESPONSE` | coordinator outbound identity verification routing key       |                                     |
| `UK_GOV_DWP_HEALTH_COORDINATOR_OUTBOUND_QUEUE_NAME_IDENTITY_RESPONSE`  | coordinator outbound identity verification queue             |                                     |



