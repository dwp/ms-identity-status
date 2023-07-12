package uk.gov.dwp.health.pip.identity.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.pip.identity.config.web.ReactorWebClient;
import uk.gov.dwp.health.pip.identity.config.web.ReactorWebClientFactory;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.repository.IdentityRepository;
import uk.gov.dwp.health.pip.identity.service.IdentityService;
import uk.gov.dwp.health.pip.identity.utils.DateParseUtil;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class IdentityServiceImpl implements IdentityService {

  private static final String APPLICATION_MATCHER_PATH = "/v1/application/matcher";
  private static final String APPLICATION_ID = "application_id";
  private static final String NINO_INPUT = "{\"nino\":\"NINO_VALUE\"}";
  private final IdentityRepository repository;

  private final ReactorWebClient client;

  public IdentityServiceImpl(IdentityRepository repository, ReactorWebClientFactory factory) {
    this.repository = repository;
    this.client = initGetApplicationId(factory);
  }

  @Override
  public Identity createIdentity(IdentityRequestUpdateSchemaV1 request) {

    log.debug("create identity request received {} ", request.getIdentityId());
    final Optional<Identity> ninoRecord = getIdentityByNino(request.getNino());

    if (ninoRecord.isPresent()) {
      Identity identity = setBaseIdentityFields(ninoRecord.get(), request);
      processApplicationIdForExistingIdentity(request, identity);
      identity.setSubjectId(request.getSubjectId());
      return repository.save(identity);
    }

    final Optional<Identity> subjectRecord = getIdentityBySubjectId(request.getSubjectId());
    if (subjectRecord.isPresent()) {
      Identity identity = setBaseIdentityFields(subjectRecord.get(), request);
      processApplicationIdForExistingIdentity(request, identity);
      identity.setNino(request.getNino());
      return repository.save(identity);
    }

    Identity.IdentityBuilder builder = buildIdentity(request);
    processForApplicationId(request, builder);
    Identity identity = builder.build();
    return repository.save(identity);
  }

  private Identity setBaseIdentityFields(Identity identity, IdentityRequestUpdateSchemaV1 request) {
    identity.setChannel(request.getChannel().toString());
    identity.setIdentityId(request.getIdentityId());
    identity.setIdvStatus(request.getIdvOutcome().toString());
    identity.setDateTime(DateParseUtil.stringToDateTime(request.getTimestamp()));
    return identity;
  }

  private Identity.IdentityBuilder buildIdentity(IdentityRequestUpdateSchemaV1 request) {
    return Identity.builder()
            .subjectId(request.getSubjectId())
            .identityId(request.getIdentityId())
            .channel(request.getChannel().toString())
            .idvStatus(request.getIdvOutcome().toString())
            .nino(request.getNino())
            .dateTime(DateParseUtil.stringToDateTime(request.getTimestamp()));
  }

  private void processApplicationIdForExistingIdentity(IdentityRequestUpdateSchemaV1 request,
                                                       Identity identity) {
    try {
      String applicationIdResponse = getApplicationIdResponse(request);
      JsonParser springParser = JsonParserFactory.getJsonParser();

      if (validApplicationIdExists(applicationIdResponse, springParser)) {
        setApplicationId(identity, applicationIdResponse, springParser);
      } else {
        setErrorMessage(request, identity);
      }
    } catch (GenericRuntimeException gre) {
      log.warn("5xx response code from application-manager.");
      throw gre;
    } catch (Exception ex) {
      identity.setErrorMessage(ex.getLocalizedMessage());
      log.warn(
              String.format(
                      "Call to application-manager service to get applicationId by nino "
                              + "resulted in an exception for identityId %s. Exception message: %s",
                      request.getIdentityId(), ex.getLocalizedMessage()));
    }
  }

  private void processForApplicationId(IdentityRequestUpdateSchemaV1 request,
                                       Identity.IdentityBuilder builder) {
    try {
      String applicationIdResponse = getApplicationIdResponse(request);
      JsonParser springParser = JsonParserFactory.getJsonParser();
      if (validApplicationIdExists(applicationIdResponse, springParser)) {
        setApplicationId(builder, applicationIdResponse, springParser);
      } else {
        setErrorMessage(request, builder);
      }
    } catch (ConflictException ce) {
      handleConflict(request, builder);
    } catch (GenericRuntimeException gre) {
      log.warn("5xx response code from application-manager.");
      throw gre;
    } catch (Exception ex) {
      builder.errorMessage(ex.getLocalizedMessage());
      log.warn(
          String.format(
              "Call to application-manager service to get applicationId by nino "
                  + "resulted in an exception for identityId %s. Exception message: %s",
              request.getIdentityId(), ex.getLocalizedMessage()));
    }
  }

  private void handleConflict(IdentityRequestUpdateSchemaV1 request,
                              Identity.IdentityBuilder builder) {
    String errorMessage =
        "Multiple application IDs found for a nino "
                + "while processing the request for identity id: "
            + request.getIdentityId();
    builder.errorMessage(errorMessage);
    log.warn(errorMessage);
  }

  private String getApplicationIdResponse(IdentityRequestUpdateSchemaV1 request) {
    log.info("About to call ms-application-manager");
    String requestJson = NINO_INPUT.replace("NINO_VALUE", request.getNino());
    String response = client.post(APPLICATION_MATCHER_PATH, String.class, requestJson).block();
    log.info("Response received from ms-application-manager");
    return response;
  }

  private void setApplicationId(Identity identity,
                                String applicationIdResponse, JsonParser springParser) {
    Map<String, Object> applicationIdMap = springParser.parseMap(applicationIdResponse);
    identity.setApplicationID(applicationIdMap.get(APPLICATION_ID).toString());
    identity.setErrorMessage("");
  }

  private void setApplicationId(Identity.IdentityBuilder builder,
                                String applicationIdResponse, JsonParser springParser) {
    Map<String, Object> applicationIdMap = springParser.parseMap(applicationIdResponse);
    builder.applicationID(applicationIdMap.get(APPLICATION_ID).toString());
    builder.errorMessage("");
  }

  private boolean validApplicationIdExists(String applicationIdResponse, JsonParser springParser) {
    if (applicationIdResponse == null || applicationIdResponse.isBlank()) {
      return false;
    }
    Object applicationId = springParser.parseMap(applicationIdResponse).get(APPLICATION_ID);
    return applicationId != null;
  }

  private void setErrorMessage(IdentityRequestUpdateSchemaV1 request, Identity identity) {
    String errorMessage =
            "Application ID not found for identity with id: " + request.getIdentityId();

    identity.setErrorMessage(errorMessage);
    log.warn(errorMessage);
  }

  private void setErrorMessage(
      IdentityRequestUpdateSchemaV1 request, Identity.IdentityBuilder builder) {
    String errorMessage =
        "Application ID not found for identity with id: " + request.getIdentityId();
    builder.errorMessage(errorMessage);
    log.warn(errorMessage);
  }

  @Override
  public Optional<Identity> getIdentityBySubjectId(String subjectId) {
    return repository.findBySubjectId(subjectId);
  }

  @Override
  public Optional<Identity> getIdentityByNino(String nino) {
    return repository.findByNino(nino);
  }

  private ReactorWebClient initGetApplicationId(ReactorWebClientFactory webClientFactory) {
    return webClientFactory.getClient();
  }
}
