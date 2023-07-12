package uk.gov.dwp.health.pip.identity.event;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import uk.gov.dwp.health.pip.identity.event.utils.HttpUtils;
import uk.gov.dwp.health.pip.identity.event.utils.MessageUtils;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
abstract class BaseApiTest {

  protected static MessageUtils messageUtils;
  protected static HttpUtils httpUtils;

  @BeforeAll
  static void beforeAll() {
    var awsEndpointOverride = getEnv("AWS_ENDPOINT_OVERRIDE", "http://localhost:4566");
    var awsRegion = getEnv("AWS_REGION", "eu-west-2");

    var requestTopicArn =
        getEnv("REQUEST_TOPIC_ARN", "arn:aws:sns:eu-west-2:000000000000:pip_identity_event_nino");

    var requestRoutingKey = getEnv("REQUEST_ROUTING_KEY", "pip_identity_inbound_routing");
    var requestQueueUrl =
        getEnv("REQUEST_QUEUE_URL", "http://localhost:4566/000000000000/pip_idv_outcome");
    var deadLetterQueueUrl =
        getEnv("DEAD_LETTER_QUEUE_URL", "http://localhost:4566/000000000000/pip_idv_outcome_dlq");
    var responseQueueUrl =
        getEnv("RESPONSE_QUEUE_URL", "http://localhost:4566/000000000000/update_pipcs_idv_queue");

    messageUtils =
        new MessageUtils(
            awsEndpointOverride,
            awsRegion,
            requestTopicArn,
            requestRoutingKey,
            requestQueueUrl,
            deadLetterQueueUrl,
            responseQueueUrl);

    var wireMockHost = getEnv("WIREMOCK_HOST", "http://localhost:9970");

    httpUtils = new HttpUtils(wireMockHost);
  }

  private static String getEnv(String name, String defaultValue) {
    String env = System.getenv(name);
    return env == null ? defaultValue : env;
  }

  @BeforeEach
  void beforeEach() {
    messageUtils.purgeResponseQueue();
    messageUtils.purgeDeadLetterQueue();
  }
}
