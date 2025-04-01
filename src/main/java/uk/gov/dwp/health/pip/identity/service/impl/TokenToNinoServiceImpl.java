package uk.gov.dwp.health.pip.identity.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.identity.status.openapi.model.NinoDto;
import uk.gov.dwp.health.pip.identity.exception.ValidationException;
import uk.gov.dwp.health.pip.identity.model.IdentifierDto;
import uk.gov.dwp.health.pip.identity.model.TokenPayload;
import uk.gov.dwp.health.pip.identity.service.TokenToNinoService;
import uk.gov.dwp.health.pip.identity.webclient.GuidServiceClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenToNinoServiceImpl implements TokenToNinoService {

  private final ObjectMapper objectMapper;
  private final Validator validator;
  private final GuidServiceClient guidServiceClient;

  @Override
  public NinoDto getNinoFromToken(String token) {
    TokenPayload tokenPayload = parsePayload(token);
    Set<ConstraintViolation<TokenPayload>> violations = validator.validate(tokenPayload);

    if (!violations.isEmpty()) {
      log.error("Invalid payload {}", violations);
      throw new ValidationException("Invalid payload");
    }

    IdentifierDto identifierDto =
        Optional.ofNullable(tokenPayload.getGuid())
            .map(guidServiceClient::getNinoFromGuid)
            .orElseThrow(() -> new ValidationException("Guid not present"));
    return transFormIdentifierToNino(identifierDto);
  }

  private NinoDto transFormIdentifierToNino(IdentifierDto identifierDto) {
    NinoDto ninoDto = new NinoDto();
    ninoDto.setNino(identifierDto.getIdentifier());
    return ninoDto;
  }

  private TokenPayload parsePayload(String payload) {
    TokenPayload tokenPayload;
    try {
      tokenPayload = objectMapper.readValue(payload, TokenPayload.class);
    } catch (Exception e) {
      log.error("Unable to parse the token {}", e.getMessage());
      throw new ValidationException("Unable to parse the token");
    }
    return tokenPayload;
  }
}
