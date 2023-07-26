package uk.gov.dwp.health.pip.identity.service.impl;

import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.utils.DateParseUtil;

public class IdentityBuilder {
  public static Identity.IdentityBuilder createBuilder(IdentityRequestUpdateSchemaV1 request) {
    final var builder = Identity.builder()
        .subjectId(request.getSubjectId())
        .identityId(request.getIdentityId())
        .channel(request.getChannel().toString())
        .idvStatus(request.getIdvOutcome().toString())
        .nino(request.getNino())
        .dateTime(DateParseUtil.stringToDateTime(request.getTimestamp()));
    final var vot = request.getVot();
    if (vot != null) {
      builder.vot(vot.toString());
    }
    return builder;
  }

  public static Identity.IdentityBuilder createBuilderFromIdentity(Identity identity) {
    return Identity.builder()
        .id(identity.getId())
        .subjectId(identity.getSubjectId())
        .identityId(identity.getIdentityId())
        .dateTime(identity.getDateTime())
        .channel(identity.getChannel())
        .idvStatus(identity.getIdvStatus())
        .nino(identity.getNino())
        .applicationID(identity.getApplicationID())
        .errorMessage(identity.getErrorMessage())
        .vot(identity.getVot());
  }
}
