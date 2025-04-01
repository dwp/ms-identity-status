package uk.gov.dwp.health.pip.identity.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;
import uk.gov.dwp.health.pip.identity.messaging.properties.PipIdvOutcomeOutboundEventProperties;
import uk.gov.dwp.health.pip.identity.model.TokenPayload;
import uk.gov.dwp.health.pip.identity.utils.DateParseUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class PipIdvOutcomeMessagePublisher {
  private final EventManager eventManager;
  private final PipIdvOutcomeOutboundEventProperties eventProperties;

  public void publish(final Identity identity, final TokenPayload tokenPayload) {
    log.info("Queuing up identity for app id lookup and push to PIPCS");

    HashMap<String, Object> payload = new HashMap<>();
    payload.put("channel", "oidv");
    payload.put("timestamp", DateParseUtil.dateTimeToString(LocalDateTime.now()));
    payload.put("nino", identity.getNino());
    payload.put("subject_id", identity.getSubjectId());
    payload.put("identity_id", UUID.randomUUID());
    payload.put("idv_outcome", "verified");
    if (tokenPayload.getVot() == null) {
      log.info("Building token for existing PIP cred user");
    } else {
      log.info("Building token for DTH user");
      payload.put("vot", tokenPayload.getVot());
    }

    PipIdentityGuidEvent pipIdentityGuidEvent =
        new PipIdentityGuidEvent(
            eventProperties.getTopicNameIdentityRequest(),
            eventProperties.getRoutingKeyIdentityRequest(), payload
        );
    try {
      eventManager.send(pipIdentityGuidEvent);
    } catch (Exception e) {
      log.error("Error publishing Pip Identity guid event: {}", e.getMessage());
      throw new GenericRuntimeException("Error publishing Pip Identity guid event");
    }
  }
}
