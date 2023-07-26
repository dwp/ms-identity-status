package uk.gov.dwp.health.pip.identity.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.dwp.health.identity.status.openapi.model.IdentityDto;
import uk.gov.dwp.health.identity.status.openapi.model.IdvDto;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.service.impl.IdentityServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityControllerTest {
  private final UUID identityId = UUID.randomUUID();
  @InjectMocks private IdentityController controller;
  @Mock private IdentityServiceImpl service;

  @Test
  void getIdentityBySubjectId_returns200_whenSuccessfullyFound() {
    String subjectId = "test@dwp.gov.uk";
    LocalDateTime dateTime = LocalDateTime.now().minusMinutes(2);

    Identity savedIdentity =
        new Identity(
            "id", subjectId, identityId, dateTime, "oidv", "verified", "nino", "applicationId", "", null);

    when(service.getIdentityBySubjectId(subjectId)).thenReturn(Optional.of(savedIdentity));

    ResponseEntity<IdvDto> expectedResponse =
        new ResponseEntity<>(new IdvDto().idvStatus(IdvDto.IdvStatusEnum.VERIFIED), HttpStatus.OK);

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

    Identity savedIdentity =
        new Identity(
            "id",
            "test@dwp.gov.uk",
            identityId,
            dateTime,
            "oidv",
            "verified",
            nino,
            "applicationId",
            "",
            null);

    when(service.getIdentityByNino(nino)).thenReturn(Optional.of(savedIdentity));

    ResponseEntity<IdvDto> expectedResponse =
        new ResponseEntity<>(new IdvDto().idvStatus(IdvDto.IdvStatusEnum.VERIFIED), HttpStatus.OK);

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

    Identity savedIdentity =
        new Identity(
            "id",
            "test@dwp.gov.uk",
            identityId,
            dateTime,
            "oidv",
            "verified",
            "RN000010A",
            applicationId,
            "",
            null);

    when(service.getIdentityByApplicationId(applicationId)).thenReturn(Optional.of(savedIdentity));

    ResponseEntity<IdentityDto> actualResponse = controller.getIdentityByApplicationId(applicationId);
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
    ResponseEntity<IdentityDto> actualResponse = controller.getIdentityByApplicationId(applicationId);

    assertEquals(expectedResponse, actualResponse);
  }
}
