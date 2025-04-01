package uk.gov.dwp.health.pip.identity.messaging;

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
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.NoKeyChangesToExistingRecordException;
import uk.gov.dwp.health.pip.identity.messaging.properties.PipIdvOutcomeInboundEventProperties;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.service.IdentityService;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PipIdvOutcomeMessageConsumer
    implements HealthMessageConsumer<IdentityRequestUpdateSchemaV1> {

  private final PipIdvOutcomeInboundEventProperties inboundEventProperties;
  private final Validator validator;
  private final IdentityService identityService;
  private final IdvUpdateMessageDistributor idvUpdateMessageDistributor;

  @Override
  public String getQueueName() {
    return inboundEventProperties.getQueueNameIdentityResponse();
  }

  @Override
  public void handleMessage(MessageHeaders messageHeaders, IdentityRequestUpdateSchemaV1 payload) {
    log.info("IDV MATCHER MESSAGE CONSUMED");
    Set<ConstraintViolation<IdentityRequestUpdateSchemaV1>> violations =
        validator.validate(payload);
    if (!violations.isEmpty()) {
      log.error("Invalid payload {}", violations);
      String violationProps = getViolationProps(violations);
      throw new ConstraintViolationException(
          violationProps + " values are not supplied or not valid", violations);
    }

    try {
      final Identity identity = identityService.recordUpliftedIdentity(payload);
      final boolean noErrorMessage = StringUtils.isEmpty(identity.getErrorMessage());
      if (noErrorMessage) {
        idvUpdateMessageDistributor.distribute(payload, identity);
      }
    } catch (NoKeyChangesToExistingRecordException ex) {
      log.warn("No key changes to existing record detected");
    } catch (ConflictException e) {
      log.warn("Conflict Exception thrown creating identity");
    }
    log.info("IDV MATCHER MESSAGE SUCCESSFULLY PROCESSED");
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
