package uk.gov.dwp.health.pip.identity.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;
import uk.gov.dwp.health.pip.identity.messaging.properties.UpdatePipCsIdentityEventProperties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdatePipCsIdentityMessagePublisherTest {
    @Mock
    UpdatePipCsIdentityEventProperties inboundEventProperties;

    @Mock
    EventManager eventManager;

    private UpdatePipCsIdentityMessagePublisher updatePipCsIdentityMessagePublisher;

    final String applicationId = "123";
    final String idvStatus = "verified";
    final String identityId = "456";

    @BeforeEach
    void setUp() {
        when(inboundEventProperties.getTopicName()).thenReturn("789");
        when(inboundEventProperties.getRoutingKeyIdentityResponse()).thenReturn("foo");
        updatePipCsIdentityMessagePublisher = new UpdatePipCsIdentityMessagePublisher(eventManager,
                inboundEventProperties);
    }

    @Test
    void publishMessage_publishes_to_the_queue() {
        updatePipCsIdentityMessagePublisher.publishMessage(applicationId, idvStatus, identityId);
        verify(eventManager, times(1)).send(any(UpdatePipCsIdentityEvent.class));
    }

    @Test
    void publishMessage_throws_generic_runtime_exception_onerror() {
        when(inboundEventProperties.getRoutingKeyIdentityResponse()).thenThrow(NullPointerException.class);
        assertThrows(GenericRuntimeException.class, () -> updatePipCsIdentityMessagePublisher.
                publishMessage(applicationId, idvStatus, identityId));

    }
}
