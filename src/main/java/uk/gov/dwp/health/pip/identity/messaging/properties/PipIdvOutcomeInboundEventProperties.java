package uk.gov.dwp.health.pip.identity.messaging.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Configuration
@Validated
public class PipIdvOutcomeInboundEventProperties {

  @NotNull(message = "Identity response message routing key can not be null")
  @NotBlank(message = "Identity response message routing key can not be blank")
  @Value("${uk.gov.dwp.health.inbound.routing.key.identity.response}")
  private String routingKeyIdentityResponse;

  @NotNull(message = "Identity response message queue name can not be null")
  @NotBlank(message = "Identity response message queue name can not be blank")
  @Value("${uk.gov.dwp.health.inbound.queue.name.identity.response}")
  private String queueNameIdentityResponse;
}
