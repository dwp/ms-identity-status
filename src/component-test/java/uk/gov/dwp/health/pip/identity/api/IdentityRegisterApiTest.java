package uk.gov.dwp.health.pip.identity.api;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.matchesPattern;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import java.util.UUID;
import org.junit.jupiter.api.*;
import uk.gov.dwp.health.pip.identity.config.MongoClientConnection;
import uk.gov.dwp.health.pip.identity.utils.UrlBuilderUtil;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IdentityRegisterApiTest extends ApiTest {
  private final UUID correlation = UUID.randomUUID();

  @BeforeAll
  static void beforeAll() {
    MongoClientConnection.emptyMongoCollection();
  }

  @Test
  @Order(1)
  void shouldCreateNewIdentityForValidSubjectId() {
    // payload = {"sub": "identity_no_account@test.com", "vot": "P0.Cl.Cm", "iat": 1516239022}
    String token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmNAZ21h" +
                "aWwuY29tIiwidm90IjoiUDAuQ2wuQ20iLCJpYXQiOjE1MTYyMzkwMjJ9.c1ZwVhXCl93L7MhSmsPDbtLeo-dEzJoX7zvzxuEv0wU";

    RestAssured.given()
            .header("x-dwp-correlation-id", correlation)
            .header("x-id-token", token)
            .header("x-channel", "seeded")
            .filter(new AllureRestAssured())
            .when()
            .post(UrlBuilderUtil.identityRegister())
            .then()
            .statusCode(201)
            .body("ref", matchesPattern("^[a-zA-Z0-9]{24}$"),
                    "applicationId", nullValue(),
                    "subjectId", matchesPattern("(^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$)"));
  }

  @Test
  void shouldUpdateIdentityForExistingSubjectId() {
    // payload = {"sub": "identity_no_account@test.com", "vot": "P2.Cl.Cm", "iat": 1516239022}
    String token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJpZGVudGl0eV9ub19hY2NvdW50QHRlc3QuY29tIiwidm90IjoiUDAuQ2wu" +
                    "Q20iLCJpYXQiOjE1MTYyMzkwMjJ9.HwJ7fcipZRu8tA8wd15otSoXbRBg6emOtJR5Lj0R0cg";

    RestAssured.given()
            .header("x-dwp-correlation-id", correlation)
            .header("x-id-token", token)
            .filter(new AllureRestAssured())
            .when()
            .post(UrlBuilderUtil.identityRegister())
            .then()
            .statusCode(200)
            .body("ref", matchesPattern("^[a-zA-Z0-9]{24}$"),
                    "applicationId", nullValue(),
                    "subjectId", matchesPattern("(^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$)"));
  }

  @Test
  void shouldReturnBadRequestForInvalidPayload() {
    // payload = {"sub": "abc", "vot": "P0.Cl.Cm", "iat": 1516239022}
    String token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmMiLCJ2b3Q"
                    + "iOiJQMC5DbC5DbSIsImlhdCI6MTUxNjIzOTAyMn0.NXOYfuiztqDdod_Bm1Hr69qZTBJeEbduSOgNpZLPAPM";
    RestAssured.given()
            .header("x-dwp-correlation-id", correlation)
            .header("x-id-token", token)
            .filter(new AllureRestAssured())
            .when()
            .post(UrlBuilderUtil.identityRegister())
            .then()
            .statusCode(400)
            .body(emptyString());
  }

  @Test
  void shouldConflictForExistingAccountWithEmail() {
    // payload = {"sub": "identity_with_account@test.com", "vot": "P2.Cl.Cm", "iat": 1516239022}
    String token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJpZGVudGl0eV93aXRoX2FjY291bnRAdGVzdC5jb20iLCJ2b3QiOiJQMC5D" +
                    "bC5DbSIsImlhdCI6MTUxNjIzOTAyMn0.d6bqVnhRrojQtZ5tkOf-t0BFVVX7rMU5sK7L0wI-SIY";

    RestAssured.given()
            .header("x-dwp-correlation-id", correlation)
            .header("x-id-token", token)
            .filter(new AllureRestAssured())
            .when()
            .post(UrlBuilderUtil.identityRegister())
            .then()
            .statusCode(409);
  }

  @Test
  void shouldReturn404whenNoVotAndNoIdentity() {
    // payload = {"sub": "noidentity@test.com", "iat": 1516239022}
    String token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJub2lkZW50aXR5QH" +
                    "Rlc3QuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ.rwCU85H-vpHPtvRSKOEhrfSw3b9DxbTpdRph4c8h-Lk";
    RestAssured.given()
            .header("x-dwp-correlation-id", correlation)
            .header("x-id-token", token)
            .filter(new AllureRestAssured())
            .when()
            .post(UrlBuilderUtil.identityRegister())
            .then()
            .statusCode(404)
            .body(emptyString());
  }

  @Test
  void shouldReturn200whenNoVotAndIdentityExists() {
    // payload = {"sub": "abc@gmail.com", "iat": 1516239022}
    String token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmNAZ21h"
            + "aWwuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ.hxyJgLgDMDhoxK1QTqiAB6WNYkDFfUD_az0RmFanu7g";
    RestAssured.given()
            .header("x-dwp-correlation-id", correlation)
            .header("x-id-token", token)
            .filter(new AllureRestAssured())
            .when()
            .post(UrlBuilderUtil.identityRegister())
            .then()
            .statusCode(200);
  }
}
