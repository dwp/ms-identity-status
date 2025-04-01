package uk.gov.dwp.health.pip.identity.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.dwp.health.coordinator.openapi.v1.ApiClient;
import uk.gov.dwp.health.coordinator.openapi.v1.api.DefaultApi;
import uk.gov.dwp.health.pip.identity.config.properties.CoordinatorClientProperties;

@Configuration
@RequiredArgsConstructor
public class CoordinatorClientConfiguration {

  @Bean
  public DefaultApi defaultApi(
      final CoordinatorClientProperties coordinatorClientProperties
  ) {
    final ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(coordinatorClientProperties.getBaseUrl());
    return new DefaultApi(apiClient);
  }

}
