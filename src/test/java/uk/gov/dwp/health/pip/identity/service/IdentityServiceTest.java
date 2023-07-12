package uk.gov.dwp.health.pip.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.dwp.health.pip.identity.config.web.ReactorWebClient;
import uk.gov.dwp.health.pip.identity.config.web.ReactorWebClientFactory;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.repository.IdentityRepository;
import uk.gov.dwp.health.pip.identity.service.impl.IdentityServiceImpl;
import uk.gov.dwp.health.pip.identity.utils.DateParseUtil;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {
  UUID identityId = UUID.randomUUID();
  @Mock IdentityRepository repository;

  @Mock ReactorWebClient webClient;

  @Mock ReactorWebClientFactory webClientFactory;

  @BeforeEach
  void setUp() {
    when(webClientFactory.getClient()).thenReturn(webClient);
  }

  @Test
  void createIdentity_successfullyCreates_whenApplicationManagerReturnsApplicationId() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    IdentityRequestUpdateSchemaV1.Channel channelEnum = IdentityRequestUpdateSchemaV1.Channel.OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED;

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("test@dwp.gov.uk");
    newIdentityRequest.setIdentityId(identityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setNino("RN000004A");

    Identity savedIdentity =
        new Identity(
            "id",
            "test@dwp.gov.uk",
            identityId,
            dateTime,
            channelEnum.toString(),
            idvOutcome.toString(),
            "RN000004A",
            "applicationId",
            "");

    when(repository.findByNino("RN000004A")).thenReturn(Optional.empty());
    when(repository.save(any(Identity.class))).thenReturn(savedIdentity);
    when(webClient.post("/v1/application/matcher", String.class, "{\"nino\":\"RN000004A\"}"))
        .thenReturn(Mono.just("{\"application_id\":\"applicationId\"}"));

    Identity actualResponse = service.createIdentity(newIdentityRequest);

    assertEquals(savedIdentity, actualResponse);
  }

  @Test
  void createIdentity_successfullyCreates_whenApplicationManagerReturnsNoApplicationId() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    IdentityRequestUpdateSchemaV1.Channel channelEnum = IdentityRequestUpdateSchemaV1.Channel.OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED;

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("test@dwp.gov.uk");
    newIdentityRequest.setIdentityId(identityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setNino("RN000004A");

    Identity savedIdentity =
        new Identity(
            "id",
            "test@dwp.gov.uk",
            identityId,
            dateTime,
            channelEnum.toString(),
            idvOutcome.toString(),
            "RN000004A",
            null,
            "Application ID not found for identity with id: " + identityId);

    when(repository.findByNino("RN000004A")).thenReturn(Optional.empty());
    when(repository.save(any(Identity.class))).thenReturn(savedIdentity);
    when(webClient.post("/v1/application/matcher", String.class, "{\"nino\":\"RN000004A\"}"))
        .thenReturn(Mono.just("{\"application_id\": null }"));

    Identity actualResponse = service.createIdentity(newIdentityRequest);

    assertEquals(savedIdentity, actualResponse);
  }

  @Test
  void createIdentity_successfullyCreates_whenApplicationIdReturnsNull() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    IdentityRequestUpdateSchemaV1.Channel channelEnum = IdentityRequestUpdateSchemaV1.Channel.OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED;

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("test@dwp.gov.uk");
    newIdentityRequest.setIdentityId(identityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setNino("RN000004A");

    Identity savedIdentity =
        new Identity(
            "id",
            "test@dwp.gov.uk",
            identityId,
            dateTime,
            channelEnum.toString(),
            idvOutcome.toString(),
            "RN000004A",
            null,
            "Application ID not found for identity with id: 531a6d93-3889-45d5-92cd-9d5bb78d1a89");

    when(repository.findByNino("RN000004A")).thenReturn(Optional.empty());
    when(repository.save(any(Identity.class))).thenReturn(savedIdentity);
    when(webClient.post("/v1/application/matcher", String.class, "{\"nino\":\"RN000004A\"}"))
        .thenReturn(Mono.just("{\"application_id\": null }"));

    service.createIdentity(newIdentityRequest);

    ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
    verify(repository, times(1)).save(captor.capture());
    assertNull(captor.getValue().getApplicationID());
    assertEquals(
        captor.getValue().getErrorMessage(),
        "Application ID not found for identity with id: " + identityId);
  }

  @Test
  void processForApplicationId_savesConflictErrorMessageToIdentity_whenApiResponseReturns409() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    IdentityRequestUpdateSchemaV1.Channel channelEnum = IdentityRequestUpdateSchemaV1.Channel.OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED;

    IdentityRequestUpdateSchemaV1 updateIdentityRequest = new IdentityRequestUpdateSchemaV1();
    updateIdentityRequest.setSubjectId("test@dwp.gov.uk");
    updateIdentityRequest.setIdentityId(identityId);
    updateIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    updateIdentityRequest.setChannel(channelEnum);
    updateIdentityRequest.setIdvOutcome(idvOutcome);
    updateIdentityRequest.setNino("RN000004A");

    when(webClient.post(anyString(), any(Class.class), anyString()))
        .thenReturn(
            Mono.error(new ConflictException("Conflict occurred while processing the request")));

    try {
      service.createIdentity(updateIdentityRequest);
    } catch (ConflictException e) {
      // ignore the exception, we are not testing it
    }

    ArgumentCaptor<Identity> identityCaptor = ArgumentCaptor.forClass(Identity.class);
    verify(repository).save(identityCaptor.capture());
    Identity savedIdentity = identityCaptor.getValue();

    assertTrue(
        savedIdentity
            .getErrorMessage()
            .contains(
                "Multiple application IDs found for a nino while processing the request for identity id"));
  }

  @Test
  void createIdentity_throwsException_whenApplicationManagerRuntimeErrors() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    IdentityRequestUpdateSchemaV1.Channel channelEnum = IdentityRequestUpdateSchemaV1.Channel.OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED;

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("test@dwp.gov.uk");
    newIdentityRequest.setIdentityId(identityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setNino("RN000004A");

    when(repository.findByNino("RN000004A")).thenReturn(Optional.empty());
    when(webClient.post("/v1/application/matcher", String.class, "{\"nino\":\"RN000004A\"}"))
        .thenThrow(new GenericRuntimeException("Server error: example server error"));

    assertThrows(GenericRuntimeException.class, () -> service.createIdentity(newIdentityRequest));
  }

  @Test
  void createIdentity_throwsException_whenApplicationManagerRuntimeErrors_on_update() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    IdentityRequestUpdateSchemaV1.Channel channelEnum = IdentityRequestUpdateSchemaV1.Channel.OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
            IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED;

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("test@dwp.gov.uk");
    newIdentityRequest.setIdentityId(identityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setNino("RN000004A");

    Identity savedIdentity =
            new Identity(
                    "id",
                    "test@dwp.gov.uk",
                    identityId,
                    dateTime,
                    channelEnum.toString(),
                    idvOutcome.toString(),
                    "RN000004A",
                    null,
                    "Application ID not found for identity with id: 531a6d93-3889-45d5-92cd-9d5bb78d1a89");

    when(repository.findByNino("RN000004A")).thenReturn(Optional.of(savedIdentity));
    when(webClient.post("/v1/application/matcher", String.class, "{\"nino\":\"RN000004A\"}"))
            .thenThrow(new GenericRuntimeException("Server error: example server error"));

    assertThrows(GenericRuntimeException.class, () -> service.createIdentity(newIdentityRequest));
  }

  @Test
  void createIdentity_successfullyCreates_whenApplicationManagerOtherException() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    IdentityRequestUpdateSchemaV1.Channel channelEnum = IdentityRequestUpdateSchemaV1.Channel.OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED;

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("test@dwp.gov.uk");
    newIdentityRequest.setIdentityId(identityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setNino("RN000004A");

    Identity savedIdentity =
        new Identity(
            "id",
            "test@dwp.gov.uk",
            identityId,
            dateTime,
            channelEnum.toString(),
            idvOutcome.toString(),
            "RN000004A",
            null,
            "Server error: example server error");

    when(repository.findByNino("RN000004A")).thenReturn(Optional.empty());
    when(repository.save(any(Identity.class))).thenReturn(savedIdentity);
    when(webClient.post("/v1/application/matcher", String.class, "{\"nino\":\"RN000004A\"}"))
        .thenThrow(new IllegalArgumentException("Server error: example server error"));

    Identity actualResponse = service.createIdentity(newIdentityRequest);

    assertEquals(savedIdentity, actualResponse);
  }

  @Test
  void createIdentity_doesNotthrowException_whenExistingNinoIsPresentAndApplicationIdIsNotPresent() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    IdentityRequestUpdateSchemaV1.Channel channelEnum = IdentityRequestUpdateSchemaV1.Channel.OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
            IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED;

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("test@dwp.gov.uk");
    newIdentityRequest.setIdentityId(identityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setNino("RN000004A");

    Identity savedIdentity =
            new Identity(
                    "id",
                    "subjectId",
                    identityId,
                    dateTime,
                    channelEnum.toString(),
                    idvOutcome.toString(),
                    "RN000004A",
                    null,
                    "Test123");
    when(repository.findByNino("RN000004A")).thenReturn(Optional.of(savedIdentity));
    assertDoesNotThrow(() -> service.createIdentity(newIdentityRequest));
  }

  @Test
  void createIdentity_doesNotThrowException_whenExistingSubjectIsPresentAndApplicationIDIsNull() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    IdentityRequestUpdateSchemaV1.Channel channelEnum = IdentityRequestUpdateSchemaV1.Channel.OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
            IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED;
    UUID newIdentityId = UUID.randomUUID();

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("subjectId");
    newIdentityRequest.setIdentityId(newIdentityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setNino("RN000005A");

    Identity savedIdentity =
            new Identity(
                    "id",
                    "subjectId",
                    identityId,
                    dateTime,
                    channelEnum.toString(),
                    idvOutcome.toString(),
                    "RN000004A",
                    null,
                    "Test123");
    when(repository.findBySubjectId("subjectId")).thenReturn(Optional.of(savedIdentity));
    when(webClient.post("/v1/application/matcher", String.class, "{\"nino\":\"RN000005A\"}"))
            .thenReturn(Mono.just("{\"application_id\":\"123\"}"));

    assertDoesNotThrow(() -> service.createIdentity(newIdentityRequest));

    ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
    verify(repository, times(1)).save(captor.capture());
    assertEquals(captor.getValue().getIdentityId(), newIdentityId);
    assertEquals(captor.getValue().getNino(), "RN000005A");
    assertEquals(captor.getValue().getApplicationID(), "123");
    assertEquals(captor.getValue().getErrorMessage(), "");
  }

  @Test
  void createIdentity_doesNotThrowException_whenExistingNinoIsPresentAndApplicationIDIsNull() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    IdentityRequestUpdateSchemaV1.Channel channelEnum = IdentityRequestUpdateSchemaV1.Channel.OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
            IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED;
    UUID newIdentityId = UUID.randomUUID();

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("subjectIdTwo");
    newIdentityRequest.setIdentityId(newIdentityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setNino("RN000004A");

    Identity savedIdentity =
            new Identity(
                    "id",
                    "subjectId",
                    identityId,
                    dateTime,
                    channelEnum.toString(),
                    idvOutcome.toString(),
                    "RN000004A",
                    null,
                    "Test123");
    when(repository.findByNino("RN000004A")).thenReturn(Optional.of(savedIdentity));
    when(webClient.post("/v1/application/matcher", String.class, "{\"nino\":\"RN000004A\"}"))
            .thenReturn(Mono.just("{\"application_id\":\"722\"}"));

    assertDoesNotThrow(() -> service.createIdentity(newIdentityRequest));

    ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
    verify(repository, times(1)).save(captor.capture());
    assertEquals(captor.getValue().getIdentityId(), newIdentityId);
    assertEquals(captor.getValue().getSubjectId(), "subjectIdTwo");
    assertEquals(captor.getValue().getApplicationID(), "722");
    assertEquals(captor.getValue().getErrorMessage(), "");
  }

  @Test
  void createIdentity_doesNotThrowException_whenExistingNinoIsPresentAndApplicationIDIsNotFound() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    IdentityRequestUpdateSchemaV1.Channel channelEnum = IdentityRequestUpdateSchemaV1.Channel.OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
            IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED;
    UUID newIdentityId = UUID.randomUUID();

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("test2@dwp.gov.uk");
    newIdentityRequest.setIdentityId(newIdentityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setNino("RN000004A");

    Identity savedIdentity =
            new Identity(
                    "id",
                    "test@dwp.gov.uk",
                    identityId,
                    dateTime,
                    channelEnum.toString(),
                    idvOutcome.toString(),
                    "RN000004A",
                    null,
                    "Test123");
    when(repository.findByNino("RN000004A")).thenReturn(Optional.of(savedIdentity));
    when(webClient.post("/v1/application/matcher", String.class, "{\"nino\":\"RN000004A\"}"))
            .thenReturn(Mono.just(""));

    assertDoesNotThrow(() -> service.createIdentity(newIdentityRequest));

    ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
    verify(repository, times(1)).save(captor.capture());
    assertEquals(captor.getValue().getIdentityId(), newIdentityId);
    assertEquals(captor.getValue().getSubjectId(), "test2@dwp.gov.uk");
    assertNull(captor.getValue().getApplicationID());
    assertEquals(captor.getValue().getErrorMessage(), "Application ID not found for identity with id: " +
            newIdentityId.toString());
  }

  @Test
  void getIdentityBySubjectId_returnsOptionalEmpty_whenNoIdentityFound() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);

    String subjectId = "test@dwp.gov.uk";
    when(repository.findBySubjectId(subjectId)).thenReturn(Optional.empty());

    assertEquals(Optional.empty(), service.getIdentityBySubjectId(subjectId));
  }

  @Test
  void getIdentityBySubjectId_returnsIdentity_whenSubjectIdMatches() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);

    String subjectId = "test@dwp.gov.uk";
    Identity savedIdentity =
        new Identity(
            "id",
            "test@test.com",
            identityId,
            LocalDateTime.now().minusMinutes(2),
            "oidv",
            "verified",
            "RN000004A",
            "applicationId",
            "");

    when(repository.findBySubjectId(subjectId)).thenReturn(Optional.of(savedIdentity));

    assertEquals(Optional.of(savedIdentity), service.getIdentityBySubjectId(subjectId));
  }

  @Test
  void getIdentityByNino_returnsOptionalEmpty_whenNoIdentityFound() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);

    String nino = "RN000004A";
    when(repository.findByNino(nino)).thenReturn(Optional.empty());

    assertEquals(Optional.empty(), service.getIdentityByNino(nino));
  }

  @Test
  void getIdentityByNino_returnsIdentity_whenNinoMatches() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClientFactory);

    String nino = "RN000004A";
    Identity savedIdentity =
        new Identity(
            "id",
            "test@dwp.gov.uk",
            identityId,
            LocalDateTime.now().minusMinutes(2),
            "oidv",
            "verified",
            "RN000004A",
            "applicationId",
            "");

    when(repository.findByNino(nino)).thenReturn(Optional.of(savedIdentity));

    assertEquals(Optional.of(savedIdentity), service.getIdentityByNino(nino));
  }
}
