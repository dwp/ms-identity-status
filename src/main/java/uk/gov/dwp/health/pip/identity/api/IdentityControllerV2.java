package uk.gov.dwp.health.pip.identity.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.dwp.health.identity.status.openapi.api.V2Api;
import uk.gov.dwp.health.identity.status.openapi.model.IdentityResponse2;
import uk.gov.dwp.health.pip.identity.model.IdentityResponseDto;
import uk.gov.dwp.health.pip.identity.service.IdentityRegistrationService;
import uk.gov.dwp.health.pip.identity.utils.TokenUtils;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Controller
@RequiredArgsConstructor
public class IdentityControllerV2 implements V2Api {

  private final IdentityRegistrationService identityApiService;

  @Override
  public ResponseEntity<IdentityResponse2> registerV2(
      final String token, final String channel, final Boolean publish
  ) {
    log.debug("Encoded token: {} ", token);
    String payload = TokenUtils.decodePayload(token);
    log.debug("Decoded token: {} ", payload);
    IdentityResponseDto responseDto = identityApiService.register(payload, channel, publish);
    ResponseEntity<IdentityResponse2> result;
    final HttpStatus status = responseDto != null && responseDto.isCreated() ? CREATED : OK;
    final IdentityResponse2 body = responseDto == null ? null : responseDto.getIdentityResponse();
    result = ResponseEntity.status(status).body(body);
    log.info("register2 response {}", result.getStatusCode());
    return result;
  }

}
