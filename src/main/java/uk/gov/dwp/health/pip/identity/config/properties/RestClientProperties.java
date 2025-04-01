package uk.gov.dwp.health.pip.identity.config.properties;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "ms.identity.status")
@Configuration
@Getter
@Setter
public class RestClientProperties {

  private int connectionTimeout;

}
