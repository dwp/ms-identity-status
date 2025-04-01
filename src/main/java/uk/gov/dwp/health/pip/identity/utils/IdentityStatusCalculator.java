package uk.gov.dwp.health.pip.identity.utils;

import org.apache.commons.lang3.StringUtils;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;

public class IdentityStatusCalculator {
  public static final String VERIFIED = "verified";
  public static final String UNVERIFIED = "unverified";

  private IdentityStatusCalculator() {}

  private static boolean mediumConfidenceVot(String vot) {
    return vot.contentEquals(IdentityRequestUpdateSchemaV1.Vot.P_2_CL_CM.value());
  }

  public static String fromIdentity(Identity identity) {
    final String vot = identity.getVot();
    if (vot == null) {
      return identity.getIdvStatus();
    }
    return fromDataValues(identity.getIdvStatus(), vot, identity.getNino());
  }

  private static String fromDataValues(
      final String idvStatus, final String vot, final String nino
  ) {
    if ((VERIFIED.equals(idvStatus) || idvStatus == null)
        && (mediumConfidenceVot(vot) && !StringUtils.isEmpty(nino))) {
      return VERIFIED;
    } else {
      return UNVERIFIED;
    }
  }

}
