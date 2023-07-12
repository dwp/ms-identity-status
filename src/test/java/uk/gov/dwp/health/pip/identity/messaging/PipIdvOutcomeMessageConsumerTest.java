package uk.gov.dwp.health.pip.identity.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;
import uk.gov.dwp.health.pip.identity.exception.ValidationException;
import uk.gov.dwp.health.pip.identity.messaging.properties.PipIdvOutcomeInboundEventProperties;
import uk.gov.dwp.health.pip.identity.service.IdentityService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipIdvOutcomeMessageConsumerTest {
  private final UUID identityId = UUID.randomUUID();

  @Mock PipIdvOutcomeInboundEventProperties inboundEventProperties;

  @Mock ObjectMapper objectMapper;

  @Mock IdentityService identityService;

  @Mock UpdatePipCsIdentityMessagePublisher updatePipCsIdentityMessagePublisher;
  @Mock MessageHeaders messageHeaders;
  @InjectMocks private PipIdvOutcomeMessageConsumer pipIdvOutcomeMessageConsumer;
  private Map<String, Object> payload;

  @BeforeEach
  void jsonRequestPayload() {
    payload = new HashMap<>();
    payload.put("channel", "oidv");
    payload.put("timestamp", getCurrentDate());
    payload.put("identity_id", identityId);
    payload.put("idv_outcome", "verified");
    payload.put("nino", "RN000003A");
    payload.put("subject_id", "positive@dwp.gov.uk");

    when(inboundEventProperties.getQueueNameIdentityResponse()).thenReturn("inboundQueue");
    when(inboundEventProperties.getRoutingKeyIdentityResponse()).thenReturn("routingKey");

    pipIdvOutcomeMessageConsumer =
        new PipIdvOutcomeMessageConsumer(
            inboundEventProperties,
            objectMapper,
            identityService,
            updatePipCsIdentityMessagePublisher);
  }

  @Test
  void handleMessage_throws_exception_on_Channel_not_valid() {
    payload.put("channel", "test");
    assertThatThrownBy(() -> pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload))
        .isInstanceOf(ValidationException.class)
        .hasMessageStartingWith("Mandatory values are not supplied or not valid");
  }

  @Test
  void handleMessage_throws_exception_on_IdvOutcome_not_valid() {
    payload.put("idv_outcome", "test");
    assertThatThrownBy(() -> pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload))
        .isInstanceOf(ValidationException.class)
        .hasMessageStartingWith("Mandatory values are not supplied or not valid");
  }

  @Test
  void
      handleMessage_throws_exception_on_IdentityId_TimeStamp_Nino_SubjectId_IdvOutcome_Channel_null() {
    handleMessage_throws_exception_on_param_null("identity_id");
    handleMessage_throws_exception_on_param_null("timestamp");
    handleMessage_throws_exception_on_param_null("nino");
    handleMessage_throws_exception_on_param_null("subject_id");
    handleMessage_throws_exception_on_param_null("idv_outcome");
    handleMessage_throws_exception_on_param_null("channel");
  }

  void handleMessage_throws_exception_on_param_null(String param) {
    payload.put(param, null);
    assertThatThrownBy(() -> pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload))
        .isInstanceOf(ValidationException.class)
        .hasMessageStartingWith("Mandatory values are not supplied or not valid");
  }

  private String getCurrentDate() {
    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    LocalDateTime localDateTime = LocalDateTime.now();
    return localDateTime.format(format);
  }
}
