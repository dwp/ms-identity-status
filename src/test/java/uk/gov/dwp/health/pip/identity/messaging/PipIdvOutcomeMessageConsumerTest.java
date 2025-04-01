package uk.gov.dwp.health.pip.identity.messaging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.NoKeyChangesToExistingRecordException;
import uk.gov.dwp.health.pip.identity.messaging.properties.PipIdvOutcomeInboundEventProperties;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.service.IdentityService;
import uk.gov.dwp.health.pip.identity.service.impl.IdentityBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipIdvOutcomeMessageConsumerTest {
  private final UUID identityId = UUID.randomUUID();

  @Mock PipIdvOutcomeInboundEventProperties inboundEventProperties;

  Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Mock IdentityService identityService;

  @Mock MessageHeaders messageHeaders;

  @Mock
  private IdvUpdateMessageDistributor idvUpdateMessageDistributor;
  @Mock
  private PipIdvOutcomeMessageConsumer pipIdvOutcomeMessageConsumer;
  private IdentityRequestUpdateSchemaV1 payload;
  Logger idvOutcomeLogger = (Logger) LoggerFactory.getLogger(PipIdvOutcomeMessageConsumer.class);

  @BeforeEach
  void jsonRequestPayload() {
    payload = new IdentityRequestUpdateSchemaV1();
    payload.setTimestamp(getCurrentDate());
    payload.setIdentityId(identityId);
    payload.setIdvOutcome(IdentityRequestUpdateSchemaV1.IdvOutcome.fromValue("verified"));
    payload.setNino("RN000003A");
    payload.setSubjectId("positive@dwp.gov.uk");
    payload.setChannel(IdentityRequestUpdateSchemaV1.Channel.fromValue("oidv"));

    pipIdvOutcomeMessageConsumer =
        new PipIdvOutcomeMessageConsumer(
            inboundEventProperties,
            validator,
            identityService,
            idvUpdateMessageDistributor
        );
  }

  @Test
  @DisplayName("Should return valid configuration")
  void shouldReturnProperConfig() {
    when(inboundEventProperties.getQueueNameIdentityResponse()).thenReturn("idv_outcome");
    assertThat(pipIdvOutcomeMessageConsumer.getQueueName()).isEqualTo("idv_outcome");
  }

  @Test
  @DisplayName("Should not throw any exception for valid input")
  void handleMessageDoesNotThrowWithValidInput() {
    when(identityService.recordUpliftedIdentity(any()))
        .thenAnswer(invocation -> IdentityBuilder.createBuilder(invocation.getArgument(0)).build());
    assertDoesNotThrow(() -> pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload));
  }

  @Test
  @DisplayName("Should not throw exception for valid vot values")
  void handleMessageDoesNotThrowWithValidVotInput() {
    payload = new IdentityRequestUpdateSchemaV1();
    payload.setTimestamp(getCurrentDate());
    payload.setIdentityId(identityId);
    payload.setVot(IdentityRequestUpdateSchemaV1.Vot.P_2_CL_CM);
    payload.setNino("RN000003A");
    payload.setSubjectId("positive@dwp.gov.uk");
    payload.setChannel(IdentityRequestUpdateSchemaV1.Channel.fromValue("oidv"));

    when(identityService.recordUpliftedIdentity(any()))
            .thenAnswer(invocation -> IdentityBuilder.createBuilder(invocation.getArgument(0)).build());
    assertDoesNotThrow(() -> pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload));
  }

  @Test
  @DisplayName("Should throw exception if mandatory fields are null")
  void validateMandatoryFields() {
    IdentityRequestUpdateSchemaV1 payload = new IdentityRequestUpdateSchemaV1();
    assertThatThrownBy(() -> pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessage(
            "channel,identityId,nino,subjectId,timestamp values are not supplied or not valid");
  }

  @Test
  @DisplayName("Should throw exception if nino and subjectId has junk values")
  void shouldThrowExceptionIfNinoAndSubjectIdHasJunkValues() {
    IdentityRequestUpdateSchemaV1 payload = new IdentityRequestUpdateSchemaV1();
    payload.setTimestamp(getCurrentDate());
    payload.setIdentityId(identityId);
    payload.setChannel(IdentityRequestUpdateSchemaV1.Channel.fromValue("oidv"));
    payload.setNino("003A");
    payload.setSubjectId("positive");
    payload.setIdvOutcome(IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED);
    assertThatThrownBy(() -> pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessage("nino,subjectId values are not supplied or not valid");
  }

  @Test
  void handleMessage_warning_logged_when_conflict_exception_caught() {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    idvOutcomeLogger.addAppender(listAppender);

    when(identityService.recordUpliftedIdentity(any(IdentityRequestUpdateSchemaV1.class)))
        .thenThrow(ConflictException.class);

    pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload);

    List<ILoggingEvent> logsList =
        listAppender.list.stream()
            .filter(l -> l.getLevel() == Level.WARN)
            .collect(Collectors.toList());

    assertThat(logsList).hasSize(1);
    assertThat(logsList)
        .first()
        .extracting(ILoggingEvent::getMessage)
        .isEqualTo("Conflict Exception thrown creating identity");

    verify(idvUpdateMessageDistributor, never()).distribute(any(),any());
  }

  @Test
  void handleMessage_warning_logged_when_no_key_changes_detected() {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    idvOutcomeLogger.addAppender(listAppender);

    when(identityService.recordUpliftedIdentity(any(IdentityRequestUpdateSchemaV1.class)))
        .thenThrow(NoKeyChangesToExistingRecordException.class);

    pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload);

    List<ILoggingEvent> logsList =
        listAppender.list.stream()
            .filter(l -> l.getLevel() == Level.WARN)
            .collect(Collectors.toList());

    assertThat(logsList).hasSize(1);
    assertThat(logsList)
        .first()
        .extracting(ILoggingEvent::getMessage)
        .isEqualTo("No key changes to existing record detected");

    verify(idvUpdateMessageDistributor, never()).distribute(any(), any());
  }

  @Test
  void shouldTriggerMessageDistributorIfErrorIsNotEmpty() {
    when(identityService.recordUpliftedIdentity(any(IdentityRequestUpdateSchemaV1.class)))
        .thenReturn(Identity.builder()
            .idvStatus("verified")
            .applicationID(String.valueOf(UUID.randomUUID()))
            .identityId(identityId)
            .build());

    pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload);

    verify(idvUpdateMessageDistributor, times(1)).distribute(any(), any());
  }

  @Test
  void shouldNotTriggerMessageDistributorIfErrorIsNotEmpty() {

    when(identityService.recordUpliftedIdentity(any(IdentityRequestUpdateSchemaV1.class)))
        .thenReturn(Identity.builder().errorMessage("Error").build());

    pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload);

    verify(idvUpdateMessageDistributor, never()).distribute(any(), any());
  }

  private String getCurrentDate() {
    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    LocalDateTime localDateTime = LocalDateTime.now();
    return localDateTime.format(format);
  }
}
