package uk.gov.dwp.health.pip.identity.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
public class IdentityNotFoundExceptionTest {

    @Test
    @DisplayName("Test identity not found exception")
    void testClaimNotFoundException() {
        IdentityNotFoundException cut = new IdentityNotFoundException("Identity not found");
        assertThat(cut.getMessage()).isEqualTo("Identity not found");
    }
}
