package uk.gov.dwp.health.pip.identity.messaging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipIdvOutcomeMessageConsumerTest {
  private final UUID identityId = UUID.randomUUID();

  @Mock PipIdvOutcomeInboundEventProperties inboundEventProperties;

  Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Mock IdentityService identityService;

  @Mock UpdatePipCsIdentityMessagePublisher updatePipCsIdentityMessagePublisher;
  @Mock MessageHeaders messageHeaders;
  @InjectMocks private PipIdvOutcomeMessageConsumer pipIdvOutcomeMessageConsumer;
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
            updatePipCsIdentityMessagePublisher);
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
    when(identityService.createIdentity(any()))
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

    when(identityService.createIdentity(any()))
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

    when(identityService.createIdentity(any(IdentityRequestUpdateSchemaV1.class)))
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

    verify(updatePipCsIdentityMessagePublisher, never()).publishMessage(any(), any(), any());
  }

  @Test
  void handleMessage_warning_logged_when_no_key_changes_detected() {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    idvOutcomeLogger.addAppender(listAppender);

    when(identityService.createIdentity(any(IdentityRequestUpdateSchemaV1.class)))
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

    verify(updatePipCsIdentityMessagePublisher, never()).publishMessage(any(), any(), any());
  }

  @Test
  void shouldNotPublishMessageIfErrorIsNotEmpty() {

    when(identityService.createIdentity(any(IdentityRequestUpdateSchemaV1.class)))
        .thenReturn(Identity.builder().errorMessage("Error").build());

    pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload);

    verify(updatePipCsIdentityMessagePublisher, never()).publishMessage(any(), any(), any());
  }

  @Test
  void shouldNotPublishMessageIfVotValueIsNotP2() {

    payload.setIdvOutcome(null);
    payload.setVot(IdentityRequestUpdateSchemaV1.Vot.P_0_CL_CM);

    when(identityService.createIdentity(any(IdentityRequestUpdateSchemaV1.class)))
        .thenReturn(Identity.builder().build());

    pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload);

    verify(updatePipCsIdentityMessagePublisher, never()).publishMessage(any(), any(), any());
  }

  @Test
  void shouldPublishMessageIfVotValueIsP2() {

    payload.setIdvOutcome(null);
    payload.setVot(IdentityRequestUpdateSchemaV1.Vot.P_2_CL_CM);

    when(identityService.createIdentity(any(IdentityRequestUpdateSchemaV1.class)))
            .thenReturn(
                    Identity.builder()
                            .vot("P2.Cl.Cm")
                            .nino("RN000001A")
                            .applicationID("5ed0d430716609122be7a4d8")
                            .identityId(identityId)
                            .build());

    pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload);

    verify(updatePipCsIdentityMessagePublisher, times(1))
            .publishMessage("5ed0d430716609122be7a4d8", "verified", String.valueOf(identityId));
  }

  @Test
  void shouldPublishMessageIfIDVOutComeValueIsVerified() {

    when(identityService.createIdentity(any(IdentityRequestUpdateSchemaV1.class)))
        .thenReturn(
            Identity.builder()
                .idvStatus("verified")
                .applicationID("5ed0d430716609122be7a4d8")
                .identityId(identityId)
                .build());

    pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload);

    verify(updatePipCsIdentityMessagePublisher, times(1))
        .publishMessage("5ed0d430716609122be7a4d8", "verified", String.valueOf(identityId));
  }

  private String getCurrentDate() {
    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    LocalDateTime localDateTime = LocalDateTime.now();
    return localDateTime.format(format);
  }
}
