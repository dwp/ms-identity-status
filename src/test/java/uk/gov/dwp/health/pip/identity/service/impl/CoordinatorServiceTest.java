package uk.gov.dwp.health.pip.identity.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import uk.gov.dwp.health.coordinator.openapi.model.ApplicationCoordinatorDto;
import uk.gov.dwp.health.coordinator.openapi.v1.api.DefaultApi;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;
import uk.gov.dwp.health.pip.identity.service.CoordinatorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.dwp.health.coordinator.openapi.model.ApplicationCoordinatorDto.RoutedToEnum.PIPCS;
import static uk.gov.dwp.health.coordinator.openapi.model.ApplicationCoordinatorDto.RoutedToEnum.PIPSERVICE;
import static uk.gov.dwp.health.pip.identity.service.impl.CoordinatorServiceImpl.UNEXPECTED_ERROR_MESSAGE;

class CoordinatorServiceTest {

  private DefaultApi defaultApi = mock(DefaultApi.class);
  private CoordinatorService coordinatorService = new CoordinatorServiceImpl(defaultApi);
  private static final String applicationId = "String";

  @Test
  void isPipcsWhenRoutedToPipcs() {
    configureMockApi(PIPCS);
    final boolean isPipcsApplication = coordinatorService.isPipcsApplication(applicationId);
    assertTrue(isPipcsApplication, "Expect routed to PIPCS to mean not thin slice");
  }

  @Test
  void isNotPipcsWhenRoutedToThinSlice() {
    configureMockApi(PIPSERVICE);
    final boolean isPipcsApplication = coordinatorService.isPipcsApplication(applicationId);
    assertFalse(isPipcsApplication, "Expect routed to PIPSERVICE to mean not PIPCS");
  }

  @Test
  void isPipcsWhenNotRouted() {
    configureMockApi(null);
    final boolean isPipcsApplication = coordinatorService.isPipcsApplication(applicationId);
    assertTrue(isPipcsApplication, "Expect not routed to mean not thin slice");
  }

  @Test
  void isPipcsWhenNotFound() {
    when(
        defaultApi.getApplication(nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class))
    ).thenThrow(new RestClientException("API returned 404"));
    final boolean isPipcsApplication = coordinatorService.isPipcsApplication(applicationId);
    assertTrue(isPipcsApplication, "Expect not found to mean not thin slice");
  }

  @Test
  void genericErrorForUnexpectedFailure() {
    when(
        defaultApi.getApplication(nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class))
    ).thenThrow(new RestClientException("API returned 1000004"));
    try {
      coordinatorService.isPipcsApplication(applicationId);
      fail();
    } catch (final GenericRuntimeException gre) {
      assertEquals(UNEXPECTED_ERROR_MESSAGE, gre.getMessage());
    }
  }

  private void configureMockApi(final ApplicationCoordinatorDto.RoutedToEnum routedTo) {
    final ApplicationCoordinatorDto result = new ApplicationCoordinatorDto();
    result.setRoutedTo(routedTo);
    when(
        defaultApi.getApplication(nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class))
    ).thenReturn(result);
  }
}
