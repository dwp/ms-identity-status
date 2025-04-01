package uk.gov.dwp.health.pip.identity.service.impl;

import static uk.gov.dwp.health.pip.identity.constants.Constants.CLIENT_CREDENTIALS;
import static uk.gov.dwp.health.pip.identity.constants.Constants.CLIENT_ID;
import static uk.gov.dwp.health.pip.identity.constants.Constants.CORRELATION_ID;
import static uk.gov.dwp.health.pip.identity.constants.Constants.GRANT_TYPE;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.pip.identity.exception.IdentityRestClientException;
import uk.gov.dwp.health.pip.identity.model.CognitoToken;

@Service
@Slf4j
@RequiredArgsConstructor
public class CognitoService {

  private final RestTemplate restTemplate;

  @Value("#{${uk.gov.dwp.guid.service.guid-credentials}.client_id}")
  private String cognitoClientID;

  @Value("#{${uk.gov.dwp.guid.service.guid-credentials}.secret}")
  private String cognitoSecret;

  @Value("${uk.gov.dwp.auth.token.service.url}")
  private String authTokenServiceUrl;

  @Value("${uk.gov.dwp.integration-gateway.base-url}")
  private String integrationGatewayBaseUrl;

  public CognitoToken getCognitoToken() {
    HttpHeaders cognitoRequestHeaders = new HttpHeaders();
    String correlationId = UUID.randomUUID().toString();
    cognitoRequestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    cognitoRequestHeaders.setBasicAuth(cognitoClientID, cognitoSecret);
    cognitoRequestHeaders.add(CORRELATION_ID, correlationId);

    MultiValueMap<String, String> cognitoRequestBody = new LinkedMultiValueMap<>();
    cognitoRequestBody.add(GRANT_TYPE, CLIENT_CREDENTIALS);
    cognitoRequestBody.add(CLIENT_ID, cognitoClientID);

    HttpEntity<MultiValueMap<String, String>> request =
        new HttpEntity<>(cognitoRequestBody, cognitoRequestHeaders);
    log.info("Attempting to retrieve cognito token , correlation_id: {}", correlationId);
    try {
      ResponseEntity<CognitoToken> response =
          restTemplate.postForEntity(
              integrationGatewayBaseUrl + authTokenServiceUrl, request, CognitoToken.class);
      log.info("Cognito Token Retrieved Successfully, correlation_id: " + correlationId);
      return response.getBody();
    } catch (org.springframework.web.client.RestClientException restClientException) {
      log.error(
          "Client exception Calling Cognito Service, error message: "
              + restClientException.getMessage()
              + " correlation_id: "
              + correlationId, restClientException);
      throw new IdentityRestClientException(
          "Error Communicating with Cognito Server, review application logs");
    } catch (Exception e) {
      log.error(
          "Exception calling the cognito service: "
              + e.getMessage()
              + " for correlation id : "
              + correlationId, e);
      throw new RuntimeException("Unknown exception calling guid service");
    }
  }
}
