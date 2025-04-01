package uk.gov.dwp.health.pip.identity.api;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.utils.UrlBuilderUtil;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.dwp.health.pip.identity.config.MongoClientConnection.emptyMongoIdentityCollection;
import static uk.gov.dwp.health.pip.identity.config.MongoClientConnection.getMongoTemplate;

class IdentityRegisterApiIT extends ApiTest {
  private final UUID correlation = UUID.randomUUID();

  @BeforeEach
  void setup() {
    emptyMongoIdentityCollection();
  }

  @Test
  void shouldCreateNewIdentityForValidSubjectId() {
    // payload = {"sub": "identity_no_account@test.com", "vot": "P0.Cl.Cm", "iat": 1516239022, "guid":"023c3305534381c6cc37ec4edc7da2b1cf1c50a8596beb5da3a9aa585cfb0497"}
    String token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiAiaWRlbnRpdHlfbm9fYWNjb3VudEB0ZXN0LmNvbSIsICJ2b3QiOiAiUDAuQ2wuQ20iLCAiaWF0IjogMTUxNjIzOTAyMiwgImd1aWQiOiIwMjNjMzMwNTUzNDM4MWM2Y2MzN2VjNGVkYzdkYTJiMWNmMWM1MGE4NTk2YmViNWRhM2E5YWE1ODVjZmIwNDk3In0=" +
            ".c1ZwVhXCl93L7MhSmsPDbtLeo-dEzJoX7zvzxuEv0wU";

    RestAssured.given()
        .header("x-dwp-correlation-id", correlation)
        .header("x-id-token", token)
        .header("x-channel", "seeded")
        .filter(new AllureRestAssured())
        .when()
        .post(UrlBuilderUtil.identityRegister() + "?publish-guid-event=false")
        .then()
        .statusCode(201)
        .body(
            "ref",
            matchesPattern("^[a-zA-Z0-9]{24}$"),
            "applicationId",
            nullValue(),
            "subjectId",
            matchesPattern("(^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$)"));
  }

  @Test
  void shouldUpdateIdentityForExistingSubjectIdAtP0() {
    shouldUpdateIdentityForExistingSubjectId("P0.Cl.Cm");
  }

  @Test
  void shouldUpdateIdentityForExistingSubjectIdAtP2() {
    shouldUpdateIdentityForExistingSubjectId("P2.Cl.Cm");
  }

  void shouldUpdateIdentityForExistingSubjectId(final String vot) {
    seedIdentityInMongo("identity_no_account@test.com", vot);

    // payload = {"sub": "identity_no_account@test.com", "vot": "P2.Cl.Cm", "iat": 1516239022, "guid":"023c3305534381c6cc37ec4edc7da2b1cf1c50a8596beb5da3a9aa585cfb0497"}
    String token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiAiaWRlbnRpdHlfbm9fYWNjb3VudEB0ZXN0LmNvbSIsICJ2b3QiOiAiUDIuQ2wuQ20iLCAiaWF0IjogMTUxNjIzOTAyMiwgImd1aWQiOiIwMjNjMzMwNTUzNDM4MWM2Y2MzN2VjNGVkYzdkYTJiMWNmMWM1MGE4NTk2YmViNWRhM2E5YWE1ODVjZmIwNDk3In0=" +
            ".HwJ7fcipZRu8tA8wd15otSoXbRBg6emOtJR5Lj0R0cg";

    RestAssured.given()
        .header("x-dwp-correlation-id", correlation)
        .header("x-id-token", token)
        .filter(new AllureRestAssured())
        .when()
        .post(UrlBuilderUtil.identityRegister())
        .then()
        .statusCode(200)
        .body(
            "ref",
            matchesPattern("^[a-zA-Z0-9]{24}$"),
            "applicationId",
            nullValue(),
            "subjectId",
            matchesPattern("(^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$)"));
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
    // payload = {"sub": "identity_with_account@test.com", "vot": "P2.Cl.Cm", "iat": 1516239022, "guid":"5f19fa72-c6e2-4f51-a0ef-17052cd44e7d"}
    String token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiAiaWRlbnRpdHlfd2l0aF9hY2NvdW50QHRlc3QuY29tIiwgInZvdCI6ICJQMi5DbC5DbSIsICJpYXQiOiAxNTE2MjM5MDIyLCAiZ3VpZCI6IjVmMTlmYTcyLWM2ZTItNGY1MS1hMGVmLTE3MDUyY2Q0NGU3ZCJ9" +
            ".d6bqVnhRrojQtZ5tkOf-t0BFVVX7rMU5sK7L0wI-SIY";

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
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJub2lkZW50aXR5QH"
            + "Rlc3QuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ.rwCU85H-vpHPtvRSKOEhrfSw3b9DxbTpdRph4c8h-Lk";
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
  void shouldReturn200whenNoVotAndIdentityExistsAtP0() {
    shouldReturn200whenNoVotAndIdentityExists("P0.Cl.Cm");
  }

  @Test
  void shouldReturn200whenNoVotAndIdentityExistsAtP2() {
    shouldReturn200whenNoVotAndIdentityExists("P2.Cl.Cm");
  }

  void shouldReturn200whenNoVotAndIdentityExists(final String vot) {
    seedIdentityInMongo("abc@gmail.com", vot);
    // payload = {"sub": "abc@gmail.com", "iat": 1516239022, "guid":"023c3305534381c6cc37ec4edc7da2b1cf1c50a8596beb5da3a9aa585cfb0497"}
    String token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiAiYWJjQGdtYWlsLmNvbSIsICJpYXQiOiAxNTE2MjM5MDIyLCAiZ3VpZCI6IjAyM2MzMzA1NTM0MzgxYzZjYzM3ZWM0ZWRjN2RhMmIxY2YxYzUwYTg1OTZiZWI1ZGEzYTlhYTU4NWNmYjA0OTcifQ==" +
            ".hxyJgLgDMDhoxK1QTqiAB6WNYkDFfUD_az0RmFanu7g";
    RestAssured.given()
        .header("x-dwp-correlation-id", correlation)
        .header("x-id-token", token)
        .filter(new AllureRestAssured())
        .when()
        .post(UrlBuilderUtil.identityRegister())
        .then()
        .statusCode(200);
  }

  private void seedIdentityInMongo(final String subject, final String vot) {
    getMongoTemplate().insert(Identity.builder()
        .subjectId(subject)
        .identityId(UUID.randomUUID())
        .dateTime(LocalDateTime.of(2014, 7, 6, 12, 15, 10))
        .channel("seeded")
        .vot(vot)
        .build(), "identity");
  }
}
