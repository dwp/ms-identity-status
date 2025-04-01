package uk.gov.dwp.health.pip.identity.messaging;

import org.hibernate.validator.internal.engine.messageinterpolation.parser.MessageDescriptorFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;
import uk.gov.dwp.health.pip.identity.messaging.properties.PipIdvOutcomeOutboundEventProperties;
import uk.gov.dwp.health.pip.identity.model.TokenPayload;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PipIdvOutcomeMessagePublisherTest {

  private PipIdvOutcomeOutboundEventProperties eventProperties = new PipIdvOutcomeOutboundEventProperties();
  @Mock private EventManager eventManager;

  private PipIdvOutcomeMessagePublisher messagePublisher;

  @Captor
  private ArgumentCaptor<PipIdentityGuidEvent> eventArgumentCaptor;

  @BeforeEach
  void setUp() {
    messagePublisher = new PipIdvOutcomeMessagePublisher(eventManager, eventProperties);
    eventProperties.setRoutingKeyIdentityRequest("routingKey");
    eventProperties.setTopicNameIdentityRequest("topic");
  }

  @Test
  public void testWithVot() {
    sendPipIdentityGuidEvent(TokenPayload.VotEnum.P2_CL_CM, "verified");
  }

  @Test
  public void testWithoutVot() {
    sendPipIdentityGuidEvent(null, "verified");
  }

  @Test
  public void testWithException() {
    Mockito.doThrow(new MessageDescriptorFormatException("")).when(eventManager).send(any(PipIdentityGuidEvent.class));
    try {
      sendPipIdentityGuidEvent(TokenPayload.VotEnum.P2_CL_CM, "verified");
      fail("Expected GRE");
    } catch (final GenericRuntimeException e) {
      assertEquals("Error publishing Pip Identity guid event", e.getMessage());
    }
  }

  private void sendPipIdentityGuidEvent(final TokenPayload.VotEnum vot, final String expectedIdvOutcome) {
    final TokenPayload payload = new TokenPayload();
    payload.setVot(vot);
    payload.setGuid("guid");
    payload.setSub("subject2");
    final UUID uuid = UUID.randomUUID();
    final LocalDateTime ldt = LocalDateTime.of(2001, 9, 11, 8, 30);
    final String errorMessage = null;
    final Identity identity = new Identity("id", "subject", uuid, ldt, "channel", null, "nino", "app",
        errorMessage, TokenPayload.VotEnum.P1_CL_CM.getValue());
    messagePublisher.publish(identity, payload);
    verify(eventManager, times(1)).send(eventArgumentCaptor.capture());
    final PipIdentityGuidEvent event = eventArgumentCaptor.getValue();
    assertNotNull(event);
    assertEquals("topic", event.getTopic());
    assertEquals("routingKey", event.getRoutingKey());
    assertEquals("oidv", event.getPayload().get("channel"));
    assertEquals("nino", event.getPayload().get("nino"));
    assertEquals("subject", event.getPayload().get("subject_id"));
    assertEquals(expectedIdvOutcome, event.getPayload().get("idv_outcome"));
    assertEquals(vot, event.getPayload().get("vot"), "VOT is taken from token not identity in DB");
  }

}
