package uk.gov.dwp.health.pip.identity.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.dwp.health.pip.identity.exception.IdentityNotFoundException;

@Component
@ControllerAdvice
public class IdentityControllerAdvise {
  private static Logger log = LoggerFactory.getLogger(IdentityControllerAdvise.class);

  @ExceptionHandler({IdentityNotFoundException.class})
  public ResponseEntity<Void> handleApplicationNotFoundException(IdentityNotFoundException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }
}
