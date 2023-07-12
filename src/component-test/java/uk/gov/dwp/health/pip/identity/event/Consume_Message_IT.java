package uk.gov.dwp.health.pip.identity.event;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Consume_Message_IT extends BaseApiTest {

  private final UUID identityId = UUID.randomUUID();

  @Test
  void consume_valid_identity_request_from_queue_and_process_positive() throws JSONException {

    var messageJson = jsonRequestPayload();
    messageUtils.publishMessage(messageJson.toString());

    await()
        .atMost(1, TimeUnit.MINUTES)
        .until(() -> messageUtils.getResponseMessageCount().equals("1"));

    var sqsMessage = messageUtils.getResponseMessage();

    var messageBody = new JSONObject(sqsMessage.getBody());
    var messageAttributes = messageBody.getJSONObject("MessageAttributes");
    var message = new JSONObject(messageBody.getString("Message"));

    assertEquals("String", messageAttributes.getJSONObject("x-dwp-routing-key").getString("Type"));
    assertEquals(
        "pip_identity_outbound_routing",
        messageAttributes.getJSONObject("x-dwp-routing-key").getString("Value"));

    assertEquals("verified", message.getString("idv_status"));
    assertNotNull(message.getString("identity_id"));

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
    assertEquals("0", messageUtils.getResponseMessageCount());
  }

  @Test
  void consume_valid_identity_request_from_queue_application_id_null() throws JSONException {

    var messageJson = jsonRequestPayload();
    messageJson.put("nino", "RN000002A");
    messageJson.put("subject_id", "test2@test.com");
    messageUtils.publishMessage(messageJson.toString());

    // this should not hit PIPCS IDV queue as we don't get an application id back and write an error message
    // to the collection
    assertEquals("0", messageUtils.getResponseMessageCount());
    assertEquals("0", messageUtils.getRequestMessageCount());
    assertEquals("0", messageUtils.getDlqMessageCount());
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

    assertEquals("0", messageUtils.getResponseMessageCount());
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
    JSONObject messageJson = new JSONObject();
    messageJson.put("channel", "oidv");
    messageJson.put("timestamp", getCurrentDate());
    messageJson.put("identity_id", identityId);
    messageJson.put("idv_outcome", "verified");
    messageJson.put("nino", "RN000010A");
    messageJson.put("subject_id", "positive@test.com");
    return messageJson;
  }

  private String getCurrentDate() {
    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    LocalDateTime localDateTime = LocalDateTime.now();
    return localDateTime.format(format);
  }
}
