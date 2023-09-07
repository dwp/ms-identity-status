package uk.gov.dwp.health.pip.identity.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.validation.Validation;
import javax.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.identity.status.openapi.model.IdentityResponse;
import uk.gov.dwp.health.integration.message.Constants;
import uk.gov.dwp.health.logging.LoggerContext;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.ValidationException;
import uk.gov.dwp.health.pip.identity.messaging.PipIdentityGuidEventPublisher;
import uk.gov.dwp.health.pip.identity.model.IdentityResponseDto;
import uk.gov.dwp.health.pip.identity.model.TokenPayload;
import uk.gov.dwp.health.pip.identity.repository.IdentityRepository;
import uk.gov.dwp.health.pip.identity.service.IdentityRegistrationService;
import uk.gov.dwp.health.pip.identity.webclient.ApplicationManagerWebClient;

@ExtendWith(MockitoExtension.class)
class IdentityRegistrationServiceTest {
  UUID CORRELATION_ID = UUID.randomUUID();
  String EMAIL = "test.user@dwp.gov.uk";
  @Mock private IdentityRepository repository;
  @Mock private LoggerContext loggerContext;
  @Mock private ApplicationManagerWebClient webClient;
  @Mock private PipIdentityGuidEventPublisher guidEventPublisher;

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
  private final ObjectMapper mapper = new ObjectMapper();
  IdentityRegistrationService identityApiService;

  @Captor ArgumentCaptor<Identity> captor;

  @BeforeEach
  void setUp() {
    identityApiService =
        new IdentityRegistrationService(
            repository, webClient, mapper, validator, loggerContext, guidEventPublisher);
  }

  @Test
  void shouldCreateIdentityForValidToken() {
    String payload = "{\"sub\": \"test.user@dwp.gov.uk\", \"vot\": \"P0.Cl.Cm\"}";
    when(repository.findBySubjectId(EMAIL)).thenReturn(Optional.empty());
    when(repository.save(any())).thenReturn(Identity.builder().id("1234567890").build());
    when(loggerContext.get(Constants.CORRELATION_ID_LOG_KEY)).thenReturn(CORRELATION_ID.toString());

    IdentityResponseDto identityResponseDto = identityApiService.register(payload, "seeded");

    verify(repository, times(1)).save(captor.capture());
    verify(guidEventPublisher, never()).publish(any(), any());
    assertThat(captor)
        .satisfies(
            arg -> {
              assertThat(arg.getValue().getNino()).isNull();
              assertThat(arg.getValue().getErrorMessage()).isNull();
              assertThat(arg.getValue().getIdentityId()).isEqualTo(CORRELATION_ID);
              assertThat(arg.getValue().getChannel()).isEqualTo("seeded");
              assertThat(arg.getValue().getSubjectId()).isEqualTo("test.user@dwp.gov.uk");
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
  }

  @Test
  void shouldUpdateExistingSubjectRecordForValidToken() {
    String payload = "{\"sub\": \"test.user@dwp.gov.uk\", \"vot\": \"P0.Cl.Cm\"}";
    when(repository.findBySubjectId("test.user@dwp.gov.uk"))
        .thenReturn(Optional.of(Identity.builder().subjectId(EMAIL).build()));
    when(repository.save(any())).thenReturn(Identity.builder().id("1234567890").build());
    when(loggerContext.get(Constants.CORRELATION_ID_LOG_KEY)).thenReturn(CORRELATION_ID.toString());

    IdentityResponseDto identityResponseDto = identityApiService.register(payload, "oidv");
    verify(webClient, never()).getApplicationId(any());
    verify(repository, times(1)).save(captor.capture());
    verify(guidEventPublisher, never()).publish(any(), any());
    assertThat(captor)
        .satisfies(
            arg -> {
              assertThat(arg.getValue().getNino()).isNull();
              assertThat(arg.getValue().getErrorMessage()).isNull();
              assertThat(arg.getValue().getIdentityId()).isEqualTo(CORRELATION_ID);
              assertThat(arg.getValue().getChannel()).isEqualTo("oidv");
              assertThat(arg.getValue().getSubjectId()).isEqualTo("test.user@dwp.gov.uk");
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
  }

  @Test
  void shouldUpdateExistingSubjectRecordWithApplicationId() {
    String payload = "{\"sub\": \"test.user@dwp.gov.uk\", \"vot\": \"P0.Cl.Cm\"}";
    when(repository.findBySubjectId("test.user@dwp.gov.uk"))
        .thenReturn(Optional.of(Identity.builder().subjectId(EMAIL).nino("RN000004A").build()));
    when(repository.save(any()))
        .thenReturn(Identity.builder().id("1234567890").applicationID("0987654321").build());
    when(webClient.getApplicationId("RN000004A")).thenReturn(Optional.of("0987654321"));
    when(loggerContext.get(Constants.CORRELATION_ID_LOG_KEY)).thenReturn(CORRELATION_ID.toString());

    IdentityResponseDto identityResponseDto = identityApiService.register(payload, "oidv");

    verify(repository, times(1)).save(captor.capture());
    verify(guidEventPublisher, never()).publish(any(), any());
    assertThat(captor)
        .satisfies(
            arg -> {
              assertThat(arg.getValue().getNino()).isEqualTo("RN000004A");
              assertThat(arg.getValue().getErrorMessage()).isNull();
              assertThat(arg.getValue().getIdentityId()).isEqualTo(CORRELATION_ID);
              assertThat(arg.getValue().getChannel()).isEqualTo("oidv");
              assertThat(arg.getValue().getSubjectId()).isEqualTo("test.user@dwp.gov.uk");
              assertThat(arg.getValue().getApplicationID()).isEqualTo("0987654321");
              assertThat(arg.getValue().getDateTime()).isBeforeOrEqualTo(LocalDateTime.now());
            });
    assertThat(identityResponseDto.isCreated()).isFalse();
    assertThat(identityResponseDto)
        .extracting(IdentityResponseDto::getIdentityResponse)
        .satisfies(
            identityResponse -> {
              assertThat(identityResponse.getRef()).isEqualTo("1234567890");
              assertThat(identityResponse.getApplicationId()).isEqualTo("0987654321");
            });
  }

  @Test
  void shouldUpdateExistingSubjectRecordWithErrorMessageIfApplicationIdNotFound() {
    String payload = "{\"sub\": \"test.user@dwp.gov.uk\", \"vot\": \"P0.Cl.Cm\"}";
    when(repository.findBySubjectId("test.user@dwp.gov.uk"))
        .thenReturn(Optional.of(Identity.builder().subjectId(EMAIL).nino("RN000004A").build()));
    when(repository.save(any())).thenReturn(Identity.builder().id("1234567890").build());
    when(webClient.getApplicationId("RN000004A")).thenReturn(Optional.empty());
    when(loggerContext.get(Constants.CORRELATION_ID_LOG_KEY)).thenReturn(CORRELATION_ID.toString());

    IdentityResponseDto identityResponseDto = identityApiService.register(payload, "oidv");

    verify(repository, times(1)).save(captor.capture());
    verify(guidEventPublisher, never()).publish(any(), any());
    assertThat(captor)
        .satisfies(
            arg -> {
              assertThat(arg.getValue().getNino()).isEqualTo("RN000004A");
              assertThat(arg.getValue().getErrorMessage())
                  .isEqualTo("Application ID not found for identity with id: " + CORRELATION_ID);
              assertThat(arg.getValue().getIdentityId()).isEqualTo(CORRELATION_ID);
              assertThat(arg.getValue().getChannel()).isEqualTo("oidv");
              assertThat(arg.getValue().getSubjectId()).isEqualTo("test.user@dwp.gov.uk");
              assertThat(arg.getValue().getApplicationID()).isNull();
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
  }

  @Test
  void shouldUpdateExistingSubjectRecordWithErrorMessageIfWebClientThrowsError() {
    String payload = "{\"sub\": \"test.user@dwp.gov.uk\", \"vot\": \"P0.Cl.Cm\"}";
    when(repository.findBySubjectId("test.user@dwp.gov.uk"))
        .thenReturn(Optional.of(Identity.builder().subjectId(EMAIL).nino("RN000004A").build()));
    when(repository.save(any())).thenReturn(Identity.builder().id("1234567890").build());
    when(webClient.getApplicationId("RN000004A"))
        .thenThrow(new ConflictException("Conflict occurred while processing the request"));
    when(loggerContext.get(Constants.CORRELATION_ID_LOG_KEY)).thenReturn(CORRELATION_ID.toString());

    IdentityResponseDto identityResponse = identityApiService.register(payload, "oidv");

    verify(repository, times(1)).save(captor.capture());
    verify(guidEventPublisher, never()).publish(any(), any());
    assertThat(captor)
        .satisfies(
            arg -> {
              assertThat(arg.getValue().getNino()).isEqualTo("RN000004A");
              assertThat(arg.getValue().getErrorMessage())
                  .isEqualTo("Conflict occurred while processing the request");
              assertThat(arg.getValue().getIdentityId()).isEqualTo(CORRELATION_ID);
              assertThat(arg.getValue().getChannel()).isEqualTo("oidv");
              assertThat(arg.getValue().getSubjectId()).isEqualTo("test.user@dwp.gov.uk");
              assertThat(arg.getValue().getApplicationID()).isNull();
              assertThat(arg.getValue().getDateTime()).isBeforeOrEqualTo(LocalDateTime.now());
            });
    assertThat(identityResponse.isCreated()).isFalse();
    assertThat(identityResponse)
        .extracting(IdentityResponseDto::getIdentityResponse)
        .extracting(IdentityResponse::getRef)
        .isEqualTo("1234567890");
  }

  @Test
  void shouldPublishGuidEvent() {
    String payload =
        "{\"sub\": \"test.user@dwp.gov.uk\", \"vot\": \"P0.Cl.Cm\", \"guid\": \"13f03f9da3a0f493e04df091865f8e77f63\"}";
    when(loggerContext.get(Constants.CORRELATION_ID_LOG_KEY)).thenReturn(CORRELATION_ID.toString());

    IdentityResponseDto identityResponseDto = identityApiService.register(payload, "seeded");

    verify(guidEventPublisher, atMostOnce())
        .publish(any(TokenPayload.class), eq(CORRELATION_ID.toString()));

    assertThat(identityResponseDto).isNull();
  }

  @Test
  void shouldReturnValidationExceptionInValidVot() {
    String payload = "{\"sub\": \"test.user@dwp.gov.uk\", \"vot\": \"P06.Cl.Cm\"}";
    assertThatThrownBy(() -> identityApiService.register(payload, "seeded"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Unable to parse the token");
  }

  @Test
  void shouldReturnValidationExceptionForValidPayload() {
    String payload = "{\"sub\": null, \"vot\": \"P0.Cl.Cm\"}";
    assertThatThrownBy(() -> identityApiService.register(payload, "seeded"))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Invalid payload");
  }

    @Test
    void shouldReturnSubjectIdInResponseForValidCreation() {
        String payload = "{\"sub\": \"test.user@dwp.gov.uk\", \"vot\": \"P0.Cl.Cm\"}";
        when(repository.findBySubjectId(EMAIL)).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(Identity.builder().id("1234567890")
                .subjectId(EMAIL).build());
        when(loggerContext.get(Constants.CORRELATION_ID_LOG_KEY)).thenReturn(CORRELATION_ID.toString());

        IdentityResponseDto identityResponseDto = identityApiService.register(payload, "seeded");

        verify(repository, times(1)).save(captor.capture());
        verify(guidEventPublisher, never()).publish(any(), any());
        assertThat(captor)
                .satisfies(
                        arg -> {
                            assertThat(arg.getValue().getNino()).isNull();
                            assertThat(arg.getValue().getErrorMessage()).isNull();
                            assertThat(arg.getValue().getIdentityId()).isEqualTo(CORRELATION_ID);
                            assertThat(arg.getValue().getChannel()).isEqualTo("seeded");
                            assertThat(arg.getValue().getSubjectId()).isEqualTo("test.user@dwp.gov.uk");
                            assertThat(arg.getValue().getDateTime()).isBeforeOrEqualTo(LocalDateTime.now());
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
    }
}
