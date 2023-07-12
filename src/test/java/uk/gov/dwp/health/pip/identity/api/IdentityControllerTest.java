package uk.gov.dwp.health.pip.identity.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            "id", subjectId, identityId, dateTime, "oidv", "verified", "nino", "applicationId", "");

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
            "");

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
}
