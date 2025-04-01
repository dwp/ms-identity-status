package uk.gov.dwp.health.pip.identity.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.dwp.health.identity.status.openapi.api.V1Api;
import uk.gov.dwp.health.identity.status.openapi.model.ApplicationIdDto;
import uk.gov.dwp.health.identity.status.openapi.model.GuidDto;
import uk.gov.dwp.health.identity.status.openapi.model.IdentityDto;
import uk.gov.dwp.health.identity.status.openapi.model.IdentityResponse;
import uk.gov.dwp.health.identity.status.openapi.model.IdvDto;
import uk.gov.dwp.health.identity.status.openapi.model.NinoDto;
import uk.gov.dwp.health.identity.status.openapi.model.RegistrationsLimiterDto;
import uk.gov.dwp.health.identity.status.openapi.model.UpliftDto;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.model.IdentityResponseDto;
import uk.gov.dwp.health.pip.identity.model.IdvAgentUpliftOutcome;
import uk.gov.dwp.health.pip.identity.service.GuidToNinoService;
import uk.gov.dwp.health.pip.identity.service.IdentityRegistrationService;
import uk.gov.dwp.health.pip.identity.service.IdentityService;
import uk.gov.dwp.health.pip.identity.service.NinoToGuidService;
import uk.gov.dwp.health.pip.identity.service.RegistrationsLimiterGetter;
import uk.gov.dwp.health.pip.identity.service.TokenToNinoService;
import uk.gov.dwp.health.pip.identity.utils.IdentityStatusCalculator;
import uk.gov.dwp.health.pip.identity.utils.TokenUtils;

import java.util.Optional;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.LOCKED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Controller
@RequiredArgsConstructor
public class IdentityController implements V1Api {

  private static final String EMAIL_REGEX = "(^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$)";
  private final IdentityService identityService;
  private final IdentityRegistrationService identityApiService;
  private final RegistrationsLimiterGetter registrationsLimiterGetter;
  private final TokenToNinoService tokenToNinoService;
  private final NinoToGuidService ninoToGuidService;
  private final GuidToNinoService guidToNinoService;

  @Override
  public ResponseEntity<IdvDto> getIdentityByNino(String nino) {
    log.info("Request to get idv status for nino received");
    return identityService.getIdentityByNino(nino)
        .map(IdentityStatusCalculator::fromIdentity)
        .map(value -> new ResponseEntity<>(
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
    final Optional<Identity> identityOptional = identityService.getIdentityBySubjectId(subjectId);
    return identityOptional
        .map(IdentityStatusCalculator::fromIdentity)
        .map(value -> new ResponseEntity<>(
                new IdvDto().idvStatus(IdvDto.IdvStatusEnum.fromValue(value)), OK
            ))
        .orElseGet(() -> new ResponseEntity<>(NOT_FOUND));
  }

  @Override
  public ResponseEntity<IdentityDto> getIdentityByApplicationId(String applicationId) {
    log.info("Request to get Identity by application id received");

    return identityService.getIdentityByApplicationId(applicationId).map(
            identity -> new ResponseEntity<>(
                new IdentityDto()
                    .applicationId(identity.getApplicationID())
                    .nino(identity.getNino())
                    .subjectId(identity.getSubjectId()), OK))
        .orElseGet(() -> new ResponseEntity<>(NOT_FOUND));
  }

  @Override
  public ResponseEntity<IdentityDto> getIdentityById(String id) {
    log.info("Request to get Identity by id received");

    return identityService.getIdentityById(id).map(
            identity -> new ResponseEntity<>(
                new IdentityDto()
                    .applicationId(identity.getApplicationID())
                    .nino(identity.getNino())
                    .subjectId(identity.getSubjectId()), OK))
        .orElseGet(() -> new ResponseEntity<>(NOT_FOUND));
  }

  @Override
  public ResponseEntity<IdentityResponse> register(
      final String token, final String channel, final Boolean publish
  ) {
    log.debug("Encoded token: {} ", token);
    String payload = TokenUtils.decodePayload(token);
    log.debug("Decoded token: {} ", payload);
    IdentityResponseDto responseDto = identityApiService.register(payload, channel, publish);

    ResponseEntity<IdentityResponse> result;
    final HttpStatus status = responseDto != null && responseDto.isCreated() ? CREATED : OK;
    final IdentityResponse body = responseDto == null ? null
        : new IdentityResponse()
        .ref(responseDto.getIdentityResponse().getRef())
        .applicationId(responseDto.getIdentityResponse().getApplicationId())
        .subjectId(responseDto.getIdentityResponse().getSubjectId());
    result = ResponseEntity.status(status).body(body);
    log.info("register response {}", result.getStatusCode());
    return result;
  }

  @Override
  public ResponseEntity<RegistrationsLimiterDto> getLimiter() {
    var registrationsLimiterDto = registrationsLimiterGetter.getRegistrationsLimiter();
    return ResponseEntity.status(OK).body(registrationsLimiterDto);
  }

  @Override
  public ResponseEntity<Void> updateApplicationId(String identityId,
      ApplicationIdDto applicationIdDto) {
    identityService.updateApplicationId(identityId, applicationIdDto.getApplicationId());
    return ResponseEntity.accepted().build();
  }

  @Override
  public ResponseEntity<Void> upliftIdentityStatusForApplicationId(String applicationId,
      UpliftDto upliftDto) {
    IdvAgentUpliftOutcome upliftIdentityStatus = identityService.upliftIdentityStatusByAgent(
        applicationId,
        upliftDto);

    return switch (upliftIdentityStatus) {
      case SUCCESS -> ResponseEntity.status(ACCEPTED).build();
      case ALREADY_MEDIUM_OR_VERIFIED -> ResponseEntity.status(LOCKED).build();
      case IDENTITY_NOT_FOUND -> ResponseEntity.status(NOT_FOUND).build();
    };
  }

  @Override
  public ResponseEntity<NinoDto> getNinoFromIdToken(String token) {
    log.debug("Encoded id token: {} ", token);
    String payload = TokenUtils.decodePayload(token);
    log.debug("Decoded id token: {} ", payload);
    NinoDto nino = tokenToNinoService.getNinoFromToken(payload);
    return ResponseEntity.ok(nino);
  }

  @Override
  public ResponseEntity<GuidDto> getGuidFromNino(String nino) {
    GuidDto guid = ninoToGuidService.getGuidFromNino(nino);
    return ResponseEntity.ok(guid);
  }

  @Override
  public ResponseEntity<NinoDto> getNinoFromGuid(String guid) {
    NinoDto nino = guidToNinoService.getNinoFromGuid(guid);
    return ResponseEntity.ok(nino);
  }

}
