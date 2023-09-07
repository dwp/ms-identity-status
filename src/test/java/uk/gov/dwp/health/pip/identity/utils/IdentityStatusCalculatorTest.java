package uk.gov.dwp.health.pip.identity.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.pip.identity.entity.Identity;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
public class IdentityStatusCalculatorTest {
  private static Stream<Arguments> provideParameters() {
    return Stream.of(
        Arguments.of("unverified", null,"unverified"),
        Arguments.of("verified",null, "verified"),
        Arguments.of("verified","P0.Cl.Cm","unverified"),
        Arguments.of("verified","P1.Cl.Cm","unverified"),
        Arguments.of("verified","P2.Cl.Cm","verified")
    );
  }
  @ParameterizedTest(name = "calculateIdentityStatus should give {2} when given an Identity object with idvStatus = {0} and vot = {1}")
  @MethodSource("provideParameters")
  void calculateIdentityStatus(String idvStatus,String vot,String expected){
    Identity identity = new Identity("id",
        "subject",
        UUID.randomUUID(),
        LocalDateTime.now(),
        "channel",
        idvStatus,
        "nino",
        "application",
        "error",
        vot);
    final var value = IdentityStatusCalculator.fromIdentity(identity);
    assertThat(value).isEqualTo(expected);
  }
}

