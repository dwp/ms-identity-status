package uk.gov.dwp.health.pip.identity.service;

import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;

import java.util.Optional;

public interface IdentityService {
  Identity createIdentity(IdentityRequestUpdateSchemaV1 request);

  Optional<Identity> getIdentityBySubjectId(String subjectId);

  Optional<Identity> getIdentityByNino(String nino);
}
