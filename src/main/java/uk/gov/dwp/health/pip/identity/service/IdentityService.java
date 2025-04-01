package uk.gov.dwp.health.pip.identity.service;

import uk.gov.dwp.health.identity.status.openapi.model.UpliftDto;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.model.IdvAgentUpliftOutcome;

import java.util.Optional;

public interface IdentityService {

  Identity recordUpliftedIdentity(IdentityRequestUpdateSchemaV1 request);

  Optional<Identity> getIdentityBySubjectId(String subjectId);

  Optional<Identity> getIdentityByNino(String nino);

  Optional<Identity> getIdentityByApplicationId(String applicationId);

  Optional<Identity> getIdentityById(String id);

  void updateApplicationId(String identityId, String applicationId);

  IdvAgentUpliftOutcome upliftIdentityStatusByAgent(String applicationId, UpliftDto upliftDto);
}
