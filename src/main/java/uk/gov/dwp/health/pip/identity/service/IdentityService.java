package uk.gov.dwp.health.pip.identity.service;

import java.util.Optional;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;

public interface IdentityService {
  Identity createIdentity(IdentityRequestUpdateSchemaV1 request);

  Optional<Identity> getIdentityBySubjectId(String subjectId);

  Optional<Identity> getIdentityByNino(String nino);

  Optional<Identity> getIdentityByApplicationId(String applicationId);

  void updateApplicationId(String identityId, String applicationId);
}
