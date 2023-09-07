package uk.gov.dwp.health.pip.identity.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TokenUtils {
  private TokenUtils() {}

  public static String decodePayload(String token) {
    String[] parts = token.split("\\.", 0);
    byte[] bytes = Base64.getUrlDecoder().decode(parts[1]);
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
