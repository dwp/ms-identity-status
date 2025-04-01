package uk.gov.dwp.health.pip.identity.api;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.dwp.health.identity.status.openapi.model.ApplicationIdDto;
import uk.gov.dwp.health.identity.status.openapi.model.IdentityDto;
import uk.gov.dwp.health.identity.status.openapi.model.IdentityResponse;
import uk.gov.dwp.health.identity.status.openapi.model.IdentityResponse2;
import uk.gov.dwp.health.identity.status.openapi.model.IdvDto;
import uk.gov.dwp.health.identity.status.openapi.model.UpliftDto;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.model.IdentityResponseDto;
import uk.gov.dwp.health.pip.identity.model.IdvAgentUpliftOutcome;
import uk.gov.dwp.health.pip.identity.service.IdentityRegistrationService;
import uk.gov.dwp.health.pip.identity.service.impl.IdentityServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityControllerTest {

  private final UUID identityId = UUID.randomUUID();
  private final UUID applicationId = UUID.randomUUID();
  private static final String OIDV = IdentityRequestUpdateSchemaV1.Channel.OIDV.value();
  @InjectMocks
  private IdentityController controller;
  @Mock
  private IdentityServiceImpl service;
  @Mock
  private IdentityRegistrationService registrationService;

  @Test
  void getIdentityBySubjectId_returns200_whenSuccessfullyFound() {
    String subjectId = "test@dwp.gov.uk";
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    Identity savedIdentity = new Identity("id", subjectId, identityId, dateTime, OIDV, "verified",
        "nino", "applicationId", "", null);

    when(service.getIdentityBySubjectId(subjectId)).thenReturn(Optional.of(savedIdentity));

    ResponseEntity<IdvDto> expectedResponse = new ResponseEntity<>(
        new IdvDto().idvStatus(IdvDto.IdvStatusEnum.VERIFIED), HttpStatus.OK);

    ResponseEntity<IdvDto> actualResponse = controller.getIdentityBySubjectId(subjectId);
    assertEquals(expectedResponse, actualResponse);
  }

  @Test
  void getIdentityBySubjectId_returns404_whenNoIdentityFound() {
    String subjectId = "test@dwp.gov.uk";

    when(service.getIdentityBySubjectId(subjectId)).thenReturn(Optional.empty());

    ResponseEntity<IdvDto> expectedResponse = new ResponseEntity<>(HttpStatus.NOT_FOUND);
    ResponseEntity<IdvDto> actualResponse = controller.getIdentityBySubjectId(subjectId);

    assertEquals(expectedResponse, actualResponse);
  }

  @Test
  void getIdentityBySubjectId_returns404_incaseOfInvalidSubjectId() {
    String subjectId = "test@dwp.gov.uk@test";

    ResponseEntity<IdvDto> expectedResponse = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    ResponseEntity<IdvDto> actualResponse = controller.getIdentityBySubjectId(subjectId);
    assertEquals(expectedResponse, actualResponse);
  }

  @Test
  void getIdentityByNino_returns200_whenSuccessfullyFound() {
    String nino = "RN000010A";
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    Identity savedIdentity = new Identity("id", "test@dwp.gov.uk", identityId, dateTime, OIDV,
        "verified", nino, "applicationId", "", null);

    when(service.getIdentityByNino(nino)).thenReturn(Optional.of(savedIdentity));

    ResponseEntity<IdvDto> expectedResponse = new ResponseEntity<>(
        new IdvDto().idvStatus(IdvDto.IdvStatusEnum.VERIFIED), HttpStatus.OK);

    ResponseEntity<IdvDto> actualResponse = controller.getIdentityByNino(nino);
    assertEquals(expectedResponse, actualResponse);
  }

  @Test
  void getIdentityByNino_returns404_whenNoIdentityFound() {
    String nino = "RN000010A";
    when(service.getIdentityByNino(nino)).thenReturn(Optional.empty());

    ResponseEntity<IdvDto> expectedResponse = new ResponseEntity<>(HttpStatus.NOT_FOUND);
    ResponseEntity<IdvDto> actualResponse = controller.getIdentityByNino(nino);

    assertEquals(expectedResponse, actualResponse);
  }

  @Test
  void getIdentityByApplicationId_returns200_whenSuccessfullyFound() {
    var applicationId = "507f1f77bcf86cd799439011";
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    Identity savedIdentity = new Identity("id", "test@dwp.gov.uk", identityId, dateTime, OIDV,
        "verified", "RN000010A", applicationId, "", null);

    when(service.getIdentityByApplicationId(applicationId)).thenReturn(Optional.of(savedIdentity));

    ResponseEntity<IdentityDto> actualResponse = controller.getIdentityByApplicationId(
        applicationId);
    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    assertEquals(applicationId, actualResponse.getBody().getApplicationId());
    assertEquals("RN000010A", actualResponse.getBody().getNino());
    assertEquals("test@dwp.gov.uk", actualResponse.getBody().getSubjectId());
  }

  @Test
  void getIdentityByApplicationId_returns404_whenNoIdentityFound() {
    var applicationId = "507f1f77bcf86cd799439011";
    when(service.getIdentityByApplicationId(applicationId)).thenReturn(Optional.empty());

    ResponseEntity<IdentityDto> expectedResponse = new ResponseEntity<>(HttpStatus.NOT_FOUND);
    ResponseEntity<IdentityDto> actualResponse = controller.getIdentityByApplicationId(
        applicationId);

    assertEquals(expectedResponse, actualResponse);
  }

  @Test
  void getIdentityById_returns200_whenSuccessfullyFound() {
    var id = "507f1f77bcf86cd799439011";
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    Identity savedIdentity = new Identity(id, "test@dwp.gov.uk", identityId, dateTime, OIDV,
        "verified", "RN000010A", "applicationId", "", null);

    when(service.getIdentityById(id)).thenReturn(Optional.of(savedIdentity));

    ResponseEntity<IdentityDto> actualResponse = controller.getIdentityById(id);
    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    assertEquals("applicationId", actualResponse.getBody().getApplicationId());
    assertEquals("RN000010A", actualResponse.getBody().getNino());
    assertEquals("test@dwp.gov.uk", actualResponse.getBody().getSubjectId());
  }

  @Test
  void getIdentityById_returns404_whenNoIdentityFound() {
    var id = "507f1f77bcf86cd799439011";
    when(service.getIdentityById(id)).thenReturn(Optional.empty());

    ResponseEntity<IdentityDto> expectedResponse = new ResponseEntity<>(HttpStatus.NOT_FOUND);
    ResponseEntity<IdentityDto> actualResponse = controller.getIdentityById(id);

    assertEquals(expectedResponse, actualResponse);
  }

  @Test
  void shouldReturn200ForUpdateIdentityResponse() {
    String dummy_token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmNAZ21haWwuY29tIiwidm90I"
        + "joiUDIuQ2wuQ20iLCJpYXQiOjE1MTYyMzkwMjJ9.zxdfmq-Mo5MRWOKfyamKuSPWQF4gr8aLFU2zseFh7TA";
    IdentityResponse2 ref = new IdentityResponse2().ref("12435668768979");
    when(registrationService.register(contains("abc@gmail.com"), eq(OIDV), eq(false))).thenReturn(
        IdentityResponseDto.of(false, ref));
    ResponseEntity<IdentityResponse> registered = controller.register(dummy_token, OIDV, false);
    Assertions.assertThat(registered).extracting(ResponseEntity::getStatusCode)
        .isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldReturn201ForCreateIdentityResponse() {
    String dummy_token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmNAZ21haWwuY29tIiwidm90I"
        + "joiUDIuQ2wuQ20iLCJpYXQiOjE1MTYyMzkwMjJ9.zxdfmq-Mo5MRWOKfyamKuSPWQF4gr8aLFU2zseFh7TA";
    IdentityResponse2 ref = new IdentityResponse2().ref("12435668768979");
    when(registrationService.register(contains("abc@gmail.com"), eq(OIDV), eq(false))).thenReturn(
        IdentityResponseDto.of(true, ref));
    ResponseEntity<IdentityResponse> registered = controller.register(dummy_token, OIDV, false);
    Assertions.assertThat(registered).extracting(ResponseEntity::getStatusCode)
        .isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void shouldReturn200ForMediumUplift() {
    String dummy_token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmNAZ21haWwuY29tIiwidm90I"
        + "joiUDIuQ2wuQ20iLCJpYXQiOjE1MTYyMzkwMjJ9.zxdfmq-Mo5MRWOKfyamKuSPWQF4gr8aLFU2zseFh7TA";
    IdentityResponseDto ref = IdentityResponseDto.of(false, new IdentityResponse2());
    when(registrationService.register(contains("abc@gmail.com"), eq(OIDV), eq(true))).thenReturn(ref);
    ResponseEntity<IdentityResponse> registered = controller.register(dummy_token, OIDV, true);
    Assertions.assertThat(registered).extracting(ResponseEntity::getStatusCode)
        .isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldReturn202ForUpdateApplicationIdResponse() {
    var identityId = this.identityId.toString();
    var applicationId = this.applicationId.toString();
    var applicationIdDto = new ApplicationIdDto();
    applicationIdDto.setApplicationId(applicationId);

    ResponseEntity<Void> actualResponse = controller.updateApplicationId(identityId,
        applicationIdDto);
    assertEquals(HttpStatus.ACCEPTED, actualResponse.getStatusCode());

  }

  @Test
  void shouldReturn200ForNoVotIdentityExists() {
    String dummy_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ"
                         + "hYmNAZ21haWwuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ.hxyJgLgDMDhoxK1QTqiAB6WNYkDFfUD_az0RmFanu7g";
    IdentityResponse2 ref = new IdentityResponse2().ref("12435668768979");
    when(registrationService.register(contains("abc@gmail.com"), eq(OIDV), eq(false))).thenReturn(
        IdentityResponseDto.of(false, ref));
    ResponseEntity<IdentityResponse> registered = controller.register(dummy_token, OIDV, false);
    Assertions.assertThat(registered).extracting(ResponseEntity::getStatusCode)
        .isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldReturn202ForIdvUplift() {
    UpliftDto upliftRequest = new UpliftDto();
    upliftRequest.setStaffId("123456789");
    String applicationId = "12435668768979";

    when(service.upliftIdentityStatusByAgent(eq(applicationId), any(UpliftDto.class))).thenReturn(
        IdvAgentUpliftOutcome.SUCCESS);

    ResponseEntity<Void> response = controller.upliftIdentityStatusForApplicationId(applicationId,
        upliftRequest);

    Assertions.assertThat(response).extracting(ResponseEntity::getStatusCode)
        .isEqualTo(HttpStatus.ACCEPTED);
  }

  @Test
  void shouldReturn423ForIdvUpliftIfRecordAlreadyMedium() {
    UpliftDto upliftRequest = new UpliftDto();
    upliftRequest.setStaffId("123456789");
    String applicationId = "12435668768979";

    when(service.upliftIdentityStatusByAgent(eq(applicationId), any(UpliftDto.class))).thenReturn(
        IdvAgentUpliftOutcome.ALREADY_MEDIUM_OR_VERIFIED);

    ResponseEntity<Void> response = controller.upliftIdentityStatusForApplicationId(applicationId,
        upliftRequest);

    Assertions.assertThat(response).extracting(ResponseEntity::getStatusCode)
        .isEqualTo(HttpStatus.LOCKED);

  }

  @Test
  void shouldReturn404ForIdvUpliftIfNoIdentityRecordForApplicationId() {
    UpliftDto upliftRequest = new UpliftDto();
    upliftRequest.setStaffId("123456789");
    String applicationId = "12435668768979";

    when(service.upliftIdentityStatusByAgent(eq(applicationId), any(UpliftDto.class))).thenReturn(
        IdvAgentUpliftOutcome.IDENTITY_NOT_FOUND);

    ResponseEntity<Void> response = controller.upliftIdentityStatusForApplicationId(applicationId,
        upliftRequest);

    Assertions.assertThat(response).extracting(ResponseEntity::getStatusCode)
        .isEqualTo(HttpStatus.NOT_FOUND);
  }
}
