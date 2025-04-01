package uk.gov.dwp.health.pip.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Internal Server Error")
public class IdentityRestClientException extends RuntimeException {
  public IdentityRestClientException(String message) {
    super(message);
  }
}
