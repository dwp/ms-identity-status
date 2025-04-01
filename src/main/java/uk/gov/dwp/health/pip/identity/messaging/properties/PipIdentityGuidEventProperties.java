package uk.gov.dwp.health.pip.identity.messaging.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties("uk.gov.dwp.health.guid-event.outbound")
public class PipIdentityGuidEventProperties {
  private String topic;
  private String routingKey;
}
