package uk.gov.dwp.health.pip.identity.repository;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import uk.gov.dwp.health.mongo.changestream.config.properties.WatcherConfigProperties;
import uk.gov.dwp.health.pip.identity.entity.Identity;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class MongoEventListenerPipIdentityTest {

    private static final String COLLECTION = "identity";

    @Mock
    private WatcherConfigProperties watcherConfigProperties;

    @Test
    void listener_updates_channel_for_identity() {
        var identity = Identity.builder().build();
        var event = new BeforeConvertEvent<>(identity, COLLECTION);
        var eventListener = new MongoEventListenerPipIdentity(watcherConfigProperties);

        eventListener.onBeforeConvert(event);

        verify(watcherConfigProperties, times(1)).setChangeStreamChannel(identity, COLLECTION);
    }
}
