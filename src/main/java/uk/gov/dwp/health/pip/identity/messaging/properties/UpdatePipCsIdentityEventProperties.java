package uk.gov.dwp.health.pip.identity.messaging.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Configuration
@Getter
@Setter
public class UpdatePipCsIdentityEventProperties {

  @NotNull(message = "pip-cs-identity message topic can not be null")
  @NotBlank(message = "pip-cs-identity message topic can not be blank")
  @Value("${uk.gov.dwp.health.outbound.topic}")
  private String topicName;

  @NotNull(message = "Identity response message routing key can not be null")
  @NotBlank(message = "Identity response message routing key can not be blank")
  @Value("${uk.gov.dwp.health.outbound.routing.key.identity.response}")
  private String routingKeyIdentityResponse;
}
