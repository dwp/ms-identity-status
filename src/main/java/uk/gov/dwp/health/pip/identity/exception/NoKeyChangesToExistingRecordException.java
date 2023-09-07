package uk.gov.dwp.health.pip.identity.exception;

public class NoKeyChangesToExistingRecordException extends RuntimeException {
  public NoKeyChangesToExistingRecordException(final String msg) {
    super(msg);
  }
}
