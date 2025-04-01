package uk.gov.dwp.health.pip.identity.api.patch;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.dwp.health.pip.identity.api.ApiTest;
import uk.gov.dwp.health.pip.identity.config.MongoClientConnection;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dwp.health.pip.identity.utils.UrlBuilderUtil.updateIdentityStatus;

public class UpdateIdentityStatusIT extends ApiTest {

  private static final String applicationID = "4bce57c491efc3ac3bc3e6f1";
  private static final String applicationIDMedium = "4bce57c491efc3ac3bc3e6f2";
  private static final String applicationIDNotFound = "4bce57c491efc3ac3bc3e6f3";

  @BeforeAll
  static void beforeAll() {
    MongoClientConnection.emptyMongoIdentityCollection();
    MongoClientConnection.getMongoTemplate()
        .save(
            Identity.builder()
                .id(applicationID)
                .applicationID(applicationID)
                .vot(IdentityRequestUpdateSchemaV1.Vot.P_0_CL_CM.toString())
                .nino("AA123456A")
                .build());
    MongoClientConnection.getMongoTemplate()
        .save(
            Identity.builder()
                .id(applicationIDMedium)
                .applicationID(applicationIDMedium)
                .vot(IdentityRequestUpdateSchemaV1.Vot.P_2_CL_CM.toString())
                .nino("AA123456A")
                .build());
  }

  Object body = "{\"staffId\": 12345678}";

  @Test
  void shouldReturn202ForSuccessfulIdentityStatusUpdate() {
    Response response = patchRequest(updateIdentityStatus(applicationID), body);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED.value());
  }

  @Test
  public void shouldReturn404StatusCodeWhenTheApplicationIdIsNotFound() {
    Response response = patchRequest(updateIdentityStatus(applicationIDNotFound), body);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
  }

  @Test
  public void shouldReturn423StatusCodeWhenConfidenceIsAlreadyAtMedium() {
    Response response = patchRequest(updateIdentityStatus(applicationIDMedium), body);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED.value());
  }
}
