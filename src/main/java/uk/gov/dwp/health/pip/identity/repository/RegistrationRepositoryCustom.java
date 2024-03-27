package uk.gov.dwp.health.pip.identity.repository;

public interface RegistrationRepositoryCustom {

  void incrementRegistrationCount();

  void resetRegistrationCount();
}
