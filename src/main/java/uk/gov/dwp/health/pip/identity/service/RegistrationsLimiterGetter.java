package uk.gov.dwp.health.pip.identity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.pip.identity.config.properties.ApplicationProperties;
import uk.gov.dwp.health.pip.identity.repository.RegistrationRepository;
import uk.gov.dwp.health.identity.status.openapi.model.RegistrationsLimiterDto;


import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@RequiredArgsConstructor
@Service
@Slf4j
public class RegistrationsLimiterGetter {

  private final ApplicationProperties applicationProperties;
  private final RegistrationRepository registrationRepository;

  public RegistrationsLimiterDto getRegistrationsLimiter() {
    log.info("About to check if registrations limit has been reached");

    int registrationsCount = getRegistrationsCount();

    int registrationsLimit = getRegistrationsLimit();

    log.info(registrationsCount + " registrations of " + registrationsLimit + " cap limit");

    return toDto(registrationsCount, registrationsLimit);
  }

  private int getRegistrationsCount() {
    int registrationsCount;

    var registrations = registrationRepository.findAll();
    if (registrations.isEmpty()) {
      registrationsCount = 0;
    } else {
      registrationsCount = registrations.get(0).getCount();
    }

    return registrationsCount;
  }

  private int getRegistrationsLimit() {
    return applicationProperties.getAccountRegistrationsLimit();
  }

  private RegistrationsLimiterDto toDto(int registrationsCount, int registrationsLimit) {
    var registrationsLimiterDto = new RegistrationsLimiterDto();

    if (registrationsCount >= registrationsLimit) {
      registrationsLimiterDto.setLimitReached(TRUE);
      log.info("Registrations limit has been reached");
    } else {
      registrationsLimiterDto.setLimitReached(FALSE);
      log.info("Registrations limit has not been reached");
    }

    return registrationsLimiterDto;
  }
}
