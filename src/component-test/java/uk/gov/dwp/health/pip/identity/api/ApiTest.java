package uk.gov.dwp.health.pip.identity.api;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static uk.gov.dwp.health.pip.identity.utils.EnvironmentUtil.getEnv;

public class ApiTest {
  static RequestSpecification requestSpec;

  @BeforeAll
  public static void setup() {
    RestAssured.baseURI = getEnv("HOST", "http://localhost");
    RestAssured.port = Integer.parseInt(getEnv("PORT", "8080"));
    RestAssured.defaultParser = Parser.JSON;

    requestSpec =
        new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .addFilter(new AllureRestAssured())
            .build();
  }

  protected Response postRequest(String path, Object bodyPayload) {
    return given().spec(requestSpec).body(bodyPayload).when().post(path);
  }

  protected Response postRequestWithHeader(String path, Object bodyPayload, String headerName, String headerValue) {

    final UUID correlation = UUID.randomUUID();


    RequestSpecification requestSpecWithHeaders =
            new RequestSpecBuilder()
                    .setContentType(ContentType.JSON)
                    .addFilter(new AllureRestAssured())
                    .addHeader(headerName, headerValue)
                    .addHeader("x-dwp-correlation-id", correlation.toString())
                    .build();

    return given().spec(requestSpecWithHeaders).body(bodyPayload).when().post(path);
  }

  protected Response getRequest(String path) {
    return given().spec(requestSpec).when().get(path);
  }

  protected Response getRequestWithHeader(String path, String headerName, String headerValue) {
        RequestSpecification requestSpecWithHeaders =
                new RequestSpecBuilder()
                        .setContentType(ContentType.JSON)
                        .addFilter(new AllureRestAssured())
                        .addHeader(headerName, headerValue)
                        .build();

    return given().spec(requestSpecWithHeaders).when().get(path);
  }

  protected Response patchRequest(String path, Object bodyPayload) {
    return given().spec(requestSpec).body(bodyPayload).when().patch(path);
  }
}
