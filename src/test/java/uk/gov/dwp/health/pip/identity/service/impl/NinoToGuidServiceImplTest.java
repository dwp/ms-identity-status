package uk.gov.dwp.health.pip.identity.service.impl;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.identity.status.openapi.model.GuidDto;
import uk.gov.dwp.health.pip.identity.constants.Constants;
import uk.gov.dwp.health.pip.identity.exception.ValidationException;
import uk.gov.dwp.health.pip.identity.model.IdentifierDto;
import uk.gov.dwp.health.pip.identity.webclient.GuidServiceClient;

@ExtendWith(MockitoExtension.class)
public class NinoToGuidServiceImplTest {

  static String sampleGuid = "12345678abcdabcd12345678abcdabcd12345678abcdabcd12345678abcdabcd";
  String sampleNino = "RN000008A";
  IdentifierDto identifierDto;
  private NinoToGuidServiceImpl ninoToGuidServiceImpl;
  @Mock private GuidServiceClient guidServiceClient;

  @BeforeEach
  void setUp() {
    ninoToGuidServiceImpl = new NinoToGuidServiceImpl(guidServiceClient);
    identifierDto = IdentifierDto.builder().identifier(sampleGuid).type(Constants.DWP_GUID).build();
  }

  @Test
  void shouldReturnGuidForValidNino() {
    when(guidServiceClient.getGuidFromNino(sampleNino)).thenReturn(identifierDto);

    GuidDto ninoFromToken = ninoToGuidServiceImpl.getGuidFromNino(sampleNino);

    Assertions.assertEquals(ninoFromToken.getGuid(), sampleGuid);
  }

  @Test
  void shouldThrowValidationExceptionIfNinoMissing() {
    ValidationException validationException =
        Assertions.assertThrows(
            ValidationException.class, () -> ninoToGuidServiceImpl.getGuidFromNino(null));
    Assertions.assertEquals(validationException.getMessage(), "Nino not present");
  }
}
