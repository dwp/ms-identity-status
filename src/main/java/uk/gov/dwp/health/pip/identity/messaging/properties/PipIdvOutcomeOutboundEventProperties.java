package uk.gov.dwp.health.pip.identity.messaging.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Configuration
@Validated
public class PipIdvOutcomeOutboundEventProperties {

  @NotNull(message = "Identity request message routing key can not be null")
  @NotBlank(message = "Identity request message routing key can not be blank")
  @Value("${uk.gov.dwp.health.identity.request.outbound.routing.key}")
  private String routingKeyIdentityRequest;

  @NotNull(message = "Identity request message topic name can not be null")
  @NotBlank(message = "Identity request message topic name can not be blank")
  @Value("${uk.gov.dwp.health.identity.request.outbound.topic.name}")
  private String topicNameIdentityRequest;
}
