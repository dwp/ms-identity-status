package uk.gov.dwp.health.pip.identity.messaging.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class UpdateCoordinatorIdentityEventProperties {

  @NotNull(message = "pip-coordinator-identity message topic can not be null")
  @NotBlank(message = "pip-coordinator-identity message topic can not be blank")
  @Value("${uk.gov.dwp.health.coordinator.outbound.topic}")
  private String updateCoordinatorIdvTopicName;

  @NotNull(message = "Identity response message routing key can not be null")
  @NotBlank(message = "Identity response message routing key can not be blank")
  @Value("${uk.gov.dwp.health.coordinator.outbound.routing.key.identity.response}")
  private String coordinatorIdentityResponseRoutingKey;
}
