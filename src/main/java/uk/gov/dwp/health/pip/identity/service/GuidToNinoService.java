package uk.gov.dwp.health.pip.identity.service;

import uk.gov.dwp.health.identity.status.openapi.model.NinoDto;

public interface GuidToNinoService {
  NinoDto getNinoFromGuid(String guid);
}
