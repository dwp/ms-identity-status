package uk.gov.dwp.health.pip.identity.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.dwp.health.identity.status.openapi.api.V1Api;
import uk.gov.dwp.health.identity.status.openapi.model.IdvDto;
import uk.gov.dwp.health.pip.identity.service.IdentityService;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Controller
@RequiredArgsConstructor
public class IdentityController implements V1Api {

  private static final String EMAIL_REGEX = "(^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$)";
  private final IdentityService identityService;

  @Override
  public ResponseEntity<IdvDto> getIdentityByNino(String nino) {
    log.info("Request to get idv status for nino received");
    return identityService
        .getIdentityByNino(nino)
        .map(
            identity ->
                new ResponseEntity<>(
                    new IdvDto().idvStatus(IdvDto.IdvStatusEnum.fromValue(identity.getIdvStatus())),
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
        .map(
            identity ->
                new ResponseEntity<>(
                    new IdvDto().idvStatus(IdvDto.IdvStatusEnum.fromValue(identity.getIdvStatus())),
                    OK))
        .orElseGet(() -> new ResponseEntity<>(NOT_FOUND));
  }
}
