package uk.gov.dwp.health.pip.identity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.identity.status.openapi.model.IdentityResponse2;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.exception.AccountNotFoundException;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.IdentityNotFoundException;
import uk.gov.dwp.health.pip.identity.exception.ValidationException;
import uk.gov.dwp.health.pip.identity.messaging.PipIdentityGuidEventPublisher;
import uk.gov.dwp.health.pip.identity.messaging.PipIdvOutcomeMessagePublisher;
import uk.gov.dwp.health.pip.identity.model.AccountDetailsResponse;
import uk.gov.dwp.health.pip.identity.model.IdentifierDto;
import uk.gov.dwp.health.pip.identity.model.IdentityResponseDto;
import uk.gov.dwp.health.pip.identity.model.TokenPayload;
import uk.gov.dwp.health.pip.identity.repository.IdentityRepository;
import uk.gov.dwp.health.pip.identity.repository.RegistrationRepository;
import uk.gov.dwp.health.pip.identity.service.impl.IdentityBuilder;
import uk.gov.dwp.health.pip.identity.webclient.AccountManagerWebClient;
import uk.gov.dwp.health.pip.identity.webclient.GuidServiceClient;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.dwp.health.pip.identity.utils.IdentityStatusCalculator.UNVERIFIED;
import static uk.gov.dwp.health.pip.identity.utils.IdentityStatusCalculator.VERIFIED;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityRegistrationService {

  private final IdentityRepository repository;
  private final AccountManagerWebClient accountManagerWebClient;
  private final ObjectMapper objectMapper;
  private final Validator validator;
  // These two publishers route the data to pipcs via a nino lookup then an app id lookup
  private final PipIdentityGuidEventPublisher ninoLookupPublisher;
  private final PipIdvOutcomeMessagePublisher applicationIdLookupPublisher;
  private final RegistrationRepository registrationRepository;
  private final GuidServiceClient guidServiceClient;

  public IdentityResponseDto register(
      final String payload, final String channel, final Boolean publishObject
  ) {
    final boolean publish = publishObject != null && publishObject;
    final TokenPayload tokenPayload = parsePayload(payload);
    validate(tokenPayload);
    final String guid = tokenPayload.getGuid();
    if (isBlank(guid)) {
      missingGuidException();
    }
    final Optional<Identity> identityOptional = repository.findBySubjectId(tokenPayload.getSub());
    final boolean isNewIdentity = identityOptional.isEmpty();

    if (publish) {
      if (isNewIdentity || isBlank(identityOptional.get().getNino())) {
        log.info("GUID Present for token - publishing to SNS topic");
        ninoLookupPublisher.publish(tokenPayload);
      } else {
        applicationIdLookupPublisher.publish(identityOptional.get(), tokenPayload);
      }
      return null;
    } else {
      final var sub = tokenPayload.getSub();
      if (tokenPayload.getVot() == null) {
        if (isNewIdentity) {
          unexpectedMissingVotException();
        } else {
          return getIdentityResponseDto(identityOptional.get(), false);
        }
      }
      if (accountExistsForEmail(sub)) {
        unexpectedAccountRecordException();
      }
      String nino = null;
      if (isNewIdentity || isBlank(identityOptional.get().getNino())) {
        final IdentifierDto ninoFromLookup = guidServiceClient.getNinoFromGuid(guid);
        nino = ninoFromLookup.getIdentifier();
      }
      Identity identity;
      if (isNewIdentity) {
        identity = createIdentity(tokenPayload, channel, nino);
      } else {
        identity = identityOptional.get();
        if (isBlank(identity.getNino())) {
          identity.setNino(nino);
          if (identity.getIdvStatus() == null) {
            identity.setIdvStatus(UNVERIFIED);
          }
        }
        identity = updateIdentity(identity, tokenPayload, channel);
      }
      return getIdentityResponseDto(identity, isNewIdentity);
    }
  }

  private void validate(final TokenPayload tokenPayload) {
    final Set<ConstraintViolation<TokenPayload>> violations = validator.validate(tokenPayload);
    if (!violations.isEmpty()) {
      log.error("Invalid payload {}", violations);
      throw new ValidationException("Invalid payload");
    }
    if (isBlank(tokenPayload.getGuid())) {
      throw new IdentityNotFoundException("DTH has not provided a guid");
    }
  }

  private IdentityResponseDto getIdentityResponseDto(
      Identity identity, boolean isCreated) {
    IdentityResponse2 identityResponse =
        new IdentityResponse2()
            .ref(identity.getId())
            .applicationId(identity.getApplicationID())
            .subjectId(identity.getSubjectId())
            .nino(identity.getNino());

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

  private Identity updateIdentity(
      Identity identity, TokenPayload tokenPayload, String channel) {

    Identity.IdentityBuilder builder = IdentityBuilder.createBuilderFromIdentity(identity);
    builder.vot(tokenPayload.getVot() == null ? null : tokenPayload.getVot().getValue());
    builder.dateTime(LocalDateTime.now());
    builder.channel(channel);

    return repository.save(builder.build());
  }

  private Identity createIdentity(TokenPayload tokenPayload, String channel, String nino) {
    Identity identity =
        Identity.builder()
            .identityId(UUID.randomUUID())
            .vot(tokenPayload.getVot().getValue())
            .dateTime(LocalDateTime.now())
            .channel(channel)
            .subjectId(tokenPayload.getSub())
            .nino(nino)
            .idvStatus(UNVERIFIED)
            .build();

    Identity savedIdentity = repository.save(identity);

    incrementRegistrationCount();

    return savedIdentity;
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

  private static void missingGuidException() {
    log.error("No GUID in token from DTH.");
    throw new IdentityNotFoundException("No GUID in token from DTH.");
  }

  private static void unexpectedAccountRecordException() {
    log.error("PIP Account detected for DTH route. Throwing exception");
    throw new ConflictException("Account already exists for email");
  }

  private static void unexpectedMissingVotException() {
    log.error("No VOT in token for DTH route. Throwing exception");
    throw new IdentityNotFoundException("No VOT in token and no Identity found for sub");
  }

}
