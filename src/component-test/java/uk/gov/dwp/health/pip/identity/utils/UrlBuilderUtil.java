package uk.gov.dwp.health.pip.identity.utils;

import static io.restassured.RestAssured.baseURI;

public class UrlBuilderUtil {

  public static String getIdvStatusBySubjectUrl() {
    return baseURI + "/v1/identity/get-idv-status-by-subject-id";
  }

  public static String getIdvStatusByNinoUrl() {
    return baseURI + "/v1/identity/get-idv-status-by-nino";
  }

  public static String identityRegister() {
    return baseURI + "/v1/identity";
  }

  public static String updateApplicationIdUrl(String identityId) { return baseURI + "/v1/identity/" + identityId; }
}
