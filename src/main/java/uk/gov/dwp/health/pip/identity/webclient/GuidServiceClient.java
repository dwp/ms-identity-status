package uk.gov.dwp.health.pip.identity.webclient;

import static uk.gov.dwp.health.pip.identity.constants.Constants.CORRELATION_ID;
import static uk.gov.dwp.health.pip.identity.constants.Constants.IDENTIFIER;
import static uk.gov.dwp.health.pip.identity.constants.Constants.REQUEST_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.pip.identity.exception.IdentityRestClientException;
import uk.gov.dwp.health.pip.identity.model.CognitoToken;
import uk.gov.dwp.health.pip.identity.model.IdentifierDto;
import uk.gov.dwp.health.pip.identity.service.impl.CognitoService;

@Component
@Slf4j
@RequiredArgsConstructor
public class GuidServiceClient {
  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate;
  private final CognitoService cognitoService;

  @Value("${uk.gov.dwp.guid.service.nino.url}")
  private String guidServiceNinoUrl;

  @Value("${uk.gov.dwp.guid.service.guid.url}")
  private String guidServiceGuidUrl;

  @Value("${uk.gov.dwp.integration-gateway.base-url}")
  private String integrationGatewayBaseUrl;

  public IdentifierDto getGuidFromNino(String nino) {
    CognitoToken guidCognitoToken = cognitoService.getCognitoToken();
    HttpHeaders guidRequestHeaders = new HttpHeaders();
    String correlationId = UUID.randomUUID().toString();
    String requestId = UUID.randomUUID().toString();
    guidRequestHeaders.add(CORRELATION_ID, correlationId);
    guidRequestHeaders.add(REQUEST_ID, requestId);
    guidRequestHeaders.add(IDENTIFIER, nino);
    guidRequestHeaders.setBearerAuth(guidCognitoToken.getAccessToken());
    HttpEntity<String> guidRequest = new HttpEntity<>(guidRequestHeaders);
    log.info("Attempting to get DWP GUID for NINO, correlation id " + correlationId);
    try {
      ResponseEntity<String> guidResponse =
          restTemplate.exchange(
              integrationGatewayBaseUrl + guidServiceGuidUrl,
              HttpMethod.GET,
              guidRequest,
              String.class);
      log.info("DWP GUID Retrieved Successfully, correlation_id: " + correlationId);
      return handleGuidResponse(guidResponse);
    } catch (RestClientException restClientException) {
      log.error(
          "ClientException Calling Guid Service, error message: "
              + restClientException.getMessage()
              + " correlation_id: "
              + correlationId);
      throw new IdentityRestClientException(
          "Error Communicating with Guid Server, review application logs");
    } catch (Exception e) {
      log.error(
          "Exception calling the guid service: "
              + e.getMessage()
              + " for correlation id : "
              + correlationId);
      throw new RuntimeException("Exception calling guid service");
    }
  }

  public IdentifierDto getNinoFromGuid(String identifierValue) {
    CognitoToken guidCognitoToken = cognitoService.getCognitoToken();
    HttpHeaders guidRequestHeaders = new HttpHeaders();
    String correlationId = UUID.randomUUID().toString();
    String requestId = UUID.randomUUID().toString();
    guidRequestHeaders.add(CORRELATION_ID, correlationId);
    guidRequestHeaders.add(REQUEST_ID, requestId);
    guidRequestHeaders.add(IDENTIFIER, identifierValue);
    guidRequestHeaders.setBearerAuth(guidCognitoToken.getAccessToken());
    HttpEntity<String> guidRequest = new HttpEntity<>(guidRequestHeaders);
    log.info("Attempting to get NINO for DWP Guid, correlation id " + correlationId);
    try {
      ResponseEntity<String> guidResponse =
          restTemplate.exchange(
              integrationGatewayBaseUrl + guidServiceNinoUrl,
              HttpMethod.GET,
              guidRequest,
              String.class);
      log.info("NINO Retrieved Successfully, correlation_id: " + correlationId);
      return handleGuidResponse(guidResponse);
    } catch (RestClientException restClientException) {
      log.error(
          "ClientException Calling Guid Service, error message: "
              + restClientException.getMessage()
              + " correlation_id: "
              + correlationId);
      throw new IdentityRestClientException(
          "Error Communicating with Guid Server, review application logs");
    } catch (Exception e) {
      log.error(
          "Exception calling the guid service: "
              + e.getMessage()
              + " for correlation id : "
              + correlationId);
      throw new RuntimeException("Exception calling guid service");
    }
  }

  private IdentifierDto handleGuidResponse(ResponseEntity<String> guidResponse)
      throws JsonProcessingException {
    if (guidResponse.getStatusCode() == HttpStatus.OK) {
      return objectMapper.readValue(guidResponse.getBody(), new TypeReference<>() {});
    } else {
      throw new IdentityRestClientException(
          "Guid service returned unexpected status code : "
              + guidResponse.getStatusCode()
              + ", with reason : "
              + guidResponse.getBody());
    }
  }
}
