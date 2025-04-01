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
    return identityRegister(1);
  }

  public static String identityRegisterV2() {
    return identityRegister(2);
  }

  private static String identityRegister(final int version) {
    return baseURI + "/v" + version + "/identity";
  }

  public static String getLimiterUrl() {
    return baseURI + "/v1/identity/limiter";
  }

  public static String updateApplicationIdUrl(String identityId) {
    return baseURI + "/v1/identity/" + identityId;
  }

  public static String updateIdentityStatus(String applicationId) {
    return baseURI + "/v1/identity/agent-idv-uplift/" + applicationId;
  }

  public static String getIdentityDetailsUrl() { return baseURI + "/v1/identity/nino"; }

  public static String getNinoToGuidUrl() { return baseURI + "/v1/identity/guid"; }

  public static String getGuidToNinoUrl(String guid) {
    return baseURI + "/v1/identity/" + guid + "/nino";
  }
}
