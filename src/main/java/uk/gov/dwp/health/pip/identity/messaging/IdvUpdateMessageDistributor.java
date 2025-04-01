package uk.gov.dwp.health.pip.identity.messaging;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.service.CoordinatorService;
import uk.gov.dwp.health.pip.identity.utils.IdentityStatusCalculator;

@Component
@Slf4j
@RequiredArgsConstructor
public class IdvUpdateMessageDistributor {

  private final CoordinatorService coordinatorService;

  private final UpdatePipCsIdentityMessagePublisher updatePipCsIdentityMessagePublisher;

  private final UpdateCoordinatorIdentityMessagePublisher updateCoordinatorIdentityMessagePublisher;

  public void distribute(IdentityRequestUpdateSchemaV1 payload, Identity identity) {

    if (isIdentityVerified(payload) && isPipServiceApplication(identity)) {
      updateCoordinatorIdentityMessagePublisher.publishMessage(
          identity.getApplicationID(),
          IdentityStatusCalculator.fromIdentity(identity),
          identity.getIdentityId().toString());
      log.info("Update COORDINATOR IDV Message publish completed.");
    }

    if (isIdentityVerified(payload) && isPipcsApplication(identity)) {
      updatePipCsIdentityMessagePublisher.publishMessage(
          identity.getApplicationID(),
          IdentityStatusCalculator.fromIdentity(identity),
          identity.getIdentityId().toString());
      log.info("Update PIPCS IDV Message publish completed.");
    }
  }

  private boolean isIdentityVerified(IdentityRequestUpdateSchemaV1 payload) {
    return payload.getVot() == IdentityRequestUpdateSchemaV1.Vot.P_2_CL_CM
        || payload.getIdvOutcome() == IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED;
  }

  private boolean isPipcsApplication(final Identity identity) {
    return coordinatorService.isPipcsApplication(identity.getApplicationID());
  }

  private boolean isPipServiceApplication(final Identity identity) {
    return coordinatorService.isPipServiceApplication(identity.getApplicationID());
  }

}


