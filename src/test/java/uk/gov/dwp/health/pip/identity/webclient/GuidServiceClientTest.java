package uk.gov.dwp.health.pip.identity.webclient;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.pip.identity.constants.Constants;
import uk.gov.dwp.health.pip.identity.exception.IdentityRestClientException;
import uk.gov.dwp.health.pip.identity.model.CognitoToken;
import uk.gov.dwp.health.pip.identity.model.IdentifierDto;
import uk.gov.dwp.health.pip.identity.service.impl.CognitoService;

@ExtendWith(MockitoExtension.class)
public class GuidServiceClientTest {
  public static final String CITIZEN_INFORMATION_DWP_GUID_SERVICE_NINO =
      "/citizen-information/dwp-guid-service/nino";
  public static final String CITIZEN_INFORMATION_DWP_GUID_SERVICE_GUID =
      "/citizen-information/dwp-guid-service/guid";
  public static final String INTEGRATION_URL = "http://integration-gateway:8080";
  private static final String SAMPLE_GUID =
      "12345678abcdabcd12345678abcdabcd12345678abcdabcd12345678abcdabcd";
  String sampleNino = "RN000008A";
  @InjectMocks private GuidServiceClient guidServiceClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  @Captor private ArgumentCaptor<HttpEntity> httpEntityArgumentCaptor;
  @Mock private RestTemplate restTemplate;
  @Mock private CognitoService cognitoService;

  @BeforeEach
  void setup() {
    guidServiceClient = new GuidServiceClient(objectMapper, restTemplate, cognitoService);
    ReflectionTestUtils.setField(
        guidServiceClient, "guidServiceNinoUrl", CITIZEN_INFORMATION_DWP_GUID_SERVICE_NINO);
    ReflectionTestUtils.setField(
        guidServiceClient, "guidServiceGuidUrl", CITIZEN_INFORMATION_DWP_GUID_SERVICE_GUID);
    ReflectionTestUtils.setField(guidServiceClient, "integrationGatewayBaseUrl", INTEGRATION_URL);
  }

  @Test
  void shouldReturnNinoFromGuid() {
    CognitoToken cognitoToken =
        CognitoToken.builder().tokenType("Bearer").accessToken("example-token").build();
    IdentifierDto identifierDto =
        IdentifierDto.builder().type(Constants.NINO).identifier(sampleNino).build();
    String identifierResponse = getIdentifierResponse(identifierDto);
    ResponseEntity<String> ninoResponse = ResponseEntity.ok(identifierResponse);
    Mockito.when(cognitoService.getCognitoToken()).thenReturn(cognitoToken);
    Mockito.when(
            restTemplate.exchange(
                eq(INTEGRATION_URL + CITIZEN_INFORMATION_DWP_GUID_SERVICE_NINO),
                eq(HttpMethod.GET),
                httpEntityArgumentCaptor.capture(),
                eq(String.class)))
        .thenReturn(ninoResponse);

    IdentifierDto ninoIdentifierResponse = guidServiceClient.getNinoFromGuid(SAMPLE_GUID);

    Assertions.assertEquals(sampleNino, ninoIdentifierResponse.getIdentifier());
    Assertions.assertEquals(Constants.NINO, ninoIdentifierResponse.getType());
    Assertions.assertEquals(
        httpEntityArgumentCaptor.getValue().getHeaders().getFirst(Constants.IDENTIFIER),
        SAMPLE_GUID);
    Mockito.verify(cognitoService).getCognitoToken();
    Mockito.verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class));
  }

  @Test
  void guidToNinoShouldThrowRuntimeExceptionWhenStatusCodeNot200() {
    CognitoToken cognitoToken =
        CognitoToken.builder().tokenType("Bearer").accessToken("example-token").build();
    ResponseEntity ninoResponse = ResponseEntity.internalServerError().build();
    Mockito.when(cognitoService.getCognitoToken()).thenReturn(cognitoToken);
    Mockito.when(
            restTemplate.exchange(
                eq(INTEGRATION_URL + CITIZEN_INFORMATION_DWP_GUID_SERVICE_NINO),
                eq(HttpMethod.GET),
                httpEntityArgumentCaptor.capture(),
                eq(String.class)))
        .thenReturn(ninoResponse);

    RuntimeException runtimeException =
        Assertions.assertThrows(
            RuntimeException.class, () -> guidServiceClient.getNinoFromGuid(SAMPLE_GUID));
    Assertions.assertEquals(runtimeException.getMessage(), "Exception calling guid service");
  }

  @Test
  void guidToNinoShouldThrowRestClientExceptionWhenRestClientExceptionRaised() {
    CognitoToken cognitoToken =
        CognitoToken.builder().tokenType("Bearer").accessToken("example-token").build();
    Mockito.when(cognitoService.getCognitoToken()).thenReturn(cognitoToken);
    Mockito.when(
            restTemplate.exchange(
                eq(INTEGRATION_URL + CITIZEN_INFORMATION_DWP_GUID_SERVICE_NINO),
                eq(HttpMethod.GET),
                httpEntityArgumentCaptor.capture(),
                eq(String.class)))
        .thenThrow(new RestClientException("Unknown Exception"));

    IdentityRestClientException exception =
        Assertions.assertThrows(
            IdentityRestClientException.class, () -> guidServiceClient.getNinoFromGuid(SAMPLE_GUID));
    Assertions.assertEquals(
        exception.getMessage(), "Error Communicating with Guid Server, review application logs");
  }

  @Test
  void shouldReturnGuidFromNino() {
    CognitoToken cognitoToken =
        CognitoToken.builder().tokenType("Bearer").accessToken("example-token").build();
    IdentifierDto identifierDto =
        IdentifierDto.builder().type(Constants.DWP_GUID).identifier(SAMPLE_GUID).build();
    String identifierResponse = getIdentifierResponse(identifierDto);
    ResponseEntity<String> guidResponse = ResponseEntity.ok(identifierResponse);
    Mockito.when(cognitoService.getCognitoToken()).thenReturn(cognitoToken);
    Mockito.when(
            restTemplate.exchange(
                eq(INTEGRATION_URL + CITIZEN_INFORMATION_DWP_GUID_SERVICE_GUID),
                eq(HttpMethod.GET),
                httpEntityArgumentCaptor.capture(),
                eq(String.class)))
        .thenReturn(guidResponse);

    IdentifierDto guidIdentifierResponse = guidServiceClient.getGuidFromNino(sampleNino);

    Assertions.assertEquals(SAMPLE_GUID, guidIdentifierResponse.getIdentifier());
    Assertions.assertEquals(Constants.DWP_GUID, guidIdentifierResponse.getType());
    Assertions.assertEquals(
        httpEntityArgumentCaptor.getValue().getHeaders().getFirst(Constants.IDENTIFIER),
        sampleNino);
    Mockito.verify(cognitoService).getCognitoToken();
    Mockito.verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class));
  }

  @Test
  void NinoToGuidShouldThrowRuntimeExceptionWhenStatusCodeNot200() {
    CognitoToken cognitoToken =
        CognitoToken.builder().tokenType("Bearer").accessToken("example-token").build();
    ResponseEntity guidResponse = ResponseEntity.internalServerError().build();
    Mockito.when(cognitoService.getCognitoToken()).thenReturn(cognitoToken);
    Mockito.when(
            restTemplate.exchange(
                eq(INTEGRATION_URL + CITIZEN_INFORMATION_DWP_GUID_SERVICE_GUID),
                eq(HttpMethod.GET),
                httpEntityArgumentCaptor.capture(),
                eq(String.class)))
        .thenReturn(guidResponse);

    RuntimeException runtimeException =
        Assertions.assertThrows(
            RuntimeException.class, () -> guidServiceClient.getGuidFromNino(sampleNino));
    Assertions.assertEquals(runtimeException.getMessage(), "Exception calling guid service");
  }

  @Test
  void NinoToGuidShouldThrowRestClientExceptionWhenRestClientExceptionRaised() {
    CognitoToken cognitoToken =
        CognitoToken.builder().tokenType("Bearer").accessToken("example-token").build();
    Mockito.when(cognitoService.getCognitoToken()).thenReturn(cognitoToken);
    Mockito.when(
            restTemplate.exchange(
                eq(INTEGRATION_URL + CITIZEN_INFORMATION_DWP_GUID_SERVICE_GUID),
                eq(HttpMethod.GET),
                httpEntityArgumentCaptor.capture(),
                eq(String.class)))
        .thenThrow(new RestClientException("Unknown Exception"));

    IdentityRestClientException exception =
        Assertions.assertThrows(
            IdentityRestClientException.class, () -> guidServiceClient.getGuidFromNino(sampleNino));
    Assertions.assertEquals(
        exception.getMessage(), "Error Communicating with Guid Server, review application logs");
  }

  private String getIdentifierResponse(IdentifierDto identifierDto) {
    try {
      return objectMapper.writeValueAsString(identifierDto);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
