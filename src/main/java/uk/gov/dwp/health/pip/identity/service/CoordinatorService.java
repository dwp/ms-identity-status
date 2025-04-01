package uk.gov.dwp.health.pip.identity.service;

public interface CoordinatorService {
  boolean isPipcsApplication(final String applicationId);

  boolean isPipServiceApplication(final String applicationId);
}
