package uk.gov.dwp.health.pip.identity.api;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.dwp.health.identity.status.openapi.api.V1Api;
import uk.gov.dwp.health.identity.status.openapi.model.ApplicationIdDto;
import uk.gov.dwp.health.identity.status.openapi.model.IdentityDto;
import uk.gov.dwp.health.identity.status.openapi.model.IdentityResponse;
import uk.gov.dwp.health.identity.status.openapi.model.IdvDto;
import uk.gov.dwp.health.pip.identity.model.IdentityResponseDto;
import uk.gov.dwp.health.pip.identity.service.IdentityRegistrationService;
import uk.gov.dwp.health.pip.identity.service.IdentityService;
import uk.gov.dwp.health.pip.identity.service.RegistrationsLimiterGetter;
import uk.gov.dwp.health.pip.identity.utils.IdentityStatusCalculator;
import uk.gov.dwp.health.pip.identity.utils.TokenUtils;
import uk.gov.dwp.health.identity.status.openapi.model.RegistrationsLimiterDto;

@Slf4j
@Controller
@RequiredArgsConstructor
public class IdentityController implements V1Api {

  private static final String EMAIL_REGEX = "(^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$)";
  private final IdentityService identityService;
  private final IdentityRegistrationService identityApiService;
  private final RegistrationsLimiterGetter registrationsLimiterGetter;

  @Override
  public ResponseEntity<IdvDto> getIdentityByNino(String nino) {
    log.info("Request to get idv status for nino received");
    return identityService
        .getIdentityByNino(nino)
        .map(IdentityStatusCalculator::fromIdentity)
        .map(
            value ->
                new ResponseEntity<>(
                    new IdvDto().idvStatus(IdvDto.IdvStatusEnum.fromValue(value)),
                    OK))
        .orElseGet(() -> {
          log.warn("No idv status found for nino");
          return new ResponseEntity<>(NOT_FOUND);
        });
  }

  @Override
  public ResponseEntity<IdvDto> getIdentityBySubjectId(String subjectId) {

    if (!subjectId.matches(EMAIL_REGEX)) {
      return new ResponseEntity<>(BAD_REQUEST);
    }
    return identityService
        .getIdentityBySubjectId(subjectId)
        .map(IdentityStatusCalculator::fromIdentity)
        .map(
            value ->
                new ResponseEntity<>(
                    new IdvDto().idvStatus(IdvDto.IdvStatusEnum.fromValue(value)),
                    OK))
        .orElseGet(() -> new ResponseEntity<>(NOT_FOUND));
  }

  @Override
  public ResponseEntity<IdentityDto> getIdentityByApplicationId(String applicationId) {
    log.info("Request to get Identity by application id received");

    return identityService
      .getIdentityByApplicationId(applicationId)
      .map(
        identity ->
          new ResponseEntity<>(
            new IdentityDto()
              .applicationId(identity.getApplicationID())
              .nino(identity.getNino())
              .subjectId(identity.getSubjectId()),
            OK))
      .orElseGet(() -> new ResponseEntity<>(NOT_FOUND));
  }

  @Override
  public ResponseEntity<IdentityResponse> register(String token, String channel) {
    log.debug("Encoded token: {} ", token);
    String payload = TokenUtils.decodePayload(token);
    log.debug("Decoded token: {} ", payload);
    IdentityResponseDto responseDto = identityApiService.register(payload, channel);
    if (responseDto.isCreated()) {
      return ResponseEntity.status(HttpStatus.CREATED).body(responseDto.getIdentityResponse());
    }
    return ResponseEntity.ok().body(responseDto.getIdentityResponse());
  }

  @Override
  public ResponseEntity<RegistrationsLimiterDto> getLimiter() {
    var registrationsLimiterDto = registrationsLimiterGetter.getRegistrationsLimiter();
    return ResponseEntity.status(HttpStatus.OK).body(registrationsLimiterDto);
  }

  @Override
  public ResponseEntity<Void> updateApplicationId(String identityId, 
                                                  ApplicationIdDto applicationIdDto) {
    identityService.updateApplicationId(identityId, applicationIdDto.getApplicationId());
    return ResponseEntity.accepted().build();
  }
}
