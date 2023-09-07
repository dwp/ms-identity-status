package uk.gov.dwp.health.pip.identity.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;
import uk.gov.dwp.health.pip.identity.exception.IdentityNotFoundException;
import uk.gov.dwp.health.pip.identity.exception.NoKeyChangesToExistingRecordException;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.repository.IdentityRepository;
import uk.gov.dwp.health.pip.identity.service.IdentityService;
import uk.gov.dwp.health.pip.identity.utils.DateParseUtil;
import uk.gov.dwp.health.pip.identity.webclient.ApplicationManagerWebClient;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityServiceImpl implements IdentityService {

  private final IdentityRepository repository;

  private final ApplicationManagerWebClient applicationManagerWebClient;

  @Override
  public Identity createIdentity(IdentityRequestUpdateSchemaV1 request) {

    log.debug("create identity request received {} ", request.getIdentityId());
    final Optional<Identity> ninoRecord = getIdentityByNino(request.getNino());

    if (ninoRecord.isPresent()) {
      Identity recordToUpdate = ninoRecord.get();
      if (isKeyInformationSame(recordToUpdate, request)) {
        throw new NoKeyChangesToExistingRecordException(
            "Key information not changed on existing NINO record");
      }

      return updateExistingNinoRecord(request, recordToUpdate);
    }

    final Optional<Identity> subjectRecord = getIdentityBySubjectId(request.getSubjectId());
    if (subjectRecord.isPresent()) {
      Identity recordToUpdate = subjectRecord.get();
      if (isKeyInformationSame(recordToUpdate, request)) {
        throw new NoKeyChangesToExistingRecordException(
            "Key information not changed on existing Subject record");
      }

      return updateExistingSubjectRecord(request, recordToUpdate);
    }

    Identity.IdentityBuilder builder = IdentityBuilder.createBuilder(request);
    processApplicationId(request, builder);
    return saveIdentity(builder);
  }

  private Identity saveIdentity(Identity.IdentityBuilder builder) {
    Identity identity = builder.build();
    return repository.save(identity);
  }

  @Override
  public void updateApplicationId(String identityId, String applicationId) {
    var identity = repository.findById(identityId);
    if (identity.isEmpty()) {
      throw new IdentityNotFoundException(
              String.format("No identity found for given identity id %s", identityId));
    }
    identity.get().setApplicationID(applicationId);
    log.info("Application id: {}, has been set for Identity: {}", applicationId, identityId);
    repository.save(identity.get());
  }

  private Identity updateExistingNinoRecord(
      IdentityRequestUpdateSchemaV1 request, Identity ninoRecord) {
    var builder = IdentityBuilder.createBuilderFromIdentity(ninoRecord);
    setBaseIdentityFields(builder, request);
    processApplicationIdIfNoneExists(request, ninoRecord, builder);
    builder.subjectId(request.getSubjectId());
    return saveIdentity(builder);
  }

  private Identity updateExistingSubjectRecord(
      IdentityRequestUpdateSchemaV1 request, Identity subjectRecord) {
    var builder = IdentityBuilder.createBuilderFromIdentity(subjectRecord);
    setBaseIdentityFields(builder, request);
    processApplicationIdIfNoneExists(request, subjectRecord, builder);
    builder.nino(request.getNino());
    return saveIdentity(builder);
  }

  private boolean isKeyInformationSame(Identity record, IdentityRequestUpdateSchemaV1 request) {
    final var newVotValue = request.getVot() == null ? "" : request.getVot().value();
    final var existingVotValue = record.getVot() == null ? "" : record.getVot();

    return (areValuesEqual(record.getSubjectId(), request.getSubjectId())
        && areValuesEqual(existingVotValue, newVotValue)
        && areValuesEqual(record.getNino(), request.getNino()));
  }

  private boolean areValuesEqual(String originalRecordValue, String newRecordValue) {
    return StringUtils.equals(originalRecordValue, newRecordValue);
  }

  private void setBaseIdentityFields(
      Identity.IdentityBuilder builder, IdentityRequestUpdateSchemaV1 request) {
    builder.channel(request.getChannel().toString());
    builder.identityId(request.getIdentityId());
    builder.idvStatus(request.getIdvOutcome().toString());
    builder.dateTime(DateParseUtil.stringToDateTime(request.getTimestamp()));

    var vot = request.getVot();
    if (vot != null) {
      builder.vot(vot.value());
    }
  }

  private void processApplicationIdIfNoneExists(
      IdentityRequestUpdateSchemaV1 request, Identity record, Identity.IdentityBuilder builder) {
    if (StringUtils.isBlank(record.getApplicationID())
        || !areValuesEqual(request.getNino(), record.getNino())) {
      processApplicationId(request, builder);
    } else {
      log.info(
              "Existing record {} has no key changes around Application ID,"
                      + " therefore not calling Application Manager",
              record.getIdentityId());
    }
  }

  private void processApplicationId(
      IdentityRequestUpdateSchemaV1 request, Identity.IdentityBuilder identity) {
    try {
      Optional<Object> optionalApplicationId = getApplicationId(request.getNino());
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

  private void handleConflict(
      IdentityRequestUpdateSchemaV1 request, Identity.IdentityBuilder identity) {
    String errorMessage =
        "Multiple application IDs found for a nino while processing the request for identity id: "
            + request.getIdentityId();
    identity.errorMessage(errorMessage);
    log.warn(errorMessage);
  }

  private void setApplicationId(Identity.IdentityBuilder identity, String applicationId) {
    identity.applicationID(applicationId);
    identity.errorMessage("");
  }

  private Optional<Object> getApplicationId(String nino) {
    return applicationManagerWebClient.getApplicationId(nino);
  }

  private void setErrorMessage(
      IdentityRequestUpdateSchemaV1 request, Identity.IdentityBuilder identity) {
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
