package uk.gov.dwp.health.pip.identity.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import uk.gov.dwp.health.coordinator.openapi.model.ApplicationCoordinatorDto;
import uk.gov.dwp.health.coordinator.openapi.v1.api.DefaultApi;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;
import uk.gov.dwp.health.pip.identity.service.CoordinatorService;

import static uk.gov.dwp.health.coordinator.openapi.model.ApplicationCoordinatorDto.RoutedToEnum.PIPCS;
import static uk.gov.dwp.health.coordinator.openapi.model.ApplicationCoordinatorDto.RoutedToEnum.PIPSERVICE;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoordinatorServiceImpl implements CoordinatorService {

  static final String UNEXPECTED_ERROR_MESSAGE = "Unexpected error calling coordinator";

  private final DefaultApi coordinatorClient;

  @Override
  public boolean isPipcsApplication(final String applicationID) {
    try {
      final ApplicationCoordinatorDto application = coordinatorClient.getApplication(
          applicationID, null, null, null
      );
      final ApplicationCoordinatorDto.RoutedToEnum routedTo = application.getRoutedTo();
      final boolean isPipcs = routedTo == null || PIPCS.equals(routedTo);
      log.info("isPipcsApplication ? {}", isPipcs);
      return isPipcs;
    } catch (final RestClientException e) {
      // expect and ignore 404s, just means no match for this nino
      if (e.getMessage().startsWith("API returned 404")) {
        return true;
      } else {
        log.debug(UNEXPECTED_ERROR_MESSAGE, e);
        throw new GenericRuntimeException(UNEXPECTED_ERROR_MESSAGE);
      }
    }
  }

  @Override
  public boolean isPipServiceApplication(final String applicationID) {
    try {
      final ApplicationCoordinatorDto application = coordinatorClient.getApplication(
          applicationID, null, null, null
      );
      final ApplicationCoordinatorDto.RoutedToEnum routedTo = application.getRoutedTo();
      final boolean isPipService = routedTo == null || PIPSERVICE.equals(routedTo);
      log.info("isPipServiceApplication ? {}", isPipService);
      return isPipService;
    } catch (final RestClientException e) {
      // expect and ignore 404s, just means no match for this nino
      if (e.getMessage().startsWith("API returned 404")) {
        return true;
      } else {
        log.debug(UNEXPECTED_ERROR_MESSAGE, e);
        throw new GenericRuntimeException(UNEXPECTED_ERROR_MESSAGE);
      }
    }
  }
}
