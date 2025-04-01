package uk.gov.dwp.health.pip.identity.messaging;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;
import uk.gov.dwp.health.pip.identity.messaging.properties.UpdatePipCsIdentityEventProperties;

@Component
@AllArgsConstructor
@Slf4j
public class UpdatePipCsIdentityMessagePublisher {

  private final EventManager eventManager;
  private final UpdatePipCsIdentityEventProperties properties;

  public void publishMessage(String applicationId, String idvStatus, String identityId) {
    log.info(
        " Attempting to publish update PIP CS IDV event {} for application id {}",
        identityId,
        applicationId);

    try {
      var updateIdentity =
          new UpdatePipCsIdentityEvent(
              properties.getTopicName(),
              applicationId,
              idvStatus,
              identityId,
              properties.getRoutingKeyIdentityResponse());
      eventManager.send(updateIdentity);
    } catch (Exception ex) {
      log.info("Error publishing update PIP CS IDV event: {}", ex.getMessage());
      throw new GenericRuntimeException(ex.getMessage());
    }
    log.info("Published update PIP CS IDV event to PIPCS-GW");
  }
}
