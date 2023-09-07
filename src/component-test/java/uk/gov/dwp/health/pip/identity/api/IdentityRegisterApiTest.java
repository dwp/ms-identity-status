package uk.gov.dwp.health.pip.identity.api;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.matchesPattern;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import uk.gov.dwp.health.pip.identity.config.MongoClientConnection;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.utils.UrlBuilderUtil;

class IdentityRegisterApiTest extends ApiTest {
  private final UUID correlation = UUID.randomUUID();

  @BeforeAll
  static void beforeAll() {
    MongoClientConnection.emptyMongoCollection();
  }

  @Test
  void shouldCreateNewIdentityForValidSubjectId() {
    // payload = {"sub": "abc@gmail.com", "vot": "P0.Cl.Cm", "iat": 1516239022}
    String token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmNAZ21haWwuY29tIiwid"
            + "m90IjoiUDAuQ2wuQ20iLCJpYXQiOjE1MTYyMzkwMjJ9.c1ZwVhXCl93L7MhSmsPDbtLeo-dEzJoX7zvzxuEv0wU";

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
    // payload = {"sub": "abc@gmail.com", "vot": "P2.Cl.Cm", "iat": 1516239022}
    String token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYmNAZ21haWwuY29tIiwidm"
            + "90IjoiUDIuQ2wuQ20iLCJpYXQiOjE1MTYyMzkwMjJ9.zxdfmq-Mo5MRWOKfyamKuSPWQF4gr8aLFU2zseFh7TA";

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
  void shouldReturnApplicationIdForExistingSubjectRecord() {
    MongoTemplate template = MongoClientConnection.getMongoTemplate();
    Identity identity =
        Identity.builder()
            .identityId(UUID.randomUUID())
            .subjectId("test@email.com")
            .applicationID("507f1f77bcf86cd799439011")
            .nino("AB000000B")
            .build();
    template.insert(identity);

    String token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGVtYWlsLmNvbSIsInZvdCI6IlAyL"
            + "kNsLkNtIiwiaWF0IjoxNTE2MjM5MDIyfQ.Q4UZq-8FVf7QBjeMWpUwqrlFs4hwJWVt-7nQTIcJygA";
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
            matchesPattern("^[a-zA-Z0-9]{24}$"), 
            "subjectId", 
            matchesPattern("(^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$)")
        );
    
  }
}
