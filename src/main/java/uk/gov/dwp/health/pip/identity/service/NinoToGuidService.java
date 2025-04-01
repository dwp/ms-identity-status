package uk.gov.dwp.health.pip.identity.service;

import uk.gov.dwp.health.identity.status.openapi.model.GuidDto;

public interface NinoToGuidService {
  GuidDto getGuidFromNino(String nino);
}
