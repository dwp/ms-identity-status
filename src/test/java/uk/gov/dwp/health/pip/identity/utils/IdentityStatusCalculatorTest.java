package uk.gov.dwp.health.pip.identity.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
public class IdentityStatusCalculatorTest {
  private static final String UNVERIFIED = "unverified";
  private static final String VERIFIED = "verified";
  private static final String ZERO_CONF = IdentityRequestUpdateSchemaV1.Vot.P_0_CL_CM.value();
  private static final String MEDIUM_CONF = IdentityRequestUpdateSchemaV1.Vot.P_2_CL_CM.value();
  private static final String LOW_CONF = IdentityRequestUpdateSchemaV1.Vot.P_1_CL_CM.value();
  private static final String NINO = "RN000001A";

  private static Stream<Arguments> provideParameters() {
    return Stream.of(
        Arguments.of(UNVERIFIED,null,null,UNVERIFIED),
        Arguments.of(VERIFIED,null,null,VERIFIED),
        Arguments.of(null,ZERO_CONF,null,UNVERIFIED),
        Arguments.of(null,LOW_CONF,null,UNVERIFIED),
        Arguments.of(null,MEDIUM_CONF,null,UNVERIFIED),
        Arguments.of(null,MEDIUM_CONF,NINO,VERIFIED)
    );
  }
  @ParameterizedTest(name = "calculateIdentityStatus should give {3} when given an Identity object with idvStatus = {0}, vot = {1} and nino = {2}")
  @MethodSource("provideParameters")
  void calculateIdentityStatus(String idvStatus,String vot,String nino,String expected){
    Identity identity = new Identity("id",
        "subject",
        UUID.randomUUID(),
        LocalDateTime.now(),
        "channel",
        idvStatus,
        nino,
        "application",
        "error",
        vot);
    final var value = IdentityStatusCalculator.fromIdentity(identity);
    assertThat(value).isEqualTo(expected);
  }
}

