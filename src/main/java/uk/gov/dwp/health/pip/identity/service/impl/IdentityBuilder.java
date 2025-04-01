package uk.gov.dwp.health.pip.identity.service.impl;

import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.utils.DateParseUtil;

import java.util.UUID;

public class IdentityBuilder {
  public static Identity.IdentityBuilder createBuilder(IdentityRequestUpdateSchemaV1 request) {
    final var builder = Identity.builder()
        .subjectId(request.getSubjectId())
        .identityId(request.getIdentityId() == null ? UUID.randomUUID() : request.getIdentityId())
        .channel(request.getChannel().toString())
        .nino(request.getNino())
        .dateTime(DateParseUtil.stringToDateTime(request.getTimestamp()));
    final var vot = request.getVot();
    final var idvOutcome = request.getIdvOutcome();
    if (vot == null) {
      builder.idvStatus(idvOutcome.toString());
    } else {
      builder.vot(vot.toString());
    }
    return builder;
  }

  public static Identity.IdentityBuilder createBuilderFromIdentity(Identity identity) {
    return Identity.builder()
        .id(identity.getId())
        .subjectId(identity.getSubjectId())
        .identityId(identity.getIdentityId() == null ? UUID.randomUUID() : identity.getIdentityId())
        .dateTime(identity.getDateTime())
        .channel(identity.getChannel())
        .idvStatus(identity.getIdvStatus())
        .nino(identity.getNino())
        .applicationID(identity.getApplicationID())
        .errorMessage(identity.getErrorMessage())
        .vot(identity.getVot());
  }
}
