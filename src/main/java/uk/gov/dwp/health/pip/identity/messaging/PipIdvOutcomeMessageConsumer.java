package uk.gov.dwp.health.pip.identity.messaging;

import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.integration.message.consumers.HealthMessageConsumer;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.NoKeyChangesToExistingRecordException;
import uk.gov.dwp.health.pip.identity.messaging.properties.PipIdvOutcomeInboundEventProperties;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.service.IdentityService;
import uk.gov.dwp.health.pip.identity.utils.IdentityStatusCalculator;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipIdvOutcomeMessageConsumer
    implements HealthMessageConsumer<IdentityRequestUpdateSchemaV1> {

  private final PipIdvOutcomeInboundEventProperties inboundEventProperties;
  private final Validator validator;
  private final IdentityService identityService;
  private final UpdatePipCsIdentityMessagePublisher updatePipCsIdentityMessagePublisher;

  @Override
  public String getQueueName() {
    return inboundEventProperties.getQueueNameIdentityResponse();
  }

  @Override
  public void handleMessage(MessageHeaders messageHeaders, IdentityRequestUpdateSchemaV1 payload) {
    log.info("Message consumed");
    Set<ConstraintViolation<IdentityRequestUpdateSchemaV1>> violations =
        validator.validate(payload);
    if (!violations.isEmpty()) {
      log.error("Invalid payload {}", violations);
      String violationProps = getViolationProps(violations);
      throw new ConstraintViolationException(
          violationProps + " values are not supplied or not valid", violations);
    }

    try {
      var identity = identityService.createIdentity(payload);
      if (StringUtils.isEmpty(identity.getErrorMessage()) && isIdentityVerified(payload)) {
        updatePipCsIdentityMessagePublisher.publishMessage(
            identity.getApplicationID(),
            IdentityStatusCalculator.fromIdentity(identity),
            identity.getIdentityId().toString());
        log.debug(" Message publish completed. ");
      }
    } catch (NoKeyChangesToExistingRecordException ex) {
      log.warn("No key changes to existing record detected");
    } catch (ConflictException e) {
      log.warn("Conflict Exception thrown creating identity");
    }
    log.info("Message successfully processed");
  }

  private boolean isIdentityVerified(IdentityRequestUpdateSchemaV1 payload) {
    return payload.getVot() == IdentityRequestUpdateSchemaV1.Vot.P_2_CL_CM
        || payload.getIdvOutcome() == IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED;
  }

  private String getViolationProps(
      Set<ConstraintViolation<IdentityRequestUpdateSchemaV1>> violations) {
    return violations.stream()
        .map(ConstraintViolation::getPropertyPath)
        .map(Path::toString)
        .sorted()
        .collect(Collectors.joining(","));
  }
}
