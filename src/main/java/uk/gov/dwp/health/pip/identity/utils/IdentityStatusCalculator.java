package uk.gov.dwp.health.pip.identity.utils;

import uk.gov.dwp.health.pip.identity.entity.Identity;

public class IdentityStatusCalculator {

  private IdentityStatusCalculator() {}

  private static String fromVot(String vot) {
    return vot.contentEquals("P2.Cl.Cm") ? "verified" : "unverified";
  }

  public static String fromIdentity(Identity identity) {
    var vot = identity.getVot();
    if (vot != null) {
      return fromVot(vot);
    }
    return identity.getIdvStatus();
  }
}
