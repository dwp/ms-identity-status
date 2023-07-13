# ms-identity-status

microservice to store the identity details supplied from a queue. Application id verification also present to make sure
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

## environment-variables

| environment variable                                            | description                                                  | example                             |
|-----------------------------------------------------------------|--------------------------------------------------------------|-------------------------------------|
| `LOGGING_LEVEL_ROOT`                                            | the log level                                                | INFO                                |
| `SPRING_DATA_MONGODB_URI`                                       | the mongodb connection string                                | mongodb://user:pass@localhost:27017 |
| `SPRING_DATA_MONGODB_DATABASE`                                  | the mongodb database                                         | example-db                          |
| `SERVER_PORT`                                                   | the running port for the server                              | 8080                                |
| `UK_GOV_DWP_HEALTH_EVENT_CRYPTO_CONFIG_CACHE_KMS_DATA_KEY`      | cache kms keys in memory                                     | true                                |
| `UK_GOV_DWP_HEALTH_EVENT_CRYPTO_CONFIG_DATA_KEY_ID`             | the kms alias for event payload encryption                   | alias/example_kms                   |
| `UK_GOV_DWP_HEALTH_EVENT_CRYPTO_CONFIG_KMS_ENDPOINT_OVERRIDE`   | the endpoint override (only for local running)               | http://localhost:4566               |
| `UK_GOV_DWP_HEALTH_INTEGRATION_OUTBOUND_TOPIC_EXCHANGE`         | the publish topic                                            | example-topic                       |
| `UK_GOV_DWP_HEALTH_INTEGRATION_SNS_ENDPOINT_OVERRIDE`           | the sns endpoint override (only for local running)           | http://localhost:4566               |
| `UK_GOV_DWP_HEALTH_INTEGRATION_SQS_ENDPOINT_OVERRIDE`           | the sqs endpoint override (only for local running)           | http://localhost:4566               |
| `SNS_MSG_ROUTING_KEY`                                           | the routing key for publish messages                         | new.event                           |
| `SQS_MSG_QUEUE_NAME`                                            | the SQS queue name                                           | example-queue                       |
| `SQS_MSG_ROUTING_KEY_PREFIX`                                    | the routing key prefix (for queue subscription)              | new                                 |
| `FEATURE_ENCRYPTION_MESSAGING_ENABLED`                          | to encryption event payloads (true/false)                    | false                               |                                                          
| `UK_GOV_DWP_HEALTH_INBOUND_TOPIC`                               | inbound topic name for Identity message                      | pip_identity_event_nino             |
| `UK_GOV_DWP_HEALTH_INBOUND_QUEUE_NAME_IDENTITY_RESPONSE`        | inbound queue name for Identity message                      | pip_idv_outcome                     |
| `UK_GOV_DWP_HEALTH_INBOUND_ROUTING_KEY_IDENTITY_RESPONSE`       | routing key to access the inbound message (to be confirmed)  | pip_identity_inbound_routing        |
| `UK_GOV_DWP_HEALTH_OUTBOUND_ROUTING_KEY_IDENTITY_RESPONSE`      | routing key to access the outbound message (to be confirmed) | pip_identity_outbound_routing       |
| `UK_GOV_DWP_HEALTH_OUTBOUND_QUEUE_NAME_IDENTITY_RESPONSE`       | identity response outbound queue                             | update_pipcs_idv_queue              |
| `UK_GOV_DWP_HEALTH_OUTBOUND_TOPIC`                              | identity response response topic                             | update_pipcs_idv_topic              |

## local testing

the `.env` file can be used to inject the docker-compose environment variables. they carry defaults for the main
variables except for the `ms-identity-status` container, this is intentionally left blank. Please fill this with a local
container (or a gitlab container-registry container value) to run the tests locally.

## Running on docker

### Pre-req:

The latest version of ms-identity-status need to get the latest version of `pip-apply-mocks` in local. This needs to be
downloaded from AWS as it is not available in Nexus. To achieve this,

- `AWS CLI` needs to be installed in the laptop (available in self service)
- one done, generate aws_access_key_id and aws_secret_access_key from your AWS account.
    - for more details refer this
      page: https://docs.aws.amazon.com/powershell/latest/userguide/pstools-appendix-sign-up.html
- check for `.aws` folder in the USER_HOME directory. If not present create one.
- create two files (if not present) named `config` and `credentials`
- contents for `config` are below
  <pre>[default]
  region = eu-west-2</pre>
- content for `credentials` are below,
    <pre>[default]
  aws_access_key_id = YOUR_GENERATED_ACCESS_KEY_ID 
  aws_secret_access_key = YOUR_GENERATED_SECRET </pre>

### Local run in docker

to run the app on docker locally, run the following commands:

- docker image rm local/ms-identity-status (to make sure the old image is removed)
- mvn clean verify

Then run the following command in another terminal window:

<pre>
sh scripts/ms-identity-status.sh
</pre>

NOTE: This script logs into AWS ECR to get the latest `pip-apply-mocks` functionality and starts up
the build locally. This script has been redacted from the public domain 
as it contains a reference to a private ECR repository.

## Running it locally

First run docker compose down then run - docker compose -f no-service.yml up

After that, you should have the base for running the app.

set the env variables locally, on intellij idea:

Run > Edit configurations > set env variables to :

AWS_ACCESS_KEY_ID=dummyaccess; AWS_SECRET_ACCESS_KEY=dummysecret; AWS_DEFAULT_REGION=eu-west-2;
SPRING_DATA_MONGODB_URI=mongodb://mongo:27017;SPRING_DATA_MONGODB_DATABASE=identity;
UK_GOV_DWP_HEALTH_INTEGRATION_AWS_REGION=eu-west-2; UK_GOV_DWP_HEALTH_EVENT_CRYPTO_CONFIG_CACHE_KMS_DATA_KEY=true;
UK_GOV_DWP_HEALTH_EVENT_CRYPTO_CONFIG_DATA_KEY_ID=alias/example_kms;
UK_GOV_DWP_HEALTH_EVENT_CRYPTO_CONFIG_KMS_ENDPOINT_OVERRIDE=http://localstack:4566;
UK_GOV_DWP_HEALTH_INTEGRATION_OUTBOUND_TOPIC_EXCHANGE=example-topic;
UK_GOV_DWP_HEALTH_INTEGRATION_SNS_ENDPOINT_OVERRIDE=http://localstack:4566;
UK_GOV_DWP_HEALTH_INTEGRATION_SQS_ENDPOINT_OVERRIDE=http://localstack:4566; SNS_MSG_ROUTING_KEY=new.event;
SQS_MSG_QUEUE_NAME=example-queue; SQS_MSG_ROUTING_KEY_PREFIX=new; FEATURE_ENCRYPTION_MESSAGING_ENABLED=false;
UK_GOV_DWP_HEALTH_INBOUND_QUEUE_NAME_IDENTITY_RESPONSE=pip_idv_outcome;
UK_GOV_DWP_HEALTH_INBOUND_TOPIC=pip_identity_event_nino;
UK_GOV_DWP_HEALTH_INBOUND_ROUTING_KEY_IDENTITY_RESPONSE=pip_identity_inbound_routing;
UK_GOV_DWP_HEALTH_OUTBOUND_ROUTING_KEY_IDENTITY_RESPONSE=pip_identity_outbound_routing;
UK_GOV_DWP_HEALTH_OUTBOUND_QUEUE_NAME_IDENTITY_RESPONSE=update_pipcs_idv_queue;
UK_GOV_DWP_HEALTH_OUTBOUND_TOPIC=update_pipcs_idv_topic;

Right click run on Application, it should load the app on the command line. 
