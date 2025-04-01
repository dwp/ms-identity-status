package uk.gov.dwp.health.pip.identity.api;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.dwp.health.identity.status.openapi.model.IdentityResponse2;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.model.IdentityResponseDto;
import uk.gov.dwp.health.pip.identity.service.IdentityRegistrationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityControllerV2Test {

  private static final String OIDV = IdentityRequestUpdateSchemaV1.Channel.OIDV.value();
  private static final String NINO = "nino";
  @InjectMocks
  private IdentityControllerV2 controller;
  @Mock
  private IdentityRegistrationService registrationService;

  @Test
  void shouldReturn200ForUpdateIdentityResponse() {
    String dummy_token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmNAZ21haWwuY29tIiwidm90I"
        + "joiUDIuQ2wuQ20iLCJpYXQiOjE1MTYyMzkwMjJ9.zxdfmq-Mo5MRWOKfyamKuSPWQF4gr8aLFU2zseFh7TA";
    IdentityResponse2 ref = new IdentityResponse2().ref("12435668768979").nino(NINO);
    when(registrationService.register(contains("abc@gmail.com"), eq(OIDV), eq(false))).thenReturn(
        IdentityResponseDto.of(false, ref));
    ResponseEntity<IdentityResponse2> registered = controller.registerV2(dummy_token, OIDV, false);
    Assertions.assertThat(registered).extracting(ResponseEntity::getStatusCode)
        .isEqualTo(HttpStatus.OK);
    assertTrue(registered.hasBody());
    assertEquals(NINO, registered.getBody().getNino());
  }

  @Test
  void shouldReturn201ForCreateIdentityResponse() {
    String dummy_token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmNAZ21haWwuY29tIiwidm90I"
        + "joiUDIuQ2wuQ20iLCJpYXQiOjE1MTYyMzkwMjJ9.zxdfmq-Mo5MRWOKfyamKuSPWQF4gr8aLFU2zseFh7TA";
    IdentityResponse2 ref = new IdentityResponse2().ref("12435668768979").nino(NINO);
    when(registrationService.register(contains("abc@gmail.com"), eq(OIDV), eq(false))).thenReturn(
        IdentityResponseDto.of(true, ref));
    ResponseEntity<IdentityResponse2> registered = controller.registerV2(dummy_token, OIDV, false);
    Assertions.assertThat(registered).extracting(ResponseEntity::getStatusCode)
        .isEqualTo(HttpStatus.CREATED);
    assertTrue(registered.hasBody());
    assertEquals(NINO, registered.getBody().getNino());
  }

  @Test
  void shouldReturn200ForMediumUplift() {
    String dummy_token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmNAZ21haWwuY29tIiwidm90I"
        + "joiUDIuQ2wuQ20iLCJpYXQiOjE1MTYyMzkwMjJ9.zxdfmq-Mo5MRWOKfyamKuSPWQF4gr8aLFU2zseFh7TA";
    IdentityResponseDto ref = IdentityResponseDto.of(false, new IdentityResponse2().nino(NINO));
    when(registrationService.register(contains("abc@gmail.com"), eq(OIDV), eq(true))).thenReturn(ref);
    ResponseEntity<IdentityResponse2> registered = controller.registerV2(dummy_token, OIDV, true);
    Assertions.assertThat(registered).extracting(ResponseEntity::getStatusCode)
        .isEqualTo(HttpStatus.OK);
    assertTrue(registered.hasBody());
    assertEquals(NINO, registered.getBody().getNino());
  }

  @Test
  void shouldReturn200ForNoVotIdentityExists() {
    String dummy_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ"
                         + "hYmNAZ21haWwuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ.hxyJgLgDMDhoxK1QTqiAB6WNYkDFfUD_az0RmFanu7g";
    IdentityResponse2 ref = new IdentityResponse2().ref("12435668768979").nino(NINO);
    when(registrationService.register(contains("abc@gmail.com"), eq(OIDV), eq(false))).thenReturn(
        IdentityResponseDto.of(false, ref));
    ResponseEntity<IdentityResponse2> registered = controller.registerV2(dummy_token, OIDV, false);
    Assertions.assertThat(registered).extracting(ResponseEntity::getStatusCode)
        .isEqualTo(HttpStatus.OK);
    assertTrue(registered.hasBody());
    assertEquals(NINO, registered.getBody().getNino());
  }

}
