package uk.gov.dwp.health.pip.identity.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.dwp.health.integration.message.consumers.HealthMessageConsumer;
import uk.gov.dwp.health.pip.identity.exception.ValidationException;
import uk.gov.dwp.health.pip.identity.messaging.properties.PipIdvOutcomeInboundEventProperties;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.service.IdentityService;

import java.util.List;
import java.util.Map;

@Service
@Getter
@Slf4j
public class PipIdvOutcomeMessageConsumer implements HealthMessageConsumer<Map<String, Object>> {

  private final ObjectMapper objectMapper;
  private final String queueName;
  private final String routingKey;
  private final IdentityService identityService;
  private final UpdatePipCsIdentityMessagePublisher updatePipCsIdentityMessagePublisher;

  public PipIdvOutcomeMessageConsumer(
      PipIdvOutcomeInboundEventProperties inboundEventProperties,
      ObjectMapper objectMapper,
      IdentityService identityService,
      UpdatePipCsIdentityMessagePublisher updatePipCsIdentityMessagePublisher) {
    this.objectMapper = objectMapper;
    this.queueName = inboundEventProperties.getQueueNameIdentityResponse();
    this.routingKey = inboundEventProperties.getRoutingKeyIdentityResponse();
    this.identityService = identityService;
    this.updatePipCsIdentityMessagePublisher = updatePipCsIdentityMessagePublisher;
  }

  @Override
  public Integer getRetryCount() {
    return 0;
  }

  @Override
  public void handleMessage(MessageHeaders messageHeaders, Map<String, Object> payload) {
    log.info("Message consumed");

    if (validateInput(payload)) {
      IdentityRequestUpdateSchemaV1 updateIdvRequest;
      try {
        updateIdvRequest = objectMapper.convertValue(payload,
                IdentityRequestUpdateSchemaV1.class);
        log.debug("Received an identity create request message {}", updateIdvRequest.toString());
      } catch (Exception e) {
        throw new ValidationException("Input values are not valid " + e.getMessage());
      }

      var identity = identityService.createIdentity(updateIdvRequest);

      if (!StringUtils.hasText(identity.getErrorMessage())) {
        updatePipCsIdentityMessagePublisher.publishMessage(
                identity.getApplicationID(),
                identity.getIdvStatus(),
                identity.getIdentityId().toString());
        log.debug(" Message publish completed. ");
      }
    } else {
      throw new ValidationException("Mandatory values are not supplied or not valid");
    }

    log.info("Message successfully processed");
  }

  private boolean validateInput(Map<String, Object> payload) {

    List<String> params =
        List.of("subject_id", "identity_id", "timestamp", "nino", "channel", "idv_outcome");

    for (String param : params) {
      if (!isValidInput(payload, param)) {
        return false;
      }
    }

    try {
      // check valid enum value
      IdentityRequestUpdateSchemaV1.Channel.fromValue((String) payload.get("channel"));
    } catch (Exception e) {
      log.error("channel value not valid " + e.getMessage());
      return false;
    }

    try {
      // check valid enum value
      IdentityRequestUpdateSchemaV1.IdvOutcome.fromValue((String) payload.get("idv_outcome"));
    } catch (Exception e) {
      log.error("idv_outcome value not valid " + e.getMessage());
      return false;
    }

    return true;
  }

  private boolean isValidInput(Map<String, Object> payload, String param) {
    if (payload.get(param) == null || payload.get(param).toString().trim().isEmpty()) {
      log.error("required input value " + param + " cannot be blank");
      return false;
    }
    return true;
  }
}
