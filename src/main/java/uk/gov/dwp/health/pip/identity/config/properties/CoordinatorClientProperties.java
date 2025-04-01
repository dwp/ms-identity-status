package uk.gov.dwp.health.pip.identity.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(value = "coordinator")
@Getter
@Setter
@Validated
public class CoordinatorClientProperties {

  @NotBlank(message = "Coordinator base url required")
  private String baseUrl;

}
