package uk.gov.dwp.health.pip.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.identity.status.openapi.model.UpliftDto;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;
import uk.gov.dwp.health.pip.identity.exception.IdentityNotFoundException;
import uk.gov.dwp.health.pip.identity.exception.NoKeyChangesToExistingRecordException;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1.Channel;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1.Vot;
import uk.gov.dwp.health.pip.identity.model.IdvAgentUpliftOutcome;
import uk.gov.dwp.health.pip.identity.repository.IdentityRepository;
import uk.gov.dwp.health.pip.identity.service.impl.IdentityServiceImpl;
import uk.gov.dwp.health.pip.identity.utils.DateParseUtil;
import uk.gov.dwp.health.pip.identity.webclient.ApplicationManagerWebClient;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1.Channel.OIDV;
import static uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1.IdvOutcome.UNVERIFIED;
import static uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1.IdvOutcome.VERIFIED;
import static uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1.Vot.P_0_CL_CM;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

  UUID identityId = UUID.randomUUID();
  @Mock
  IdentityRepository repository;

  @Mock
  ApplicationManagerWebClient webClient;

  IdentityServiceImpl service;

  @BeforeEach
  void beforeEach() {
    service = new IdentityServiceImpl(repository, webClient);
  }

  @Test
  @DisplayName("Successfully creates an identity when application manager returns application ID")
  void recordUpliftedIdentity_successfullyCreates_whenApplicationManagerReturnsApplicationId() {
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;

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
            "",
            null);

    when(repository.findByNino("RN000004A")).thenReturn(Optional.empty());
    when(repository.save(any(Identity.class))).thenReturn(savedIdentity);
    when(webClient.getApplicationId("RN000004A"))
        .thenReturn(Optional.of("applicationId"));
    when(webClient.getApplicationId("RN000004A")).thenReturn(Optional.of("applicationId"));

    Identity actualResponse = service.recordUpliftedIdentity(newIdentityRequest);
    assertEquals(savedIdentity, actualResponse);
  }

  @Test
  @DisplayName("Successfully creates when Application Manager returns no application ID")
  void recordUpliftedIdentity_successfullyCreates_whenApplicationManagerReturnsNoApplicationId() {
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;

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
            "Application ID not found for identity with id: " + identityId,
            null);

    when(repository.findByNino("RN000004A")).thenReturn(Optional.empty());
    when(repository.save(any(Identity.class))).thenReturn(savedIdentity);
    when(webClient.getApplicationId("RN000004A")).thenReturn(Optional.empty());

    Identity actualResponse = service.recordUpliftedIdentity(newIdentityRequest);
    assertEquals(savedIdentity, actualResponse);
  }

  @Test
  @DisplayName("Saves to the identity collection when application is not found")
  void recordUpliftedIdentity_successfullyCreates_whenApplicationIdReturnsNull() {
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;

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
            "Application ID not found for identity with id: 531a6d93-3889-45d5-92cd-9d5bb78d1a89",
            null);

    when(repository.findByNino("RN000004A")).thenReturn(Optional.empty());
    when(repository.save(any(Identity.class))).thenReturn(savedIdentity);
    when(webClient.getApplicationId("RN000004A")).thenReturn(Optional.empty());

    service.recordUpliftedIdentity(newIdentityRequest);
    ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
    verify(repository, times(1)).save(captor.capture());
    assertNull(captor.getValue().getApplicationID());
    assertEquals(
        captor.getValue().getErrorMessage(),
        "Application ID not found for identity with id: " + identityId);
  }

  @Test
  @DisplayName("Saves a Conflict error message to the collection when we receive a 409 error")
  void processForApplicationId_savesConflictErrorMessageToIdentity_whenApiResponseReturns409() {
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    IdentityRequestUpdateSchemaV1 updateIdentityRequest = new IdentityRequestUpdateSchemaV1();
    updateIdentityRequest.setSubjectId("test@dwp.gov.uk");
    updateIdentityRequest.setIdentityId(identityId);
    updateIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    updateIdentityRequest.setChannel(OIDV);
    updateIdentityRequest.setIdvOutcome(VERIFIED);
    updateIdentityRequest.setNino("RN000004A");

    when(webClient.getApplicationId("RN000004A"))
        .thenThrow(new ConflictException("Conflict occurred while processing the request"));

    try {
      service.recordUpliftedIdentity(updateIdentityRequest);
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
  @DisplayName(
      "Throws an exception on update when Application Manager receives a runtime error on create")
  void recordUpliftedIdentity_throwsException_whenApplicationManagerRuntimeErrors() {
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("test@dwp.gov.uk");
    newIdentityRequest.setIdentityId(identityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(OIDV);
    newIdentityRequest.setIdvOutcome(VERIFIED);
    newIdentityRequest.setNino("RN000004A");

    when(repository.findByNino("RN000004A")).thenReturn(Optional.empty());
    when(webClient.getApplicationId("RN000004A"))
        .thenThrow(new GenericRuntimeException("Server error: example server error"));

    assertThrows(GenericRuntimeException.class, () -> service.recordUpliftedIdentity(newIdentityRequest));
  }

  @Test
  @DisplayName(
      "Throws an exception on update when Application Manager receives a runtime error on update")
  void recordUpliftedIdentity_throwsException_whenApplicationManagerRuntimeErrors_on_update() {
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("test2@dwp.gov.uk");
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
            "Application ID not found for identity with id: 531a6d93-3889-45d5-92cd-9d5bb78d1a89",
            null);

    when(repository.findByNino("RN000004A")).thenReturn(Optional.of(savedIdentity));
    when(webClient.getApplicationId("RN000004A"))
        .thenThrow(new GenericRuntimeException("Server error: example server error"));

    assertThrows(GenericRuntimeException.class, () -> service.recordUpliftedIdentity(newIdentityRequest));
  }

  @Test
  @DisplayName(
      "Creates Identity when exception is thrown from App Manager (saves an error message)")
  void recordUpliftedIdentity_successfullyCreates_whenApplicationManagerOtherException() {
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;

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
            "Server error: example server error",
            null);

    when(repository.findByNino("RN000004A")).thenReturn(Optional.empty());
    when(repository.save(any(Identity.class))).thenReturn(savedIdentity);
    when(webClient.getApplicationId("RN000004A"))
        .thenThrow(new IllegalArgumentException("Server error: example server error"));

    Identity actualResponse = service.recordUpliftedIdentity(newIdentityRequest);
    assertEquals(savedIdentity, actualResponse);
  }

  @Test
  @DisplayName(
      "Should not throw exception when existing nino is present and application id is not present")
  void
  recordUpliftedIdentity_doesNotThrowException_whenExistingNinoIsPresentAndApplicationIdIsNotPresent() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClient);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;

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
            "Test123",
            null);
    when(repository.findByNino("RN000004A")).thenReturn(Optional.of(savedIdentity));
    assertDoesNotThrow(() -> service.recordUpliftedIdentity(newIdentityRequest));
  }

  @Test
  @DisplayName(
      "Should not throw exception when existing subject id is present and application id is null")
  void recordUpliftedIdentity_doesNotThrowException_whenExistingSubjectIsPresentAndApplicationIDIsNull() {
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;
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
            "Test123",
            null);
    when(repository.findBySubjectId("subjectId")).thenReturn(Optional.of(savedIdentity));
    when(webClient.getApplicationId("RN000005A")).thenReturn(Optional.of("123"));

    assertDoesNotThrow(() -> service.recordUpliftedIdentity(newIdentityRequest));

    ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
    verify(repository, times(1)).save(captor.capture());
    assertEquals(captor.getValue().getIdentityId(), newIdentityId);
    assertEquals(captor.getValue().getNino(), "RN000005A");
    assertEquals(captor.getValue().getApplicationID(), "123");
    assertEquals(captor.getValue().getErrorMessage(), "");
  }

  @Test
  @DisplayName("Should not throw exception when nino is present and application id is null")
  void recordUpliftedIdentity_doesNotThrowException_whenExistingNinoIsPresentAndApplicationIDIsNull() {
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;
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
            "Test123",
            null);
    when(repository.findByNino("RN000004A")).thenReturn(Optional.of(savedIdentity));
    when(webClient.getApplicationId("RN000004A")).thenReturn(Optional.of("722"));

    assertDoesNotThrow(() -> service.recordUpliftedIdentity(newIdentityRequest));

    ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
    verify(repository, times(1)).save(captor.capture());
    assertEquals(captor.getValue().getIdentityId(), newIdentityId);
    assertEquals(captor.getValue().getSubjectId(), "subjectIdTwo");
    assertEquals(captor.getValue().getApplicationID(), "722");
    assertEquals(captor.getValue().getErrorMessage(), "");
  }

  @Test
  @DisplayName(
      "Should not throw an exception when existing nino is present and application id is not found")
  void recordUpliftedIdentity_doesNotThrowException_whenExistingNinoIsPresentAndApplicationIDIsNotFound() {
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;
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
            "Test123",
            null);
    when(repository.findByNino("RN000004A")).thenReturn(Optional.of(savedIdentity));
    when(webClient.getApplicationId("RN000004A"))
        .thenReturn(Optional.empty());

    assertDoesNotThrow(() -> service.recordUpliftedIdentity(newIdentityRequest));

    ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);
    verify(repository, times(1)).save(captor.capture());
    assertEquals(captor.getValue().getIdentityId(), newIdentityId);
    assertEquals(captor.getValue().getSubjectId(), "test2@dwp.gov.uk");
    assertNull(captor.getValue().getApplicationID());
    assertEquals(captor.getValue().getErrorMessage(),
        "Application ID not found for identity with id: " +
        newIdentityId);
  }

  @Test
  @DisplayName(
      "Should not throw an exception when idvStatus has changed")
  void recordUpliftedIdentity_doesNotThrowException_whenIdvStatusHasChanged() {
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    UUID newIdentityId = UUID.randomUUID();
    final String nino = "RN000004A";
    final String subject = "test2@dwp.gov.uk";
    final Vot vot = P_0_CL_CM;
    final IdentityRequestUpdateSchemaV1.IdvOutcome existingIdvOutcome = UNVERIFIED;
    final IdentityRequestUpdateSchemaV1.IdvOutcome newIdvOutcome = VERIFIED;

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId(subject);
    newIdentityRequest.setIdentityId(newIdentityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(newIdvOutcome);
    newIdentityRequest.setVot(vot);
    newIdentityRequest.setNino(nino);

    Identity savedIdentity =
        new Identity(
            "id",
            subject,
            newIdentityId,
            dateTime,
            channelEnum.toString(),
            existingIdvOutcome.toString(),
            nino,
            null,
            "Test123",
            vot.toString());
    when(repository.findByNino(nino)).thenReturn(Optional.of(savedIdentity));
    when(webClient.getApplicationId(nino)).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> service.recordUpliftedIdentity(newIdentityRequest));
  }

  @Test
  @DisplayName(
      "Should throw an exception when key info has not changed")
  void recordUpliftedIdentity_doesNotThrowException_whenIdvStatusHasNotChanged() {
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    UUID newIdentityId = UUID.randomUUID();
    final String nino = "RN000004A";
    final String subject = "test2@dwp.gov.uk";
    final Vot vot = P_0_CL_CM;
    final IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome = VERIFIED;

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId(subject);
    newIdentityRequest.setIdentityId(newIdentityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setVot(vot);
    newIdentityRequest.setNino(nino);

    Identity savedIdentity =
        new Identity(
            "id",
            subject,
            newIdentityId,
            dateTime,
            channelEnum.toString(),
            idvOutcome.toString(),
            nino,
            null,
            "Test123",
            vot.toString());
    when(repository.findByNino(nino)).thenReturn(Optional.of(savedIdentity));
    assertThrows(NoKeyChangesToExistingRecordException.class, () -> service.recordUpliftedIdentity(newIdentityRequest));
  }

  @Test
  @DisplayName("Should return an empty optional when no record is found when retrieving by subject id")
  void getIdentityBySubjectId_returnsOptionalEmpty_whenNoIdentityFound() {

    String subjectId = "test@dwp.gov.uk";
    when(repository.findBySubjectId(subjectId)).thenReturn(Optional.empty());

    assertEquals(Optional.empty(), service.getIdentityBySubjectId(subjectId));
  }

  @Test
  @DisplayName("Should return an identity when matched by subject id")
  void getIdentityBySubjectId_returnsIdentity_whenSubjectIdMatches() {

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
            "",
            null);

    when(repository.findBySubjectId(subjectId)).thenReturn(Optional.of(savedIdentity));

    assertEquals(Optional.of(savedIdentity), service.getIdentityBySubjectId(subjectId));
  }

  @Test
  @DisplayName("Should return no an empty optional when no identity is found")
  void getIdentityByNino_returnsOptionalEmpty_whenNoIdentityFound() {

    String nino = "RN000004A";
    when(repository.findByNino(nino)).thenReturn(Optional.empty());

    assertEquals(Optional.empty(), service.getIdentityByNino(nino));
  }

  @Test
  @DisplayName("Returns the identity when matched by nino")
  void getIdentityByNino_returnsIdentity_whenNinoMatches() {

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
            "",
            null);

    when(repository.findByNino(nino)).thenReturn(Optional.of(savedIdentity));

    assertEquals(Optional.of(savedIdentity), service.getIdentityByNino(nino));
  }

  @Test
  @DisplayName("Should not throw an exception when email address is different")
  void getIdentityByNino_recordFound_whenEmailAddressIsDifferent_exceptionNotThrown() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClient);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;
    UUID newIdentityId = UUID.randomUUID();

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("subjectId2");
    newIdentityRequest.setIdentityId(newIdentityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setChannel(channelEnum);
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
            "Test123",
            null);
    when(repository.findByNino("RN000004A")).thenReturn(Optional.of(savedIdentity));

    assertDoesNotThrow(() -> service.recordUpliftedIdentity(newIdentityRequest));

    ArgumentCaptor<Identity> identityCaptor = ArgumentCaptor.forClass(Identity.class);
    verify(repository).save(identityCaptor.capture());
    Identity updatedIdentity = identityCaptor.getValue();
    assertEquals(updatedIdentity.getSubjectId(), "subjectId2");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"P0.Cl.Cm", "P1.Cl.Cm", ""})
  void getIdentityByNino_recordFound_whenVotIsDifferent_exceptionNotThrown(String value) {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClient);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;
    UUID newIdentityId = UUID.randomUUID();

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("subjectId");
    newIdentityRequest.setIdentityId(newIdentityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setNino("RN000004A");
    newIdentityRequest.setVot(IdentityRequestUpdateSchemaV1.Vot.P_2_CL_CM);

    Identity savedIdentity =
        new Identity(
            "id",
            "subjectId",
            identityId,
            dateTime,
            channelEnum.toString(),
            null,
            "RN000004A",
            null,
            "Test123",
            value);
    when(repository.findByNino("RN000004A")).thenReturn(Optional.of(savedIdentity));

    assertDoesNotThrow(() -> service.recordUpliftedIdentity(newIdentityRequest));

    ArgumentCaptor<Identity> identityCaptor = ArgumentCaptor.forClass(Identity.class);
    verify(repository).save(identityCaptor.capture());
    Identity updatedIdentity = identityCaptor.getValue();
    assertEquals(updatedIdentity.getVot(), IdentityRequestUpdateSchemaV1.Vot.P_2_CL_CM.toString());
  }

  @Test
  @DisplayName("Does not throw exception when upgrading to Medium Uplift")
  void updatesSuccessfullyOnUpgrade() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClient);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    UUID newIdentityId = UUID.randomUUID();

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("subjectId");
    newIdentityRequest.setIdentityId(newIdentityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setNino("RN000004A");
    newIdentityRequest.setVot(IdentityRequestUpdateSchemaV1.Vot.P_2_CL_CM);

    Identity savedIdentity =
        new Identity(
            "id",
            "subjectId",
            identityId,
            dateTime,
            channelEnum.toString(),
            null,
            "RN000004A",
            null,
            "Test123",
            "P0.Cl.Cm");
    when(repository.findByNino("RN000004A")).thenReturn(Optional.of(savedIdentity));

    assertDoesNotThrow(() -> service.recordUpliftedIdentity(newIdentityRequest));

    ArgumentCaptor<Identity> identityCaptor = ArgumentCaptor.forClass(Identity.class);
    verify(repository).save(identityCaptor.capture());
    Identity updatedIdentity = identityCaptor.getValue();
    assertEquals(updatedIdentity.getVot(), IdentityRequestUpdateSchemaV1.Vot.P_2_CL_CM.toString());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"RN000004A", ""})
  void getIdentityBySubjectId_recordFound_whenNinoIsDifferent_exceptionNotThrown(String value) {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClient);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;
    UUID newIdentityId = UUID.randomUUID();

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("subjectId");
    newIdentityRequest.setIdentityId(newIdentityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setNino("RN000007A");
    newIdentityRequest.setVot(P_0_CL_CM);

    Identity savedIdentity =
        new Identity(
            "id",
            "subjectId",
            identityId,
            dateTime,
            channelEnum.toString(),
            idvOutcome.toString(),
            value,
            null,
            "Test123",
            P_0_CL_CM.toString());
    when(repository.findByNino(any())).thenReturn(Optional.empty());
    when(repository.findBySubjectId("subjectId")).thenReturn(Optional.of(savedIdentity));

    assertDoesNotThrow(() -> service.recordUpliftedIdentity(newIdentityRequest));

    ArgumentCaptor<Identity> identityCaptor = ArgumentCaptor.forClass(Identity.class);
    verify(repository).save(identityCaptor.capture());
    Identity updatedIdentity = identityCaptor.getValue();
    assertEquals(updatedIdentity.getNino(), "RN000007A");
  }

  @Test
  @DisplayName("Returns identity when matched by Application ID")
  void getIdentityByApplicationId_returnsIdentity_whenAppIdMatches() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClient);

    var applicationId = "507f1f77bcf86cd799439011";
    Identity savedIdentity =
        new Identity(
            "id",
            "test@dwp.gov.uk",
            identityId,
            LocalDateTime.now().minusMinutes(2),
            "oidv",
            "verified",
            "RN000004A",
            applicationId,
            "",
            null);

    when(repository.findByApplicationID(applicationId)).thenReturn(Optional.of(savedIdentity));

    assertEquals(Optional.of(savedIdentity), service.getIdentityByApplicationId(applicationId));
  }

  @Test
  @DisplayName("Should not call Application Manager when Identity has existing Application ID")
  void getIdentityByNino_recordFound_whenIdentityHasApplicationId_applicationManagerNotCalled() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClient);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;
    UUID newIdentityId = UUID.randomUUID();

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("subjectId2");
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
            "507f1f77bcf86cd799439011",
            "Test123",
            null);
    when(repository.findByNino("RN000004A")).thenReturn(Optional.of(savedIdentity));

    assertDoesNotThrow(() -> service.recordUpliftedIdentity(newIdentityRequest));
    verify(webClient, never()).getApplicationId(any());
    ArgumentCaptor<Identity> identityCaptor = ArgumentCaptor.forClass(Identity.class);
    verify(repository).save(identityCaptor.capture());
    Identity updatedIdentity = identityCaptor.getValue();
    assertEquals(updatedIdentity.getApplicationID(), "507f1f77bcf86cd799439011");

  }

  @Test
  @DisplayName("Should call Application Manager when Identity has no existing Application ID")
  void getIdentityByNino_recordFound_whenIdentityHasNoApplicationId_applicationManagerCalled() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClient);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;
    UUID newIdentityId = UUID.randomUUID();

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("subjectId2");
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
            "Test123",
            null);
    when(repository.findByNino("RN000004A")).thenReturn(Optional.of(savedIdentity));
    when(webClient.getApplicationId("RN000004A"))
        .thenReturn(Optional.of("123"));

    assertDoesNotThrow(() -> service.recordUpliftedIdentity(newIdentityRequest));
    verify(webClient, times(1)).getApplicationId(any());

    ArgumentCaptor<Identity> identityCaptor = ArgumentCaptor.forClass(Identity.class);
    verify(repository).save(identityCaptor.capture());
    Identity updatedIdentity = identityCaptor.getValue();

    assertEquals(updatedIdentity.getApplicationID(), "123");
  }

  @Test
  @DisplayName("Should call Application Manager when Identity has has existing Application ID but Nino has changed")
  void getIdentityBySubjectId_recordFound_whenNinoIsDifferent_applicationManagerCalled() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClient);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;
    UUID newIdentityId = UUID.randomUUID();

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("subjectId");
    newIdentityRequest.setIdentityId(newIdentityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setNino("RN000007A");
    newIdentityRequest.setVot(P_0_CL_CM);

    Identity savedIdentity =
        new Identity(
            "id",
            "subjectId",
            identityId,
            dateTime,
            channelEnum.toString(),
            idvOutcome.toString(),
            "RN000004A",
            "507f1f77bcf86cd799439011",
            "Test123",
            P_0_CL_CM.toString());
    when(repository.findByNino(any())).thenReturn(Optional.empty());
    when(repository.findBySubjectId("subjectId")).thenReturn(Optional.of(savedIdentity));
    when(webClient.getApplicationId("RN000007A"))
        .thenReturn(Optional.of("123"));

    assertDoesNotThrow(() -> service.recordUpliftedIdentity(newIdentityRequest));

    ArgumentCaptor<Identity> identityCaptor = ArgumentCaptor.forClass(Identity.class);
    verify(webClient, times(1)).getApplicationId(any());
    verify(repository).save(identityCaptor.capture());
    Identity updatedIdentity = identityCaptor.getValue();

    assertEquals(updatedIdentity.getNino(), "RN000007A");
    assertEquals(updatedIdentity.getApplicationID(), "123");
  }

  @Test
  @DisplayName("Should not call Application Manager when Identity has existing Application ID")
  void getIdentityBySubjectId_recordFound_whenIdentityHasApplicationId_applicationManagerNotCalled() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClient);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;
    UUID newIdentityId = UUID.randomUUID();

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("subjectId");
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
            "507f1f77bcf86cd799439011",
            "Test123",
            P_0_CL_CM.toString());
    when(repository.findByNino("RN000004A")).thenReturn(Optional.of(savedIdentity));

    assertDoesNotThrow(() -> service.recordUpliftedIdentity(newIdentityRequest));
    verify(webClient, never()).getApplicationId(any());
    ArgumentCaptor<Identity> identityCaptor = ArgumentCaptor.forClass(Identity.class);
    verify(repository).save(identityCaptor.capture());
    Identity updatedIdentity = identityCaptor.getValue();
    assertEquals(updatedIdentity.getApplicationID(), "507f1f77bcf86cd799439011");

  }

  @Test
  @DisplayName("Updates application id for valid Identity")
  void updateApplicationId_returnsAccepted_whenIdentityIdFound() {
    var identity = Identity.builder()
        .id("identity-id")
        .build();

    when(repository.findById(anyString())).thenReturn(Optional.of(identity));
    assertDoesNotThrow(() -> service.updateApplicationId(identity.getId(), "application-id"));
    assertEquals(identity.getApplicationID(), "application-id");
  }

  @Test
  @DisplayName("Throw IdentityNotFoundException for invalid identityId")
  void updateApplicationId_throwsException_whenIdentityIdNotFound() {

    String IDENTITY_ID = "identity-id";
    when(repository.findById(anyString())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.updateApplicationId(IDENTITY_ID, "application-id"))
        .isInstanceOf(IdentityNotFoundException.class)
        .hasMessage(String.format("No identity found for given identity id %s", IDENTITY_ID));

    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(repository).findById(stringArgumentCaptor.capture());
    assertThat(stringArgumentCaptor.getValue()).isEqualTo(IDENTITY_ID);
  }

  @Test
  @DisplayName(
      "Should call Application Manager when Identity has has existing Application ID but Nino has changed"
      + " and if Nino returns no record then ApplicationId should be cleared from Identity.")
  void getIdentityBySubjectId_recordFound_whenNinoIsDifferent_applicationManagerCalledAndClearedWhenNoResult() {
    IdentityServiceImpl service = new IdentityServiceImpl(repository, webClient);
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);
    Channel channelEnum = OIDV;
    IdentityRequestUpdateSchemaV1.IdvOutcome idvOutcome =
        VERIFIED;
    UUID newIdentityId = UUID.randomUUID();

    IdentityRequestUpdateSchemaV1 newIdentityRequest = new IdentityRequestUpdateSchemaV1();
    newIdentityRequest.setSubjectId("subjectId");
    newIdentityRequest.setIdentityId(newIdentityId);
    newIdentityRequest.setTimestamp(DateParseUtil.dateTimeToString(dateTime));
    newIdentityRequest.setChannel(channelEnum);
    newIdentityRequest.setIdvOutcome(idvOutcome);
    newIdentityRequest.setNino("RN000007A");
    newIdentityRequest.setVot(P_0_CL_CM);

    Identity savedIdentity =
        new Identity(
            "id",
            "subjectId",
            identityId,
            dateTime,
            channelEnum.toString(),
            idvOutcome.toString(),
            "RN000004A",
            "507f1f77bcf86cd799439011",
            "Test123",
            P_0_CL_CM.toString());
    when(repository.findByNino(any())).thenReturn(Optional.empty());
    when(repository.findBySubjectId("subjectId")).thenReturn(Optional.of(savedIdentity));
    when(webClient.getApplicationId("RN000007A"))
        .thenReturn(Optional.empty());

    assertDoesNotThrow(() -> service.recordUpliftedIdentity(newIdentityRequest));

    ArgumentCaptor<Identity> identityCaptor = ArgumentCaptor.forClass(Identity.class);
    verify(webClient, times(1)).getApplicationId(any());
    verify(repository).save(identityCaptor.capture());
    Identity updatedIdentity = identityCaptor.getValue();

    assertEquals(updatedIdentity.getNino(), "RN000007A");
    assertNull(updatedIdentity.getApplicationID());
  }


  @Test
  void shouldReturnSuccessWhenUpliftingIdentityRecord() {
    UpliftDto upliftDetails = new UpliftDto();
    upliftDetails.setStaffId("123456788");
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    Identity savedIdentityWithoutVerification =
        new Identity(
            "id",
            "subjectId",
            identityId,
            dateTime,
            OIDV.toString(),
            UNVERIFIED.toString(),
            "RN000004A",
            "507f1f77bcf86cd799439011",
            null,
            P_0_CL_CM.toString());

    when(repository.findByApplicationID(eq("507f1f77bcf86cd799439011"))).thenReturn(
        Optional.of(savedIdentityWithoutVerification));

    ArgumentCaptor<Identity> captor = ArgumentCaptor.forClass(Identity.class);

    IdvAgentUpliftOutcome result = service.upliftIdentityStatusByAgent("507f1f77bcf86cd799439011",
        upliftDetails);

    verify(repository, times(1)).save(captor.capture());
    assertEquals(captor.getValue().getVot(), Vot.P_2_CL_CM.toString());
    assertEquals(captor.getValue().getIdvStatus(), VERIFIED.toString());
    assertEquals(captor.getValue().getUpliftDetails(), upliftDetails);

    assertEquals(result, IdvAgentUpliftOutcome.SUCCESS);
  }

  @Test
  void shouldReturnAlreadyMediumVerifiedWhenIdentityAlreadyMediumVot() {
    UpliftDto upliftDetails = new UpliftDto();
    upliftDetails.setStaffId("123456788");
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    Identity savedIdentityWithVot =
        new Identity(
            "57bc2bd3a084c918b8dfd0ee",
            "subjectId",
            identityId,
            dateTime,
            OIDV.toString(),
            VERIFIED.toString(),
            "RN000004A",
            "507f1f77bcf86cd799439011",
            null,
            Vot.P_2_CL_CM.toString());

    when(repository.findByApplicationID(eq("507f1f77bcf86cd799439011"))).thenReturn(
        Optional.of(savedIdentityWithVot));

    IdvAgentUpliftOutcome result = service.upliftIdentityStatusByAgent("507f1f77bcf86cd799439011",
        upliftDetails);

    assertEquals(IdvAgentUpliftOutcome.ALREADY_MEDIUM_OR_VERIFIED, result);
  }

  @Test
  void shouldReturnAlreadyMediumVerifiedWhenIdentityAlreadyMVerifiedNoVot() {
    UpliftDto upliftDetails = new UpliftDto();
    upliftDetails.setStaffId("123456788");
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    Identity savedIdentityWithNoVotButVerified =
        new Identity(
            "507f1f77bcf86cd799439011",
            "subjectId",
            identityId,
            dateTime,
            OIDV.toString(),
            VERIFIED.toString(),
            "RN000004A",
            "507f1f77bcf86cd799439011",
            null,
            null);

    when(repository.findByApplicationID(eq("507f1f77bcf86cd799439011"))).thenReturn(
        Optional.of(savedIdentityWithNoVotButVerified));

    IdvAgentUpliftOutcome result = service.upliftIdentityStatusByAgent("507f1f77bcf86cd799439011",
        upliftDetails);

    assertEquals(result, IdvAgentUpliftOutcome.ALREADY_MEDIUM_OR_VERIFIED);
  }

  @Test
  void shouldReturnNotFoundWhenIdentityNotInRepsitory() {
    UpliftDto upliftDetails = new UpliftDto();
    upliftDetails.setStaffId("123456788");

    when(repository.findByApplicationID(eq("507f1f77bcf86cd799439011"))).thenReturn(
        Optional.empty());

    IdvAgentUpliftOutcome result = service.upliftIdentityStatusByAgent("507f1f77bcf86cd799439011",
        upliftDetails);

    assertEquals(result, IdvAgentUpliftOutcome.IDENTITY_NOT_FOUND);
  }
}
