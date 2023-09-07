package uk.gov.dwp.health.pip.identity.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import uk.gov.dwp.health.pip.identity.exception.IdentityNotFoundException;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@Tag("unit")
public class IdentityControllerAdviseTest {
    
    private static IdentityControllerAdvise controllerAdvise;

    @BeforeAll
    static void setupSpec() {
        controllerAdvise = new IdentityControllerAdvise();
    }

    @Test
    void when_application_not_found_exception_raised_unauthorised_statue_returned() {
        var exp = new IdentityNotFoundException("given identity not found");
        var actual = controllerAdvise.handleApplicationNotFoundException(exp);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(actual.getBody()).isNull();
    }
}
