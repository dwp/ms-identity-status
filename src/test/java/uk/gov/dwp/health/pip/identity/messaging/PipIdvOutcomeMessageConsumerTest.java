package uk.gov.dwp.health.pip.identity.messaging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.ValidationException;
import uk.gov.dwp.health.pip.identity.messaging.properties.PipIdvOutcomeInboundEventProperties;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.service.IdentityService;
import uk.gov.dwp.health.pip.identity.service.impl.IdentityBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipIdvOutcomeMessageConsumerTest {
  private final UUID identityId = UUID.randomUUID();

  @Mock PipIdvOutcomeInboundEventProperties inboundEventProperties;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock IdentityService identityService;

  @Mock UpdatePipCsIdentityMessagePublisher updatePipCsIdentityMessagePublisher;
  @Mock MessageHeaders messageHeaders;
  @InjectMocks private PipIdvOutcomeMessageConsumer pipIdvOutcomeMessageConsumer;
  private Map<String, Object> payload;
  Logger idvOutcomeLogger = (Logger) LoggerFactory.getLogger(PipIdvOutcomeMessageConsumer.class);


  @BeforeEach
  void jsonRequestPayload() {
    payload = new HashMap<>();
    payload.put("channel", "oidv");
    payload.put("timestamp", getCurrentDate());
    payload.put("identity_id", identityId);
    payload.put("idv_outcome", "verified");
    payload.put("nino", "RN000003A");
    payload.put("subject_id", "positive@dwp.gov.uk");

    when(inboundEventProperties.getQueueNameIdentityResponse()).thenReturn("inboundQueue");
    when(inboundEventProperties.getRoutingKeyIdentityResponse()).thenReturn("routingKey");

    pipIdvOutcomeMessageConsumer =
        new PipIdvOutcomeMessageConsumer(
            inboundEventProperties,
            objectMapper,
            identityService,
            updatePipCsIdentityMessagePublisher);
  }

  @Test
  void handleMessageDoesNotThrowWithValidInput(){
    when(identityService.createIdentity(any())).thenAnswer(invocation->IdentityBuilder.createBuilder(invocation.getArgument(0)).build());
    assertDoesNotThrow(()->pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders,payload));
  }

  @Test
  void handleMessage_throws_exception_on_Channel_not_valid() {
    payload.put("channel", "test");
    assertThatThrownBy(() -> pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload))
        .isInstanceOf(ValidationException.class)
        .hasMessageStartingWith("Mandatory values are not supplied or not valid");
  }

  @Test
  void handleMessage_throws_exception_on_IdvOutcome_not_valid() {
    payload.put("idv_outcome", "test");
    assertThatThrownBy(() -> pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload))
        .isInstanceOf(ValidationException.class)
        .hasMessageStartingWith("Mandatory values are not supplied or not valid");
  }

  @Test
  void
      handleMessage_throws_exception_on_IdentityId_TimeStamp_Nino_SubjectId_IdvOutcome_Channel_null() {
    handleMessage_throws_exception_on_param_null("identity_id");
    handleMessage_throws_exception_on_param_null("timestamp");
    handleMessage_throws_exception_on_param_null("nino");
    handleMessage_throws_exception_on_param_null("subject_id");
    handleMessage_throws_exception_on_param_null("idv_outcome");
    handleMessage_throws_exception_on_param_null("channel");
  }

  @Test
  void handleMessage_acceptsNoVot(){
    when(identityService.createIdentity(any())).thenAnswer(invocation-> IdentityBuilder.createBuilder(invocation.getArgument(0)).build());
    assertDoesNotThrow(()->pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders,payload));
  }

  @Test
  void handleMessage_acceptsNullVot(){
    payload.put("vot",null);
    when(identityService.createIdentity(any())).thenAnswer(invocation-> IdentityBuilder.createBuilder(invocation.getArgument(0)).build());
    assertDoesNotThrow(()->pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders,payload));
  }

  @ParameterizedTest
  @ValueSource(strings = {"P0.Cl.Cm","P1.Cl.Cm","P2.Cl.Cm"})
  void handleMessage_acceptsValidVot(String votValue) {
    payload.put("vot",votValue);
    when(identityService.createIdentity(any())).thenAnswer(invocation-> IdentityBuilder.createBuilder(invocation.getArgument(0)).build());
    assertDoesNotThrow(()->pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders,payload));
  }

  @ParameterizedTest
  @ValueSource(strings = {"P0.Cl.Cn","P3.Cl.Cm","P4.Cl.Cm","P0.CL.Cm","P0.Dl.Cm","accepted","verified","unverified",""})
  void handleMessage_doesNotAcceptInvalidVot(String votValue) {
    payload.put("vot",votValue);
    assertThatThrownBy(() -> pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload))
            .isInstanceOf(ValidationException.class)
            .hasMessageStartingWith("Input values are not valid") //input is invalid
            .hasMessageContaining("uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1$Vot") //it is invalid due to vot field
            .hasMessageContaining(votValue); //it is invalid due to a disallowed value

 }
  @Test
  void handleMessage_warning_logged_when_conflict_exception_caught() {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    idvOutcomeLogger.addAppender(listAppender);

    when(identityService.createIdentity(any(IdentityRequestUpdateSchemaV1.class))).thenThrow(ConflictException.class);

    pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload);

    List<ILoggingEvent> logsList = listAppender.list.stream()
            .filter(l -> l.getLevel() == Level.WARN)
            .collect(Collectors.toList());
    assertThat(logsList.size()).isEqualTo(1);

    assertThat(logsList.get(0).getMessage()).isEqualTo("Conflict Exception thrown creating identity");
  }

  void handleMessage_throws_exception_on_param_null(String param) {
    payload.put(param, null);
    assertThatThrownBy(() -> pipIdvOutcomeMessageConsumer.handleMessage(messageHeaders, payload))
        .isInstanceOf(ValidationException.class)
        .hasMessageStartingWith("Mandatory values are not supplied or not valid");
  }

  private String getCurrentDate() {
    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    LocalDateTime localDateTime = LocalDateTime.now();
    return localDateTime.format(format);
  }
}
