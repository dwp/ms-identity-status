package uk.gov.dwp.health.pip.identity.api;

import static org.hamcrest.Matchers.equalTo;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import uk.gov.dwp.health.pip.identity.config.MongoClientConnection;
import uk.gov.dwp.health.pip.identity.utils.UrlBuilderUtil;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IdentityApiIT extends ApiTest {
  private final UUID correlation = UUID.randomUUID();

  @BeforeAll
  static void beforeAll() {
    MongoClientConnection.emptyMongoIdentityCollection();
  }

  @Test
  void guidInTokenToNinoShouldReturn200WhenGuidServiceCalled() {
    // payload =
    // {"sub":"identity_no_account@test.com","iat":1516239022,"vot":"P0.Cl.Cm","guid":"023c3305534381c6cc37ec4edc7da2b1cf1c50a8596beb5da3a9aa585cfb0497"}
    String token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJpZGVudGl0eV9ub19hY2NvdW50QHRlc3QuY29tIiwiaWF0IjoxNT"
            + "E2MjM5MDIyLCJ2b3QiOiJQMC5DbC5DbSIsImd1aWQiOiIwMjNjMzMwNTUzNDM4M"
            + "WM2Y2MzN2VjNGVkYzdkYTJiMWNmMWM1MGE4NTk2YmViNWRhM2E5YWE1ODVjZmIwNDk3In0="
            + ".-IZ-7HqY8ONbQ3TPJQ44ahXxBS84cw1nh-j0X70jeWA";

    RestAssured.given()
        .header("x-dwp-correlation-id", correlation)
        .header("x-id-token", token)
        .filter(new AllureRestAssured())
        .when()
        .get(UrlBuilderUtil.getIdentityDetailsUrl())
        .then()
        .statusCode(200)
        .body("nino", equalTo("RN000000A"));
  }

  @Test
  void guidToNinoShouldReturn400WhenGuidNotPresentInToken() {
    // payload =
    // payload = {"sub": "identity_no_account@test.com", "vot": "P0.Cl.Cm", "iat": 1516239022}

    String token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmNAZ21h"
            + "aWwuY29tIiwidm90IjoiUDAuQ2wuQ20iLCJpYXQiOjE1MTYyMzkwMjJ9.c1ZwVhXCl93L7MhSmsPDbtLeo-dEzJoX7zvzxuEv0wU";

    RestAssured.given()
        .header("x-dwp-correlation-id", correlation)
        .header("x-id-token", token)
        .filter(new AllureRestAssured())
        .when()
        .get(UrlBuilderUtil.getIdentityDetailsUrl())
        .then()
        .statusCode(400);
  }

  @Test
  void guidToNinoShouldReturn400WhenGuidIsInvalidNotPresentInToken() {
    // payload =
    // payload = {"sub": "identity_no_account@test.com", "vot": "P0.Cl.Cm", "iat": 1516239022}

    String token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmNAZ21h"
            + "aWwuY29tIiwidm90IjoiUDAuQ2wuQ20iLCJpYXQiOjE1MTYyMzkwMjJ9.c1ZwVhXCl93L7MhSmsPDbtLeo-dEzJoX7zvzxuEv0wU";

    RestAssured.given()
        .header("x-dwp-correlation-id", correlation)
        .header("x-id-token", token)
        .filter(new AllureRestAssured())
        .when()
        .get(UrlBuilderUtil.getIdentityDetailsUrl())
        .then()
        .statusCode(400);
  }

  @Test
  void guidToNinoShouldReturn500WhenDWPGuidIDServiceReturnsError() {
    // payload =
    // payload = {"sub": "identity_no_account@test.com", "vot": "P0.Cl.Cm", "iat": 1516239022,
    // "guid": "adasd"}

    String token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmNAZ21haWwuY29tIiwidm90IjoiUDAuQ2wuQ20iLCJpYXQiOjE1MTYyMzkwMjIsImd1aWQiOiAiYWRhc2QifQo=.c1ZwVhXCl93L7MhSmsPDbtLeo-dEzJoX7zvzxuEv0wU";

    RestAssured.given()
        .header("x-dwp-correlation-id", correlation)
        .header("x-id-token", token)
        .filter(new AllureRestAssured())
        .when()
        .get(UrlBuilderUtil.getIdentityDetailsUrl())
        .then()
        .statusCode(500);
  }

  @Test
  void guidToNinoShouldReturn200AfterRetry() {

    // payload =
    // payload = {"sub": "identity_no_account@test.com", "vot": "P0.Cl.Cm", "iat": 1516239022,
    // "guid": "3a0986f1318bdd9393f4b18a7366aa07274436dd5ad30c1150ad82d520d5a318"}

    String token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJrYXJ0aGlrLm1lbm9uQGVuZ2luZWVyaW5nLmRpZ2l0YWwuZHdwLmdvdi51ayIsImlhdCI6MTUxNjIzOTAyMiwidm90IjoiUDAuQ2wuQ20iLCAiZ3VpZCI6ICIzYTA5ODZmMTMxOGJkZDkzOTNmNGIxOGE3MzY2YWEwNzI3NDQzNmRkNWFkMzBjMTE1MGFkODJkNTIwZDVhMzE4In0=.c1ZwVhXCl93L7MhSmsPDbtLeo-dEzJoX7zvzxuEv0wU";

    RestAssured.given()
            .header("x-dwp-correlation-id", correlation)
            .header("x-id-token", token)
            .filter(new AllureRestAssured())
            .when()
            .get(UrlBuilderUtil.getIdentityDetailsUrl())
            .then()
            .statusCode(200)
            .body("nino", equalTo("RN000000A"));;
  }


  @Test
  void guidToNinoShouldReturn500AfterRetry() {

    // payload =
    // payload = {"sub": "identity_no_account@test.com", "vot": "P0.Cl.Cm", "iat": 1516239022,
    // "guid": "66dec4674d2c0c9a12a51ce74c785ad68bfc90c15b0b78573c0581398677d094"}

    String token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJpZGVudGl0eV9ub19hY2NvdW50QHRlc3QuY29tIiwiaWF0IjoxNTE2MjM5MDIyLCJ2b3QiOiJQMC5DbC5DbSIsICJndWlkIjogIjY2ZGVjNDY3NGQyYzBjOWExMmE1MWNlNzRjNzg1YWQ2OGJmYzkwYzE1YjBiNzg1NzNjMDU4MTM5ODY3N2QwOTQifQ==.c1ZwVhXCl93L7MhSmsPDbtLeo-dEzJoX7zvzxuEv0wU";

    RestAssured.given()
            .header("x-dwp-correlation-id", correlation)
            .header("x-id-token", token)
            .filter(new AllureRestAssured())
            .when()
            .get(UrlBuilderUtil.getIdentityDetailsUrl())
            .then()
            .statusCode(500);
  }

  @Test
  void ninoToGuidShouldReturn200WhenGuidServiceCalled() {
    String nino = "RN000000A";

    RestAssured.given()
        .header("x-dwp-correlation-id", correlation)
        .header("nino", nino)
        .filter(new AllureRestAssured())
        .when()
        .get(UrlBuilderUtil.getNinoToGuidUrl())
        .then()
        .statusCode(200)
        .body("guid", equalTo("12345678abcdabcd12345678abcdabcd12345678abcdabcd12345678abcdabcd"));
  }

  @Test
  void ninoToGuidShouldReturn400WhenNinoIsEmpty() {
    String nino = "";

    RestAssured.given()
        .header("x-dwp-correlation-id", correlation)
        .header("nino", nino)
        .filter(new AllureRestAssured())
        .when()
        .get(UrlBuilderUtil.getNinoToGuidUrl())
        .then()
        .statusCode(400);
  }

  @Test
  void ninoToGuidShouldReturn400WhenNinoHeaderIsMissing() {
    RestAssured.given()
        .header("x-dwp-correlation-id", correlation)
        .filter(new AllureRestAssured())
        .when()
        .get(UrlBuilderUtil.getNinoToGuidUrl())
        .then()
        .statusCode(400);
  }

  @Test
  void ninoToGuidShouldReturn500WhenDWPGuidIDServiceReturnsError() {
    String nino = "----";

    RestAssured.given()
        .header("x-dwp-correlation-id", correlation)
        .header("nino", nino)
        .filter(new AllureRestAssured())
        .when()
        .get(UrlBuilderUtil.getNinoToGuidUrl())
        .then()
        .statusCode(500);
  }

  @Test
  void guidParamToNinoShouldReturn200WhenGuidServiceCalled() {
    String guid = "12345678abcdabcd12345678abcdabcd12345678abcdabcd12345678abcdabcd";

    RestAssured.given()
        .header("x-dwp-correlation-id", correlation)
        .filter(new AllureRestAssured())
        .when()
        .get(UrlBuilderUtil.getGuidToNinoUrl(guid))
        .then()
        .statusCode(200)
        .body("nino", equalTo("AB123456C"));
  }

  @Test
  void guidToNinoShouldReturn400WhenGuidInvalidFormat() {
    String guid = "4321";

    RestAssured.given()
        .header("x-dwp-correlation-id", correlation)
        .filter(new AllureRestAssured())
        .when()
        .get(UrlBuilderUtil.getGuidToNinoUrl(guid))
        .then()
        .statusCode(400);
  }


  @Test
  void guidToNinoCallShouldReturn500WhenDWPGuidIDServiceReturnsError() {
    String guid = "12345678abcdabcd12345678abcdabcd12345678abcdabcd123456789c0de500";

    RestAssured.given()
        .header("x-dwp-correlation-id", correlation)
        .filter(new AllureRestAssured())
        .when()
        .get(UrlBuilderUtil.getGuidToNinoUrl(guid))
        .then()
        .statusCode(500);
  }
}
