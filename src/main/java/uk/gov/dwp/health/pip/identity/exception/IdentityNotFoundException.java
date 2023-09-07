package uk.gov.dwp.health.pip.identity.exception;

public class IdentityNotFoundException extends RuntimeException {
  public IdentityNotFoundException(final String msg) {
    super(msg);
  }
}
