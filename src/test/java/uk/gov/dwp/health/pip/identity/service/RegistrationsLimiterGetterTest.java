package uk.gov.dwp.health.pip.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.identity.status.openapi.model.RegistrationsLimiterDto;
import uk.gov.dwp.health.pip.identity.config.properties.ApplicationProperties;
import uk.gov.dwp.health.pip.identity.entity.Registration;
import uk.gov.dwp.health.pip.identity.repository.RegistrationRepository;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationsLimiterGetterTest {

  @Mock private ApplicationProperties applicationProperties;
  @Mock private RegistrationRepository registrationRepository;

  private RegistrationsLimiterGetter registrationsLimiterGetter;

  @BeforeEach
  public void beforeEach() {
    registrationsLimiterGetter = new RegistrationsLimiterGetter(
        applicationProperties, registrationRepository
    );
  }

  @Test
  public void getRegistrationsLimiterLimitNotReached() {
    when(applicationProperties.getAccountRegistrationsLimit()).thenReturn(2);
    final RegistrationsLimiterDto registrationsLimiterDto = registrationsLimiterGetter.getRegistrationsLimiter();
    final Boolean limitReached = registrationsLimiterDto.isLimitReached();
    assertNotNull(limitReached);
    assertFalse(limitReached);
  }

  @Test
  public void getRegistrationsLimiterLimitReached() {
    when(applicationProperties.getAccountRegistrationsLimit()).thenReturn(1);
    when(registrationRepository.findAll()).thenReturn(Arrays.asList(Registration.builder().count(3).build()));
    final RegistrationsLimiterDto registrationsLimiterDto = registrationsLimiterGetter.getRegistrationsLimiter();
    final Boolean limitReached = registrationsLimiterDto.isLimitReached();
    assertNotNull(limitReached);
    assertTrue(limitReached);
  }
}
