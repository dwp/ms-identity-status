package uk.gov.dwp.health.pip.identity.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.exception.AccountNotFoundException;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.ValidationException;
import uk.gov.dwp.health.pip.identity.messaging.PipIdentityGuidEventPublisher;
import uk.gov.dwp.health.pip.identity.messaging.PipIdvOutcomeMessagePublisher;
import uk.gov.dwp.health.pip.identity.model.AccountDetailsResponse;
import uk.gov.dwp.health.pip.identity.model.IdentifierDto;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.model.IdentityResponseDto;
import uk.gov.dwp.health.pip.identity.model.TokenPayload;
import uk.gov.dwp.health.pip.identity.repository.IdentityRepository;
import uk.gov.dwp.health.pip.identity.repository.RegistrationRepository;
import uk.gov.dwp.health.pip.identity.service.IdentityRegistrationService;
import uk.gov.dwp.health.pip.identity.webclient.AccountManagerWebClient;
import uk.gov.dwp.health.pip.identity.webclient.GuidServiceClient;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.dwp.health.pip.identity.utils.IdentityStatusCalculator.UNVERIFIED;
import static uk.gov.dwp.health.pip.identity.utils.IdentityStatusCalculator.VERIFIED;

@ExtendWith(MockitoExtension.class)
class IdentityRegistrationServiceTest {
  private static final String EMAIL = "test.user@dwp.gov.uk";
  private static final String NINO = "RN000000A";
  private static final String OIDV = IdentityRequestUpdateSchemaV1.Channel.OIDV.value();

  @Mock private IdentityRepository repository;
  @Mock private AccountManagerWebClient accountManagerWebClient;
  @Mock private PipIdentityGuidEventPublisher guidEventPublisher;
  @Mock private PipIdvOutcomeMessagePublisher applicationIdLookupPublisher;
  @Mock private RegistrationRepository registrationRepository;
  @Mock private GuidServiceClient guidServiceClient;

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
  private final ObjectMapper mapper = new ObjectMapper();
  IdentityRegistrationService identityApiService;

  @Captor ArgumentCaptor<Identity> identityArgumentCaptor;

  @BeforeEach
  void setUp() {
    identityApiService =
        new IdentityRegistrationService(
            repository,
            accountManagerWebClient,
            mapper,
            validator,
            guidEventPublisher,
            applicationIdLookupPublisher,
            registrationRepository,
            guidServiceClient
        );
  }

  @Test
  void shouldCreateIdentityForValidToken() {
    String payload = "{\"sub\": \"" + EMAIL + "\", \"vot\": \"P0.Cl.Cm\", \"guid\": \"123123123\"}";
    when(repository.findBySubjectId(EMAIL)).thenReturn(Optional.empty());
    when(repository.save(any())).thenReturn(Identity.builder().id("1234567890").build());
    when(guidServiceClient.getNinoFromGuid(anyString())).thenReturn(IdentifierDto.builder().identifier(NINO).build());
    IdentityResponseDto identityResponseDto = identityApiService.register(payload, OIDV, false);

    verify(repository, times(1)).save(identityArgumentCaptor.capture());
    verify(registrationRepository, times(1)).incrementRegistrationCount();
    verify(guidEventPublisher, never()).publish(any());

    assertThat(identityArgumentCaptor)
        .satisfies(
            arg -> {
              assertThat(arg.getValue().getNino()).isEqualTo(NINO);
              assertThat(arg.getValue().getErrorMessage()).isNull();

              assertThat(arg.getValue().getChannel()).isEqualTo(OIDV);
              assertThat(arg.getValue().getIdvStatus()).isEqualTo(UNVERIFIED);
              assertThat(arg.getValue().getSubjectId()).isEqualTo(EMAIL);
              assertThat(arg.getValue().getDateTime()).isBeforeOrEqualTo(LocalDateTime.now());
            });
    assertThat(identityResponseDto.isCreated()).isTrue();
    assertThat(identityResponseDto)
        .extracting(IdentityResponseDto::getIdentityResponse)
        .satisfies(
            identityResponse -> {
              assertThat(identityResponse.getRef()).isEqualTo("1234567890");
              assertThat(identityResponse.getApplicationId()).isNull();
            });
    verify(guidServiceClient, times(1)).getNinoFromGuid(anyString());
  }

  @Test
  void shouldUpdateExistingSubjectRecordForValidToken() {
    String payload = "{\"sub\": \"" + EMAIL + "\", \"vot\": \"P0.Cl.Cm\", \"guid\": \"123123123\"}}";
    when(repository.findBySubjectId(EMAIL))
        .thenReturn(Optional.of(Identity.builder().subjectId(EMAIL).build()));
    when(repository.save(any())).thenReturn(Identity.builder().id("1234567890").build());
    when(guidServiceClient.getNinoFromGuid(anyString())).thenReturn(IdentifierDto.builder().identifier(NINO).build());

    IdentityResponseDto identityResponseDto = identityApiService.register(payload, "oidv", false);
    verify(repository, times(1)).save(identityArgumentCaptor.capture());
    verify(guidEventPublisher, never()).publish(any());
    assertThat(identityArgumentCaptor)
        .satisfies(
            arg -> {
              assertThat(arg.getValue().getNino()).isEqualTo(NINO);
              assertThat(arg.getValue().getErrorMessage()).isNull();

              assertThat(arg.getValue().getChannel()).isEqualTo("oidv");
              assertThat(arg.getValue().getIdvStatus()).isEqualTo(UNVERIFIED);
              assertThat(arg.getValue().getSubjectId()).isEqualTo(EMAIL);
              assertThat(arg.getValue().getDateTime()).isBeforeOrEqualTo(LocalDateTime.now());
            });
    assertThat(identityResponseDto.isCreated()).isFalse();
    assertThat(identityResponseDto)
        .extracting(IdentityResponseDto::getIdentityResponse)
        .satisfies(
            identityResponse -> {
              assertThat(identityResponse.getRef()).isEqualTo("1234567890");
              assertThat(identityResponse.getApplicationId()).isNull();
            });
    verify(guidServiceClient, times(1)).getNinoFromGuid(anyString());
  }

  @Test
  void shouldLookupNinoAndUpdatePipcsNewRecord() {
    String payload =
        "{\"sub\": \"" + EMAIL + "\", \"vot\": \"P2.Cl.Cm\", \"guid\": \"13f03f9da3a0f493e04df091865f8e77f63\"}";

    IdentityResponseDto identityResponseDto = identityApiService.register(payload, OIDV, true);

    verify(guidEventPublisher, times(1)).publish(any(TokenPayload.class));

    assertThat(identityResponseDto).isNull();
    verify(guidServiceClient, times(0)).getNinoFromGuid(anyString());
  }

  @Test
  void shouldNotPublishGuidEventNewRecordIfPublishFlagIsNull() {
    String payload = "{\"sub\": \"" + EMAIL + "\", \"vot\": \"P0.Cl.Cm\", \"guid\": \"123123123\"}}";
    when(repository.findBySubjectId(EMAIL))
        .thenReturn(Optional.of(Identity.builder().subjectId(EMAIL).build()));
    when(repository.save(identityArgumentCaptor.capture())).thenReturn(Identity.builder().id("1234567890").build());
    when(guidServiceClient.getNinoFromGuid(anyString())).thenReturn(IdentifierDto.builder().identifier(NINO).build());

    IdentityResponseDto identityResponseDto = identityApiService.register(payload, OIDV, null);

    verify(guidEventPublisher, times(0)).publish(any(TokenPayload.class));

    assertThat(identityResponseDto).isNotNull();
    verify(guidServiceClient, times(1)).getNinoFromGuid(anyString());
    assertThat(identityArgumentCaptor.getValue().getIdvStatus()).isEqualTo(UNVERIFIED);
  }

  @Test
  void shouldNotPublishGuidEventNewRecordIfPublishFlagIsFalse() {
    String payload = "{\"sub\": \"" + EMAIL + "\", \"vot\": \"P0.Cl.Cm\", \"guid\": \"123123123\"}}";
    when(repository.findBySubjectId(EMAIL))
        .thenReturn(Optional.of(Identity.builder().subjectId(EMAIL).build()));
    when(repository.save(any())).thenReturn(Identity.builder().id("1234567890").build());
    when(guidServiceClient.getNinoFromGuid(anyString())).thenReturn(IdentifierDto.builder().identifier(NINO).build());

    IdentityResponseDto identityResponseDto = identityApiService.register(payload, OIDV, false);

    verify(guidEventPublisher, times(0)).publish(any(TokenPayload.class));

    assertThat(identityResponseDto).isNotNull();
    verify(guidServiceClient, times(1)).getNinoFromGuid(anyString());
  }

  @Test
  void shouldLookupNinoAndUpdatePipcsIdentityHasNoNino() {
    String payload =
        "{\"sub\": \"" + EMAIL + "\", \"vot\": \"P0.Cl.Cm\", \"guid\": \"13f03f9da3a0f493e04df091865f8e77f63\"}";
    when(repository.findBySubjectId(EMAIL))
        .thenReturn(Optional.of(Identity.builder().id("1234").subjectId(EMAIL).build()));
    identityApiService.register(payload, "seeded", true);

    verify(guidEventPublisher, times(1)).publish(any(TokenPayload.class));
    verify(guidServiceClient, times(0)).getNinoFromGuid(anyString());
  }

  @Test
  void shouldNotPublishGuidEventIdentityHasNino() {
    String payload =
        "{\"sub\": \"" + EMAIL + "\", \"vot\": \"P0.Cl.Cm\", \"guid\": \"13f03f9da3a0f493e04df091865f8e77f63\"}";
    when(repository.findBySubjectId(EMAIL))
        .thenReturn(Optional.of(Identity.builder().id("1234").subjectId(EMAIL).nino("RN000004A").build()));
    identityApiService.register(payload, "seeded", true);

    verify(guidEventPublisher, never()).publish(any(TokenPayload.class));
    verify(guidServiceClient, times(0)).getNinoFromGuid(anyString());
  }

  @Test
  void shouldReturnValidationExceptionInvalidVot() {
    String payload = "{\"sub\": \"" + EMAIL + "\", \"vot\": \"P06.Cl.Cm\", \"guid\": \"123123123\"}}";
    assertThatThrownBy(() -> identityApiService.register(payload, OIDV, false))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Unable to parse the token");
  }

  @Test
  void shouldReturnValidationExceptionForValidPayload() {
    String payload = "{\"sub\": null, \"vot\": \"P0.Cl.Cm\", \"guid\": \"123123123\"}}";
    assertThatThrownBy(() -> identityApiService.register(payload, OIDV, false))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Invalid payload");
  }

  @Test
  void shouldReturnSubjectIdInResponseForValidCreation() {
    String payload = "{\"sub\": \"" + EMAIL + "\", \"vot\": \"P0.Cl.Cm\", \"guid\": \"123123123\"}}";
    when(repository.findBySubjectId(EMAIL)).thenReturn(Optional.empty());
    when(repository.save(any())).thenReturn(Identity.builder().id("1234567890")
        .subjectId(EMAIL).build());
    when(guidServiceClient.getNinoFromGuid(anyString())).thenReturn(IdentifierDto.builder().identifier(NINO).build());

    IdentityResponseDto identityResponseDto = identityApiService.register(payload, OIDV, false);

    verify(repository, times(1)).save(identityArgumentCaptor.capture());
    verify(guidEventPublisher, never()).publish(any());
    assertThat(identityArgumentCaptor)
        .satisfies(
            arg -> {
              assertThat(arg.getValue().getNino()).isEqualTo(NINO);
              assertThat(arg.getValue().getErrorMessage()).isNull();

              assertThat(arg.getValue().getChannel()).isEqualTo(OIDV);
              assertThat(arg.getValue().getSubjectId()).isEqualTo(EMAIL);
              assertThat(arg.getValue().getDateTime()).isBeforeOrEqualTo(LocalDateTime.now());
              assertThat(arg.getValue().getIdvStatus()).isEqualTo(UNVERIFIED);
            });
    assertThat(identityResponseDto.isCreated()).isTrue();
    assertThat(identityResponseDto)
        .extracting(IdentityResponseDto::getIdentityResponse)
        .satisfies(
            identityResponse -> {
              assertThat(identityResponse.getRef()).isEqualTo("1234567890");
              assertThat(identityResponse.getApplicationId()).isNull();
              assertThat(identityResponse.getSubjectId()).isEqualTo(EMAIL);
            });
    verify(guidServiceClient, times(1)).getNinoFromGuid(anyString());
  }

  @Test
  void shouldThrowExceptionForExistingAccount() {
    String payload = "{\"sub\": \"" + EMAIL + "\", \"vot\": \"P0.Cl.Cm\", \"guid\": \"123123123\"}}";
    when(accountManagerWebClient.getAccountDetailsFromEmail(EMAIL
    )).thenReturn(Optional.ofNullable(AccountDetailsResponse.of("1234")));

    verify(repository, never()).save(identityArgumentCaptor.capture());
    verify(guidEventPublisher, never()).publish(any());

    assertThrows(ConflictException.class, () -> identityApiService.register(payload, OIDV, false));
  }

  @Test
  void shouldThrowExceptionWhenAccountNotFoundExceptionThrown() {
    String payload = "{\"sub\": \"" + EMAIL + "\", \"vot\": \"P0.Cl.Cm\", \"guid\": \"123123123\"}}";
    when(accountManagerWebClient.getAccountDetailsFromEmail(EMAIL))
        .thenThrow(new AccountNotFoundException("Test"));
    when(guidServiceClient.getNinoFromGuid(anyString()))
        .thenReturn(IdentifierDto.builder().identifier(NINO).build());
    when(repository.save(any()))
        .thenReturn(Identity.builder().id("1234567890").subjectId(EMAIL).build());

    identityApiService.register(payload, OIDV, false);

    verify(repository, times(1)).save(identityArgumentCaptor.capture());
    verify(guidEventPublisher, never()).publish(any());
    verify(guidServiceClient, times(1)).getNinoFromGuid(anyString());
    assertThat(identityArgumentCaptor.getValue().getIdvStatus()).isEqualTo(UNVERIFIED);
  }

  @Test
  void shouldReturnIdentityWhenNoVotPassedAndIdentityExists() {
    String payload = "{\"sub\": \"" + EMAIL + "\" , \"guid\": \"123123123\"}}";
    when(repository.findBySubjectId(EMAIL))
        .thenReturn(Optional.of(Identity.builder()
            .subjectId(EMAIL).id("1234")
            .applicationID("4567")
            .build()));

    IdentityResponseDto identityResponseDto = identityApiService.register(payload, OIDV, false);

    verify(repository, never()).save(identityArgumentCaptor.capture());
    verify(guidEventPublisher, never()).publish(any());
    assertThat(identityResponseDto.isCreated()).isFalse();
    assertThat(identityResponseDto)
        .extracting(IdentityResponseDto::getIdentityResponse)
        .satisfies(
            identityResponse -> {
              assertThat(identityResponse.getRef()).isEqualTo("1234");
              assertThat(identityResponse.getApplicationId()).isEqualTo("4567");
              assertThat(identityResponse.getSubjectId()).isEqualTo(EMAIL);
            });
    verify(guidServiceClient, never()).getNinoFromGuid(anyString());
  }

  @Test
  void shouldThrowIdentityNotFoundExceptionIfNoVotAndNoIdentity() {
    String payload = "{\"sub\": \"" + EMAIL + "\" , \"guid\": \"123123123\"}}";
    when(repository.findBySubjectId(EMAIL))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> identityApiService.register(payload, OIDV, false))
        .hasMessage("No VOT in token and no Identity found for sub");

    verify(repository, never()).save(identityArgumentCaptor.capture());
    verify(guidEventPublisher, never()).publish(any());
  }

  @Test
  void shouldSuccessfullyUpdateWhenAnExistingMediumConfidenceRecordIsDowngraded() {
    UUID identityId = UUID.randomUUID();
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    when(repository.save(any())).thenReturn(Identity.builder().id("1234567890").build());

    Identity savedIdentity =
        new Identity(
            "id", EMAIL, identityId, dateTime, OIDV, null, "RN000004A", "test123", null,
            IdentityRequestUpdateSchemaV1.Vot.P_2_CL_CM.value());
    when(repository.findBySubjectId(EMAIL)).thenReturn(Optional.of(savedIdentity));

    String payload = "{\"sub\": \"" + EMAIL + "\", \"vot\": \"P0.Cl.Cm\", \"guid\": \"123123123\"}";
    identityApiService.register(payload, OIDV, false);

    verify(repository, times(1)).save(identityArgumentCaptor.capture());
    verify(guidEventPublisher, never()).publish(any());

    assertThat(identityArgumentCaptor)
        .satisfies(
            arg -> {
              assertThat(arg.getValue().getNino()).isEqualTo("RN000004A");
              assertThat(arg.getValue().getErrorMessage()).isNull();

              assertThat(arg.getValue().getChannel()).isEqualTo(OIDV);
              assertThat(arg.getValue().getIdvStatus()).isNull();
              assertThat(arg.getValue().getSubjectId()).isEqualTo(EMAIL);
              assertThat(arg.getValue().getDateTime()).isBeforeOrEqualTo(LocalDateTime.now());
              assertThat(arg.getValue().getVot()).isEqualTo(IdentityRequestUpdateSchemaV1.Vot.P_0_CL_CM.value());
            });
  }
}
