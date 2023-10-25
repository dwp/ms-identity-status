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
    var vot = identity.getVot();
    if (vot != null) {
      if (mediumConfidenceVot(vot) && !StringUtils.isEmpty(identity.getNino())) {
        return VERIFIED;
      } else {
        return UNVERIFIED;
      }
    }
    return identity.getIdvStatus();
  }
}
