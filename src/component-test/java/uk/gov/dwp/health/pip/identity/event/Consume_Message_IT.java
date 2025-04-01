package uk.gov.dwp.health.pip.identity.event;

import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import uk.gov.dwp.health.pip.identity.entity.Identity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class Consume_Message_IT extends BaseApiTest {

  private final String identityId = UUID.randomUUID().toString();

  @Test
  void consume_valid_identity_request_from_queue_and_process_pip_cs_application() throws JSONException {

    var messageJson = jsonRequestPayload();
    messageUtils.publishMessage(messageJson.toString());

    await()
        .atMost(1, TimeUnit.MINUTES)
        .pollInterval(5, TimeUnit.SECONDS)
        .until(() -> {
          log.debug("messageUtils.getPipCsResponseMessageCount() == {}", messageUtils.getPipCsResponseMessageCount());
          return messageUtils.getPipCsResponseMessageCount().equals("1");
        });

    var sqsMessage = messageUtils.getPipCsResponseMessage();

    var messageBody = new JSONObject(sqsMessage.getBody());
    var messageAttributes = messageBody.getJSONObject("MessageAttributes");
    var message = new JSONObject(messageBody.getString("Message"));

    assertEquals("String", messageAttributes.getJSONObject("x-dwp-routing-key").getString("Type"));
    assertEquals(
        "pip_identity_outbound_routing",
        messageAttributes.getJSONObject("x-dwp-routing-key").getString("Value"));

    assertEquals("verified", message.getString("idv_status"));

    assertEquals("0", messageUtils.getRequestMessageCount());
    assertEquals("0", messageUtils.getDlqMessageCount());
  }

  @Test
  void consume_valid_identity_request_from_queue_and_process_pip_service_application() throws JSONException {

    var messageJson = jsonRequestPayload("67d89108b472041a0fbdbeg4");
    messageJson.put("nino","RN000005A");
    messageUtils.publishMessage(messageJson.toString());

    await()
        .atMost(1, TimeUnit.MINUTES)
        .pollInterval(5, TimeUnit.SECONDS)
        .until(() -> {
          log.debug("messageUtils.getPipServiceResponseMessageCount() == {}", messageUtils.getPipServiceResponseMessageCount());
          return messageUtils.getPipServiceResponseMessageCount().equals("1");
        });

    var sqsMessage = messageUtils.getPipServiceResponseMessage();

    var messageBody = new JSONObject(sqsMessage.getBody());
    var messageAttributes = messageBody.getJSONObject("MessageAttributes");
    var message = new JSONObject(messageBody.getString("Message"));

    assertEquals("String", messageAttributes.getJSONObject("x-dwp-routing-key").getString("Type"));
    assertEquals(
        "pip_identity_coordinator_outbound_routing",
        messageAttributes.getJSONObject("x-dwp-routing-key").getString("Value"));

    assertEquals("verified", message.getString("idv_status"));
    assertNotNull(message.getString("identity_id"));

    assertEquals("0", messageUtils.getPipServiceResponseMessageCount());
    assertEquals("0", messageUtils.getDlqMessageCount());
  }
  @Test
  void consume_valid_identity_request_from_queue_but_reject_as_not_pipcs() throws JSONException {
    assertEquals("0", messageUtils.getRequestMessageCount());
    assertEquals("0", messageUtils.getDlqMessageCount());
    assertEquals(0, countIdentitiesMatching("nino", "RN000010A"));

    final JSONObject messageJson = jsonRequestPayload();
    final String message = messageJson.toString();
    messageUtils.publishMessage(message);

    await()
        .atMost(1, TimeUnit.MINUTES)
        .pollInterval(5, TimeUnit.SECONDS)
        .until(() -> {
          final int identities = countIdentitiesMatching("nino", "RN000010A");
          return identities == 1;
        });

    assertEquals("0", messageUtils.getRequestMessageCount());
    assertEquals("0", messageUtils.getDlqMessageCount());
  }

  @Test
  void consume_valid_identity_request_from_queue_500_response_to_dlq() throws JSONException {

    var messageJson = jsonRequestPayload();
    messageJson.put("nino", "RN000003A");
    messageJson.put("subject_id", "test1@test.com");
    messageUtils.publishMessage(messageJson.toString());

    await().atMost(1, TimeUnit.MINUTES).until(() -> messageUtils.getDlqMessageCount().equals("1"));

    assertEquals("0", messageUtils.getRequestMessageCount());
    assertEquals("0", messageUtils.getPipCsResponseMessageCount());
  }

  @Test
  void consume_valid_identity_request_from_queue_application_id_null() throws JSONException {
    assertEquals("0", messageUtils.getPipCsResponseMessageCount());
    assertEquals("0", messageUtils.getRequestMessageCount());
    assertEquals("0", messageUtils.getDlqMessageCount());

    var messageJson = jsonRequestPayload();
    messageJson.put("nino", "RN000002A");
    messageJson.put("subject_id", "test2@test.com");
    messageUtils.publishMessage(messageJson.toString());

    assertEquals("0", messageUtils.getPipCsResponseMessageCount());
    assertEquals("0", messageUtils.getRequestMessageCount());
    assertEquals("0", messageUtils.getDlqMessageCount());
    await()
        .atMost(1, TimeUnit.MINUTES)
        .pollInterval(10, TimeUnit.SECONDS)
        .until(() -> countIdentitiesMatchingRegex("errorMessage", "Application ID not found for identity with id:.*") == 1);
  }

  @Test
  void consume_valid_identity_request_from_queue_application_id_conflict() throws JSONException {

    var messageJson = jsonRequestPayload();
    messageJson.put("nino", "RN000001A");
    messageJson.put("subject_id", "test3@test.com");
    messageUtils.publishMessage(messageJson.toString());

    await()
        .atMost(1, TimeUnit.MINUTES)
        .until(() -> messageUtils.getRequestMessageCount().equals("0"));

    assertEquals("0", messageUtils.getPipCsResponseMessageCount());
    assertEquals("0", messageUtils.getDlqMessageCount());
  }

  @Test
  void consume_valid_identity_request_duplicate_nino_error_is_not_thrown() throws JSONException {
    var messageJson = jsonRequestPayload();
    messageUtils.publishMessage(messageJson.toString());

    var secondMessageJson = jsonRequestPayload();
    secondMessageJson.put("identity_id", UUID.randomUUID());
    secondMessageJson.put("timestamp", getCurrentDate());
    messageUtils.publishMessage(secondMessageJson.toString());

    await()
            .atMost(1, TimeUnit.MINUTES)
            .until(() -> messageUtils.getRequestMessageCount().equals("0"));

    assertEquals("0", messageUtils.getDlqMessageCount());
  }

  @Test
  void consume_valid_identity_request_duplicate_email_error_is_not_thrown() throws JSONException {
    var messageJson = jsonRequestPayload();
    messageUtils.publishMessage(messageJson.toString());

    var secondMessageJson = jsonRequestPayload();
    secondMessageJson.put("nino", "RN000007A");
    secondMessageJson.put("identity_id", UUID.randomUUID());
    secondMessageJson.put("timestamp", getCurrentDate());
    messageUtils.publishMessage(secondMessageJson.toString());

    await()
            .atMost(1, TimeUnit.MINUTES)
            .until(() -> messageUtils.getRequestMessageCount().equals("0"));

    assertEquals("0", messageUtils.getDlqMessageCount());
  }

  private JSONObject jsonRequestPayload() throws JSONException {
    return jsonRequestPayload(new ObjectId().toString());
  }

  private JSONObject jsonRequestPayload(final String applicationId) throws JSONException {
    JSONObject messageJson = new JSONObject();
    messageJson.put("channel", "oidv");
    messageJson.put("timestamp", getCurrentDate());
    messageJson.put("identity_id", identityId);
    messageJson.put("idv_outcome", "verified");
    messageJson.put("nino", "RN000010A");
    messageJson.put("subject_id", "positive@test.com");
    messageJson.put("applicationId", applicationId);
    return messageJson;
  }

  private String getCurrentDate() {
    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    LocalDateTime localDateTime = LocalDateTime.now();
    return localDateTime.format(format);
  }

  private static int countIdentitiesMatching(final String key, final String value) {
    final MongoTemplate mongoTemplate = getMongoTemplate();
    final Criteria criteria = Criteria.where(key).is(value);
    final Query query = Query.query(criteria);
    return (int) mongoTemplate.count(query, Identity.class);
  }

  private static int countIdentitiesMatchingRegex(final String key, final String regex) {
    final MongoTemplate mongoTemplate = getMongoTemplate();
    final Criteria criteria = Criteria.where(key).regex(regex);
    final Query query = Query.query(criteria);
    return (int) mongoTemplate.count(query, Identity.class);
  }

}
