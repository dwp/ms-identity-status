package uk.gov.dwp.health.pip.identity.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.identity.status.openapi.model.GuidDto;
import uk.gov.dwp.health.pip.identity.exception.ValidationException;
import uk.gov.dwp.health.pip.identity.model.IdentifierDto;
import uk.gov.dwp.health.pip.identity.service.NinoToGuidService;
import uk.gov.dwp.health.pip.identity.webclient.GuidServiceClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class NinoToGuidServiceImpl implements NinoToGuidService {

  private final GuidServiceClient guidServiceClient;

  @Override
  public GuidDto getGuidFromNino(String nino) {
    if (nino == null || nino.isEmpty()) {
      throw new ValidationException("Nino not present");
    }

    IdentifierDto identifierDto = guidServiceClient.getGuidFromNino(nino);
    return transformIdentifierToGuid(identifierDto);
  }

  private GuidDto transformIdentifierToGuid(IdentifierDto identifierDto) {
    GuidDto guidDto = new GuidDto();
    guidDto.setGuid(identifierDto.getIdentifier());
    return guidDto;
  }
}
