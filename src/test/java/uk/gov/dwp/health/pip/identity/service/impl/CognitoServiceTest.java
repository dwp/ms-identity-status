package uk.gov.dwp.health.pip.identity.service.impl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.pip.identity.exception.IdentityRestClientException;
import uk.gov.dwp.health.pip.identity.model.CognitoToken;

@ExtendWith(MockitoExtension.class)
public class CognitoServiceTest {
  public static final String INTEGRATION_URL = "http://integration-gateway:8080";
  public static final String CITIZEN_INFORMATION_OAUTH_2_TOKEN =
      "/citizen-information/oauth2/token";
  @Mock RestTemplate restTemplate;

  @InjectMocks CognitoService cognitoService;

  @BeforeEach
  public void setup() {
    ReflectionTestUtils.setField(cognitoService, "cognitoClientID", "example-client-id");
    ReflectionTestUtils.setField(cognitoService, "cognitoSecret", "example-secret");
    ReflectionTestUtils.setField(
        cognitoService, "authTokenServiceUrl", CITIZEN_INFORMATION_OAUTH_2_TOKEN);
    ReflectionTestUtils.setField(cognitoService, "integrationGatewayBaseUrl", INTEGRATION_URL);
  }

  @Test
  void shouldReturnCognitoTokenWhenCallingCognitoService() {

    String tokenType = "Bearer";
    String tokenValue = "example_token";
    CognitoToken cognitoToken =
        CognitoToken.builder().tokenType(tokenType).accessToken(tokenValue).build();
    ResponseEntity<CognitoToken> cognitoResponse =
        new ResponseEntity<CognitoToken>(cognitoToken, HttpStatusCode.valueOf(200));

    Mockito.when(
            restTemplate.postForEntity(
                eq(INTEGRATION_URL + CITIZEN_INFORMATION_OAUTH_2_TOKEN),
                any(),
                eq(CognitoToken.class)))
        .thenReturn(cognitoResponse);
    CognitoToken actualToken = cognitoService.getCognitoToken();

    Assertions.assertEquals(tokenValue, actualToken.getAccessToken());
    verify(restTemplate).postForEntity(anyString(), any(), eq(CognitoToken.class));
  }

  @Test
  void shouldReturnRestClientExceptionWhenErrorCallingCognitoServer() {
    Mockito.when(
            restTemplate.postForEntity(
                eq(INTEGRATION_URL + CITIZEN_INFORMATION_OAUTH_2_TOKEN),
                any(),
                eq(CognitoToken.class)))
        .thenThrow(RestClientException.class);

    Assertions.assertThrows(
        IdentityRestClientException.class, () -> cognitoService.getCognitoToken());

    verify(restTemplate).postForEntity(anyString(), any(), eq(CognitoToken.class));
  }

  @Test
  void shouldThrowRuntimeExceptionWhenUnknownErrorCallingCognitoServer() {
    Mockito.when(
            restTemplate.postForEntity(
                eq(INTEGRATION_URL + CITIZEN_INFORMATION_OAUTH_2_TOKEN),
                any(),
                eq(CognitoToken.class)))
        .thenThrow(RuntimeException.class);

    Assertions.assertThrows(RuntimeException.class, () -> cognitoService.getCognitoToken());

    verify(restTemplate).postForEntity(anyString(), any(), eq(CognitoToken.class));
  }
}
