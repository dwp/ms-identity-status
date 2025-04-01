package uk.gov.dwp.health.pip.identity.service.impl;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.identity.status.openapi.model.NinoDto;
import uk.gov.dwp.health.pip.identity.constants.Constants;

import uk.gov.dwp.health.pip.identity.model.IdentifierDto;
import uk.gov.dwp.health.pip.identity.webclient.GuidServiceClient;

@ExtendWith(MockitoExtension.class)
  class GuidToNinoServiceImplTest {
  static String sampleGuid = "12345678abcdabcd12345678abcdabcd12345678abcdabcd12345678abcdabcd";
  String sampleNino = "RN000008A";
  IdentifierDto identifierDto;
  private GuidToNinoServiceImpl guidToNinoServiceImpl;
  @Mock
  private GuidServiceClient guidServiceClient;

  @BeforeEach
  void setUp() {
    guidToNinoServiceImpl = new GuidToNinoServiceImpl(guidServiceClient);
    identifierDto = IdentifierDto.builder().identifier(sampleNino).type(Constants.NINO).build();
  }

  @Test
  void shouldReturnNinoForValidGuid() {
    when(guidServiceClient.getNinoFromGuid(sampleGuid)).thenReturn(identifierDto);

    NinoDto ninoFromToken = guidToNinoServiceImpl.getNinoFromGuid(sampleGuid);

    Assertions.assertEquals(ninoFromToken.getNino(), sampleNino);
  }

}