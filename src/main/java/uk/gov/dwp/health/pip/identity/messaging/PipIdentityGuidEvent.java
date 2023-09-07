package uk.gov.dwp.health.pip.identity.messaging;

import java.util.Map;
import uk.gov.dwp.health.integration.message.events.Event;

public class PipIdentityGuidEvent extends Event {
  public PipIdentityGuidEvent(String topic, String routingKey, Map<String, Object> payload) {
    setTopic(topic);
    setRoutingKey(routingKey);
    setPayload(payload);
  }
}
