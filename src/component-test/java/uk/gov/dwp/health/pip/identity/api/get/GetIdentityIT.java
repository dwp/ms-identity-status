package uk.gov.dwp.health.pip.identity.api.get;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.dwp.health.pip.identity.api.ApiTest;
import uk.gov.dwp.health.pip.identity.config.MongoClientConnection;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.utils.UrlBuilderUtil;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class GetIdentityIT extends ApiTest {

  private final UUID identityId = UUID.randomUUID();

  @BeforeEach
  void testSetup() {
    MongoClientConnection.emptyMongoCollection();
  }

  @Test
  void getIdvStatusBySubjectId_shouldReturn404StatusCodeIfIdentityNotFound() {
    String subjectId = "test@dwp.gov.uk";

    Response response =
        getRequestWithHeader(UrlBuilderUtil.getIdvStatusBySubjectUrl(), "X-Subject-ID", subjectId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  void getIdvStatusBySubjectId_shouldReturn200IfIdentityFound() {

    MongoClientConnection.getMongoTemplate()
        .save(
            new Identity(
                "1",
                "test@dwp.gov.uk",
                identityId,
                LocalDateTime.now().minusMinutes(10),
                "oidv",
                "verified",
                "RN000004A",
                "application123",
                ""));

    String subjectId = "test@dwp.gov.uk";

    Response response =
        getRequestWithHeader(UrlBuilderUtil.getIdvStatusBySubjectUrl(), "X-Subject-ID", subjectId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.andReturn().getBody().prettyPrint()).contains("verified");
  }

  @Test
  void shouldReturn404StatusCodeIfIdentityNotFound() {
    String nino = "RN000004A";

    Response response =
        getRequestWithHeader(UrlBuilderUtil.getIdvStatusByNinoUrl(), "X-Nino", nino);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  void shouldReturn200IfIdentityFound() {

    MongoClientConnection.getMongoTemplate()
        .save(
            new Identity(
                "2",
                "test@dwp.gov.uk",
                identityId,
                LocalDateTime.now().minusMinutes(10),
                "oidv",
                "verified",
                "RN000004A",
                "application123",
                ""));

    Response response =
        getRequestWithHeader(UrlBuilderUtil.getIdvStatusByNinoUrl(), "X-Nino", "RN000004A");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.andReturn().getBody().prettyPrint()).contains("verified");
  }
}
