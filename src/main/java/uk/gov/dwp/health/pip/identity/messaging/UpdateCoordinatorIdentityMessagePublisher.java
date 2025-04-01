package uk.gov.dwp.health.pip.identity.messaging;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;
import uk.gov.dwp.health.pip.identity.messaging.properties.UpdateCoordinatorIdentityEventProperties;

@Component
@AllArgsConstructor
@Slf4j
public class UpdateCoordinatorIdentityMessagePublisher {

  private final EventManager eventManager;
  private final UpdateCoordinatorIdentityEventProperties coordinatorOutboundProperties;

  public void publishMessage(String applicationId, String idvStatus, String identityId) {
    log.info(
        " Attempting to publish update COORDINATOR IDV event {} for application id {}",
        identityId,
        applicationId);

    try {
      var updateIdentity =
          new UpdateCoordinatorIdentityEvent(
              coordinatorOutboundProperties.getUpdateCoordinatorIdvTopicName(),
              applicationId,
              idvStatus,
              identityId,
              coordinatorOutboundProperties.getCoordinatorIdentityResponseRoutingKey());
      eventManager.send(updateIdentity);
    } catch (Exception ex) {
      log.info("Error publishing update COORDINATOR IDV event: {}", ex.getMessage());
      throw new GenericRuntimeException(ex.getMessage());
    }
    log.info("Published update COORDINATOR IDV event");
  }
}
