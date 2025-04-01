package uk.gov.dwp.health.pip.identity.messaging;

import java.util.HashMap;
import java.util.Map;
import uk.gov.dwp.health.integration.message.events.Event;

public class UpdateCoordinatorIdentityEvent extends Event {

  UpdateCoordinatorIdentityEvent(
      String topic, String applicationId, String idvStatus, String identityId, String routingKey) {
    Map<String, Object> map = new HashMap<>();
    map.put("pip_apply_application_id", applicationId);
    map.put("idv_status", idvStatus);
    map.put("identity_id", identityId);

    setPayload(map);
    setTopic(topic);
    setRoutingKey(routingKey);
  }
}
