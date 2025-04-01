package uk.gov.dwp.health.pip.identity.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.identity.status.openapi.model.NinoDto;
import uk.gov.dwp.health.pip.identity.model.IdentifierDto;
import uk.gov.dwp.health.pip.identity.service.GuidToNinoService;
import uk.gov.dwp.health.pip.identity.webclient.GuidServiceClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuidToNinoServiceImpl implements GuidToNinoService {

  private final GuidServiceClient guidServiceClient;

  @Override
  public NinoDto getNinoFromGuid(String guid) {
    IdentifierDto identifierDto = guidServiceClient.getNinoFromGuid(guid);
    return transFormIdentifierToNino(identifierDto);
  }

  private NinoDto transFormIdentifierToNino(IdentifierDto identifierDto) {
    NinoDto ninoDto = new NinoDto();
    ninoDto.setNino(identifierDto.getIdentifier());
    return ninoDto;
  }

}
