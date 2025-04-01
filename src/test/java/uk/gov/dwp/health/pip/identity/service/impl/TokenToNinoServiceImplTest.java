package uk.gov.dwp.health.pip.identity.service.impl;

import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.identity.status.openapi.model.NinoDto;
import uk.gov.dwp.health.pip.identity.constants.Constants;
import uk.gov.dwp.health.pip.identity.exception.ValidationException;
import uk.gov.dwp.health.pip.identity.model.IdentifierDto;
import uk.gov.dwp.health.pip.identity.model.TokenPayload;
import uk.gov.dwp.health.pip.identity.webclient.GuidServiceClient;

@ExtendWith(MockitoExtension.class)
public class TokenToNinoServiceImplTest {

  static String sampleGuid = "12345678abcdabcd12345678abcdabcd12345678abcdabcd12345678abcdabcd";
  static String sampleSub = "test_email@test.com";
  private static ObjectMapper objectMapper = new ObjectMapper();
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
  String sampleNino = "RN000008A";
  IdentifierDto identifierDto;
  private TokenToNinoServiceImpl tokenToNinoServiceImpl;
  @Mock private GuidServiceClient guidServiceClient;

  private static String getBasicToken(String guid, String sub, TokenPayload.VotEnum vot) {
    try {
      TokenPayload payload = new TokenPayload();
      payload.setGuid(sampleGuid);

      payload.setSub(sub);
      payload.setVot(vot);
      return objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      throw new RuntimeException("Exception occurred while getting basic token", e);
    }
  }

  public static Stream<Arguments> getPayloadAndMessageMap() {
    return Stream.of(
        Arguments.of(
            getBasicToken(sampleGuid, null, TokenPayload.VotEnum.P0_CL_CM), "Invalid payload"),
        Arguments.of(
            getBasicToken(null, sampleSub, TokenPayload.VotEnum.P2_CL_CM), "Guid not present"),
        Arguments.of("invalidToken", "Unable to parse the token"));
  }

  @BeforeEach
  void setUp() {
    tokenToNinoServiceImpl = new TokenToNinoServiceImpl(objectMapper, validator, guidServiceClient);
    identifierDto = IdentifierDto.builder().identifier(sampleNino).type(Constants.NINO).build();
  }

  @Test
  void shouldReturnNinoForValidToken() {
    String payload = getBasicToken(sampleGuid, sampleSub, TokenPayload.VotEnum.P0_CL_CM);
    when(guidServiceClient.getNinoFromGuid(sampleGuid)).thenReturn(identifierDto);

    NinoDto ninoFromToken = tokenToNinoServiceImpl.getNinoFromToken(payload);

    Assertions.assertEquals(ninoFromToken.getNino(), sampleNino);
  }

  @ParameterizedTest
  @MethodSource("getPayloadAndMessageMap")
  void shouldThrowValidationExceptionIfSubjectInvalid(String payload, String message) {
    ValidationException validationException =
        Assertions.assertThrows(
            ValidationException.class, () -> tokenToNinoServiceImpl.getNinoFromToken(payload));
    Assertions.assertEquals(validationException.getMessage(), message);
  }
}
