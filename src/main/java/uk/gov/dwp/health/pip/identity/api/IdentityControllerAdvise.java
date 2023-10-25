package uk.gov.dwp.health.pip.identity.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.dwp.health.pip.identity.exception.AccountNotFoundException;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.IdentityNotFoundException;
import uk.gov.dwp.health.pip.identity.exception.ValidationException;

@Component
@ControllerAdvice
@Slf4j
public class IdentityControllerAdvise {

  @ExceptionHandler({IdentityNotFoundException.class})
  public ResponseEntity<Void> handleIdentityNotFoundException(IdentityNotFoundException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  @ExceptionHandler({AccountNotFoundException.class})
  public ResponseEntity<Void> handleAccountNotFoundException(AccountNotFoundException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  @ExceptionHandler({ValidationException.class})
  public ResponseEntity<Void> handleValidationException(ValidationException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
  }

  @ExceptionHandler({ConflictException.class})
  public ResponseEntity<Void> handleConflictException(ConflictException ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).build();
  }

  @ExceptionHandler({Exception.class})
  public ResponseEntity<Void> handleGenericRuntimeException(Exception ex) {
    log.warn(ex.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }
}
