package uk.gov.dwp.health.pip.identity.messaging;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.identity.messaging.properties.PipIdentityGuidEventProperties;
import uk.gov.dwp.health.pip.identity.model.TokenPayload;
import uk.gov.dwp.health.pip.identity.utils.DateParseUtil;

@ExtendWith(MockitoExtension.class)
class PipIdentityGuidEventPublisherTest {

  @Mock private PipIdentityGuidEventProperties eventProperties;
  @Mock private EventManager eventManager;

  private PipIdentityGuidEventPublisher guidEventPublisher;

  @Captor private ArgumentCaptor<PipIdentityGuidEvent> eventArgumentCaptor;

  @BeforeEach
  void setUp() {
    guidEventPublisher = new PipIdentityGuidEventPublisher(eventManager, eventProperties);
  }

  @Test
  void shouldSendEventWithProperPayload() {
    when(eventProperties.getTopic()).thenReturn("pip_identity_event_guid");
    when(eventProperties.getRoutingKey()).thenReturn("pip_identity_outbound_routing");

    String identityId = String.valueOf(UUID.randomUUID());
    LocalDateTime now = LocalDateTime.now();

    TokenPayload tokenPayload =
        TokenPayload.of(
            "test@dwp.gov.uk",
            TokenPayload.VotEnum.P2_CL_CM,
            "13f03f9da3a0f493e04df091865f8e77f63");

    guidEventPublisher.publish(tokenPayload, String.valueOf(identityId));

    verify(eventManager, atMostOnce()).send(eventArgumentCaptor.capture());
    assertThat(eventArgumentCaptor.getValue())
        .satisfies(
            pipIdentityGuidEvent -> {
              assertThat(pipIdentityGuidEvent.getTopic()).isEqualTo("pip_identity_event_guid");
              assertThat(pipIdentityGuidEvent.getRoutingKey())
                  .isEqualTo("pip_identity_outbound_routing");
              assertThat(pipIdentityGuidEvent.getPayload())
                  .contains(
                      entry("subject_id", "test@dwp.gov.uk"),
                      entry("channel", "oidv"),
                      entry("guid", "13f03f9da3a0f493e04df091865f8e77f63"),
                      entry("identity_id", identityId),
                      entry("vot", TokenPayload.VotEnum.P2_CL_CM));
              assertThat(
                      DateParseUtil.stringToDateTime(
                          (String) pipIdentityGuidEvent.getPayload().get("timestamp")))
                  .isBeforeOrEqualTo(LocalDateTime.now());
            });
  }

  @Test
  void shouldNotIncludeVotIfNotPresent() {
    when(eventProperties.getTopic()).thenReturn("pip_identity_event_guid");
    when(eventProperties.getRoutingKey()).thenReturn("pip_identity_outbound_routing");

    String identityId = String.valueOf(UUID.randomUUID());
    LocalDateTime now = LocalDateTime.now();

    TokenPayload tokenPayload =
        TokenPayload.of("test@dwp.gov.uk", null, "13f03f9da3a0f493e04df091865f8e77f63");

    guidEventPublisher.publish(tokenPayload, String.valueOf(identityId));

    verify(eventManager, atMostOnce()).send(eventArgumentCaptor.capture());
    assertThat(eventArgumentCaptor.getValue())
        .satisfies(
            pipIdentityGuidEvent -> {
              assertThat(pipIdentityGuidEvent.getTopic()).isEqualTo("pip_identity_event_guid");
              assertThat(pipIdentityGuidEvent.getRoutingKey())
                  .isEqualTo("pip_identity_outbound_routing");
              assertThat(pipIdentityGuidEvent.getPayload())
                  .contains(
                      entry("subject_id", "test@dwp.gov.uk"),
                      entry("channel", "oidv"),
                      entry("guid", "13f03f9da3a0f493e04df091865f8e77f63"),
                      entry("idv_outcome", "verified"),
                      entry("identity_id", identityId));
              assertThat(
                      DateParseUtil.stringToDateTime(
                          (String) pipIdentityGuidEvent.getPayload().get("timestamp")))
                  .isBeforeOrEqualTo(LocalDateTime.now());
            });
  }

  @Test
  void shouldThrowExceptionIfEventManagerThrowsException() {
    doThrow(RuntimeException.class).when(eventManager).send(any());
    assertThatThrownBy(() -> guidEventPublisher.publish(new TokenPayload(), "id"))
        .hasMessage("Error publishing Pip Identity guid event");
  }
}
