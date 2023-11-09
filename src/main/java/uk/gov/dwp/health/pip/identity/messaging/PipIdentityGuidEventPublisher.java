package uk.gov.dwp.health.pip.identity.messaging;

import java.time.LocalDateTime;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;
import uk.gov.dwp.health.pip.identity.messaging.properties.PipIdentityGuidEventProperties;
import uk.gov.dwp.health.pip.identity.model.TokenPayload;
import uk.gov.dwp.health.pip.identity.utils.DateParseUtil;

@RequiredArgsConstructor
@Service
@Slf4j
public class PipIdentityGuidEventPublisher {
  private final EventManager eventManager;
  private final PipIdentityGuidEventProperties eventProperties;

  public void publish(TokenPayload tokenPayload, String correlationId) {
    log.info("Attempting to publish Pip Identity guid event");

    HashMap<String, Object> payload = new HashMap<>();
    payload.put("subject_id", tokenPayload.getSub());
    payload.put("channel", "oidv");
    payload.put("timestamp", DateParseUtil.dateTimeToString(LocalDateTime.now()));
    payload.put("identity_id", correlationId);
    payload.put("guid", tokenPayload.getGuid());
    if (tokenPayload.getVot() == null) {
      log.info("Building token for existing PIP cred user");
      payload.put("idv_outcome", "verified");
    } else {
      log.info("Building token for DTH user");
      payload.put("vot", tokenPayload.getVot());
    }

    PipIdentityGuidEvent pipIdentityGuidEvent =
        new PipIdentityGuidEvent(
            eventProperties.getTopic(), eventProperties.getRoutingKey(), payload);
    try {
      eventManager.send(pipIdentityGuidEvent);
    } catch (Exception e) {
      log.error("Error publishing Pip Identity guid event: {}", e.getMessage());
      throw new GenericRuntimeException("Error publishing Pip Identity guid event");
    }
  }
}
