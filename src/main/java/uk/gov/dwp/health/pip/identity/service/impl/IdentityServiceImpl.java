package uk.gov.dwp.health.pip.identity.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.pip.identity.config.web.ReactorWebClient;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.repository.IdentityRepository;
import uk.gov.dwp.health.pip.identity.service.IdentityService;
import uk.gov.dwp.health.pip.identity.utils.DateParseUtil;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityServiceImpl implements IdentityService {

  private static final String APPLICATION_MATCHER_PATH = "/v1/application/matcher";
  private static final String APPLICATION_ID = "application_id";
  private static final String NINO_INPUT = "{\"nino\":\"NINO_VALUE\"}";
  private final IdentityRepository repository;

  private final ReactorWebClient applicationManagerWebClient;

  @Override
  public Identity createIdentity(IdentityRequestUpdateSchemaV1 request) {

    log.debug("create identity request received {} ", request.getIdentityId());
    final Optional<Identity> ninoRecord = getIdentityByNino(request.getNino());

    if (ninoRecord.isPresent()) {
      var builder = IdentityBuilder.createBuilderFromIdentity(ninoRecord.get());
      setBaseIdentityFields(builder, request);
      processApplicationId(request, builder);
      builder.subjectId(request.getSubjectId());
      return repository.save(builder.build());
    }

    final Optional<Identity> subjectRecord = getIdentityBySubjectId(request.getSubjectId());
    if (subjectRecord.isPresent()) {
      var builder = IdentityBuilder.createBuilderFromIdentity(subjectRecord.get());
      setBaseIdentityFields(builder, request);
      processApplicationId(request, builder);
      builder.nino(request.getNino());
      return repository.save(builder.build());
    }

    Identity.IdentityBuilder builder = IdentityBuilder.createBuilder(request);
    processApplicationId(request, builder);
    Identity identity = builder.build();
    return repository.save(identity);
  }

  private Identity.IdentityBuilder setBaseIdentityFields(Identity.IdentityBuilder builder,
                                                         IdentityRequestUpdateSchemaV1 request) {
    builder.channel(request.getChannel().toString());
    builder.identityId(request.getIdentityId());
    builder.idvStatus(request.getIdvOutcome().toString());
    builder.dateTime(DateParseUtil.stringToDateTime(request.getTimestamp()));
    return builder;
  }

  private void processApplicationId(IdentityRequestUpdateSchemaV1 request,
                                    Identity.IdentityBuilder identity) {
    try {
      String applicationIdResponse = getApplicationIdResponse(request);
      JsonParser jsonParser = JsonParserFactory.getJsonParser();

      Optional<Object> optionalApplicationId = getApplicationId(applicationIdResponse, jsonParser);
      optionalApplicationId.ifPresentOrElse(
          applicationId -> setApplicationId(identity, String.valueOf(applicationId)),
          () -> setErrorMessage(request, identity));

    } catch (ConflictException ce) {
      handleConflict(request, identity);
    } catch (GenericRuntimeException gre) {
      log.warn("5xx response code from application-manager.");
      throw gre;
    } catch (Exception ex) {
      identity.errorMessage(ex.getLocalizedMessage());
      log.warn(
          "Call to application-manager service to get applicationId by nino resulted "
              + "in an exception for identityId {}. Exception message: {}",
          request.getIdentityId(),
          ex.getLocalizedMessage());
    }
  }

  private void handleConflict(IdentityRequestUpdateSchemaV1 request,
                              Identity.IdentityBuilder identity) {
    String errorMessage =
        "Multiple application IDs found for a nino while processing the request for identity id: "
            + request.getIdentityId();
    identity.errorMessage(errorMessage);
    log.warn(errorMessage);
  }

  private String getApplicationIdResponse(IdentityRequestUpdateSchemaV1 request) {
    log.info("About to call ms-application-manager");
    String requestJson = NINO_INPUT.replace("NINO_VALUE", request.getNino());
    String response =
        applicationManagerWebClient
            .post(APPLICATION_MATCHER_PATH, String.class, requestJson)
            .block();
    log.info("Response received from ms-application-manager");
    return response;
  }

  private void setApplicationId(Identity.IdentityBuilder identity, String applicationId) {
    identity.applicationID(applicationId);
    identity.errorMessage("");
  }

  private Optional<Object> getApplicationId(String applicationIdResponse, JsonParser jsonParser) {
    if (StringUtils.isBlank(applicationIdResponse)) {
      return Optional.empty();
    }
    return Optional.ofNullable(jsonParser.parseMap(applicationIdResponse).get(APPLICATION_ID));
  }

  private void setErrorMessage(IdentityRequestUpdateSchemaV1 request,
                               Identity.IdentityBuilder identity) {
    String errorMessage =
        "Application ID not found for identity with id: " + request.getIdentityId();
    identity.errorMessage(errorMessage);
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

  @Override
  public Optional<Identity> getIdentityByApplicationId(String applicationId) {
    return repository.findByApplicationID(applicationId);
  }
}
