package uk.gov.dwp.health.pip.identity.service;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.identity.status.openapi.model.IdentityResponse;
import uk.gov.dwp.health.integration.message.Constants;
import uk.gov.dwp.health.monitoring.logging.LoggerContext;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.exception.AccountNotFoundException;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.IdentityNotFoundException;
import uk.gov.dwp.health.pip.identity.exception.ValidationException;
import uk.gov.dwp.health.pip.identity.messaging.PipIdentityGuidEventPublisher;
import uk.gov.dwp.health.pip.identity.model.AccountDetailsResponse;
import uk.gov.dwp.health.pip.identity.model.IdentityResponseDto;
import uk.gov.dwp.health.pip.identity.model.TokenPayload;
import uk.gov.dwp.health.pip.identity.repository.IdentityRepository;
import uk.gov.dwp.health.pip.identity.repository.RegistrationRepository;
import uk.gov.dwp.health.pip.identity.service.impl.IdentityBuilder;
import uk.gov.dwp.health.pip.identity.webclient.AccountManagerWebClient;
import uk.gov.dwp.health.pip.identity.webclient.ApplicationManagerWebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityRegistrationService {

  private final IdentityRepository repository;
  private final ApplicationManagerWebClient applicationManagerWebClient;
  private final AccountManagerWebClient accountManagerWebClient;
  private final ObjectMapper objectMapper;
  private final Validator validator;
  private final LoggerContext loggerContext;
  private final PipIdentityGuidEventPublisher guidEventPublisher;
  private final RegistrationRepository registrationRepository;

  public IdentityResponseDto register(String payload, String channel) {

    TokenPayload tokenPayload = parsePayload(payload);
    Set<ConstraintViolation<TokenPayload>> violations = validator.validate(tokenPayload);

    if (!violations.isEmpty()) {
      log.error("Invalid payload {}", violations);
      throw new ValidationException("Invalid payload");
    }
    final Optional<Identity> subjectRecord = repository.findBySubjectId(tokenPayload.getSub());
    if (shouldPublishGuidEvent(tokenPayload.getGuid(), subjectRecord)) {
      log.info("GUID Present for token - publishing to SNS topic");
      guidEventPublisher.publish(tokenPayload, loggerContext.get(Constants.CORRELATION_ID_LOG_KEY));
      return null;
    } else {
      final var sub = tokenPayload.getSub();
      if (tokenPayload.getVot() == null && subjectRecord.isPresent()) {
        Identity identity = subjectRecord.get();
        return getIdentityResponseDto(
                identity.getId(), identity.getApplicationID(), identity.getSubjectId(), false);
      } else if (tokenPayload.getVot() == null) {
        log.error("No VOT in token for DTH route. Throwing exception");
        throw new IdentityNotFoundException("No VOT in token and no Identity found for sub");
      }

      if (accountExistsForEmail(sub)) {
        log.error("PIP Account detected for DTH route. Throwing exception");
        throw new ConflictException("Account already exists for email");
      }

      Pair<Boolean, Identity> identityPair =
          subjectRecord
              .map(identity -> Pair.of(false, updateSubjectRecord(identity, tokenPayload, channel)))
              .orElseGet(() -> Pair.of(true, create(tokenPayload, channel)));

      return getIdentityResponseDto(
              identityPair.getRight().getId(),
              identityPair.getRight().getApplicationID(),
              identityPair.getRight().getSubjectId(),
              identityPair.getLeft());
    }
  }

  private IdentityResponseDto getIdentityResponseDto(
      String ref, String applicationId, String sub, boolean isCreated) {
    IdentityResponse identityResponse =
        new IdentityResponse().ref(ref).applicationId(applicationId);

    identityResponse.setSubjectId(sub);

    return IdentityResponseDto.of(isCreated, identityResponse);
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

  private Identity updateSubjectRecord(
      Identity identity, TokenPayload tokenPayload, String channel) {

    Identity.IdentityBuilder builder = IdentityBuilder.createBuilderFromIdentity(identity);
    builder.vot(tokenPayload.getVot().getValue());
    builder.dateTime(LocalDateTime.now());
    builder.channel(channel);
    builder.identityId(UUID.fromString(loggerContext.get(Constants.CORRELATION_ID_LOG_KEY)));

    if (isBlank(identity.getApplicationID()) && isNotBlank(identity.getNino())) {
      try {
        Optional<Object> applicationId =
            applicationManagerWebClient.getApplicationId(identity.getNino());
        applicationId.ifPresentOrElse(
            id -> {
              builder.applicationID(String.valueOf(id));
              builder.errorMessage(null);
            },
            () ->
                builder.errorMessage(
                    "Application ID not found for identity with id: "
                        + loggerContext.get(Constants.CORRELATION_ID_LOG_KEY)));
      } catch (Exception e) {
        log.error("Unable to retrieve the application Id {}", e.getMessage());
        builder.errorMessage(e.getMessage());
      }
    }
    return repository.save(builder.build());
  }

  private Identity create(TokenPayload tokenPayload, String channel) {
    Identity identity =
        Identity.builder()
            .identityId(UUID.fromString(loggerContext.get(Constants.CORRELATION_ID_LOG_KEY)))
            .vot(tokenPayload.getVot().getValue())
            .dateTime(LocalDateTime.now())
            .channel(channel)
            .subjectId(tokenPayload.getSub())
            .build();

    Identity savedIdentity = repository.save(identity);

    incrementRegistrationCount();

    return savedIdentity;
  }

  private boolean shouldPublishGuidEvent(String guid, Optional<Identity> identity) {
    return isNotBlank(guid) && (identity.isEmpty() || isBlank(identity.get().getNino()));
  }

  private boolean accountExistsForEmail(String sub) {
    try {
      Optional<AccountDetailsResponse> accountDetailsFromEmail =
          accountManagerWebClient.getAccountDetailsFromEmail(sub);
      return accountDetailsFromEmail.isPresent();
    } catch (AccountNotFoundException e) {
      return false;
    }
  }

  private void incrementRegistrationCount() {
    log.info("About to increment registration count");

    registrationRepository.incrementRegistrationCount();

    log.info("Incremented registration count");
  }
}
