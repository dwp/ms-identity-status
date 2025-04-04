package uk.gov.dwp.health.pip.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.*;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import uk.gov.dwp.health.identity.status.openapi.api.V1Api;
import uk.gov.dwp.health.pip.identity.exception.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@Tag("unit")
public class IdentityControllerAdviseTest {

    private static IdentityControllerAdvise controllerAdvise;

    @BeforeAll
    static void setupSpec() {
        controllerAdvise = new IdentityControllerAdvise();
    }

    @Test
    void whenIdentityNotFoundExceptionRaisedNotFoundStatusReturned() {
        var exp = new IdentityNotFoundException("given identity not found");
        var actual = controllerAdvise.handleIdentityNotFoundException(exp);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(actual.getBody()).isNull();
    }

    @Test
    void whenAccountNotFoundExceptionRaisedNotFoundStatusReturned() {
        var exp = new AccountNotFoundException("given account not found");
        var actual = controllerAdvise.handleAccountNotFoundException(exp);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(actual.getBody()).isNull();
    }


    @Test
    void shouldReturnBadRequestForValidationException() {
        var exp = new ValidationException("Validation Exception");
        var actual = controllerAdvise.handleValidationException(exp);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(actual.getBody()).isNull();
    }

    @Test
    void shouldReturnBadRequestForMissingRequestHeaderException() throws Exception {
        Method method = V1Api.class.getMethod("_getGuidFromNino", String.class);
        var exp = new MissingRequestHeaderException("Missing Header Exception",
            new MethodParameter(method, 0));
        var actual = controllerAdvise.handleMissingRequestHeaderException(exp);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(actual.getBody()).isNull();
    }

    @Test
    void shouldReturnConflictForConflictException() {
        var exp = new ConflictException("Conflict Exception");
        var actual = controllerAdvise.handleConflictException(exp);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(actual.getBody()).isNull();
    }

    @Test
    void shouldReturnInternalServerForAnyException() {
        var exp = new GenericRuntimeException("Generic Runtime Exception");
        var actual = controllerAdvise.handleGenericRuntimeException(exp);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(actual.getBody()).isNull();
    }

    @Test
    void shouldReturnConstraintViolationException() {
        ConstraintViolationException constraintViolationException =
            mock(ConstraintViolationException.class);
        when(constraintViolationException.getMessage())
            .thenReturn("mock-constraint-violation-exception");

        ResponseEntity<Void> response = controllerAdvise
            .handleConstraintViolationException(constraintViolationException);
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }
}
