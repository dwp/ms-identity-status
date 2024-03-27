package uk.gov.dwp.health.pip.identity.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(value = "application")
@Getter
@Setter
public class ApplicationProperties {

  private int accountRegistrationsLimit;
}
