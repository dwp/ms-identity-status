package uk.gov.dwp.health.pip.identity.api.get;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import uk.gov.dwp.health.pip.identity.api.ApiTest;
import uk.gov.dwp.health.pip.identity.config.MongoClientConnection;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.utils.UrlBuilderUtil;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class GetIdentityIT extends ApiTest {

  private final UUID identityId = UUID.randomUUID();

  @BeforeEach
  void testSetup() {
    MongoClientConnection.emptyMongoIdentityCollection();
  }

  @Test
  void getIdvStatusBySubjectId_shouldReturn404StatusCodeIfIdentityNotFound() {
    String subjectId = "test@dwp.gov.uk";

    Response response =
            getRequestWithHeader(UrlBuilderUtil.getIdvStatusBySubjectUrl(), "X-Subject-ID", subjectId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  void getIdvStatusByNino_shouldReturn404StatusCodeIfIdentityNotFound() {
    String nino = "RN000004A";

    Response response =
            getRequestWithHeader(UrlBuilderUtil.getIdvStatusByNinoUrl(), "X-Nino", nino);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  private static Stream<Arguments> provideVotAndIdvStatus() {
    return Stream.of(
        Arguments.of("P0.Cl.Cm", "unverified", "unverified"),
        Arguments.of("P1.Cl.Cm", "unverified", "unverified"),
        Arguments.of("P2.Cl.Cm", "unverified", "verified"),
        Arguments.of("P0.Cl.Cm", "verified","unverified"),
        Arguments.of("P1.Cl.Cm", "verified","unverified"),
        Arguments.of("P2.Cl.Cm", "verified", "verified"),
        Arguments.of(null, "verified", "verified"),
        Arguments.of(null, "unverified", "unverified")
    );
  }

  @ParameterizedTest
  @MethodSource("provideVotAndIdvStatus")
  public void getIdvStatusByNinoFor_vot_idvStatus_shouldReturn200IfIdentityFound(
      String votValue,
      String currentIdvStatus,
      String expectedIdvStatus
  ) {
    final String nino ="AB000000B";
    MongoClientConnection.getMongoTemplate()
            .save(
                    new Identity(
                            "1",
                            "testNino@dwp.gov.uk",
                            identityId,
                            LocalDateTime.now().minusMinutes(10),
                            "oidv",
                            currentIdvStatus,
                            nino,
                            "application123",
                            "",
                            votValue));

      Response response =
              getRequestWithHeader(UrlBuilderUtil.getIdvStatusByNinoUrl(), "X-Nino", nino);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
      assertThat(response.andReturn().getBody().prettyPrint()).contains(expectedIdvStatus);
    }

  @ParameterizedTest
  @MethodSource("provideVotAndIdvStatus")
  public void getIdvStatusBySubjectIdFor_vot_idvStatus_shouldReturn200IfIdentityFound(
      String votValue,
      String currentIdvStatus,
      String expectedIdvStatus
  ) {
    final String subjectId = "test_subjectId@dwp.gov.uk";
    MongoClientConnection.getMongoTemplate()
            .save(
                    new Identity(
                            "507f1f77bcf86cd799439011",
                            subjectId,
                            identityId,
                            LocalDateTime.now().minusMinutes(10),
                            "oidv",
                            currentIdvStatus,
                            "AB000000B",
                            "application123",
                            "",
                            votValue));

    Response response =
            getRequestWithHeader(UrlBuilderUtil.getIdvStatusBySubjectUrl(),"X-Subject-ID", subjectId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.andReturn().getBody().prettyPrint()).contains(expectedIdvStatus);
  }
}
