package uk.gov.dwp.health.pip.identity.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.pip.identity.repository.RegistrationRepository;

@RequiredArgsConstructor
@Service
@Slf4j
class RegistrationsCountResetter {

  private final RegistrationRepository registrationRepository;

  @Scheduled(cron = "${schedule.reset.registrations.count:0 0 0 * * WED}")
  void resetRegistrationsCount() {
    log.info("Running scheduled task to reset registrations count");

    registrationRepository.resetRegistrationCount();

    log.info("Finished scheduled task to reset registrations count");
  }
}
