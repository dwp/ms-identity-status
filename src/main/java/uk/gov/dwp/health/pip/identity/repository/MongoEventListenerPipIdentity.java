package uk.gov.dwp.health.pip.identity.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.mongo.changestream.config.properties.WatcherConfigProperties;
import uk.gov.dwp.health.pip.identity.entity.Identity;

@Component
@RequiredArgsConstructor
@Slf4j
public class MongoEventListenerPipIdentity extends AbstractMongoEventListener<Identity> {
  private final WatcherConfigProperties watcherConfigProperties;

  // Intercepts when the collection is about to be updated and sets a change stream event
  @Override
  public void onBeforeConvert(BeforeConvertEvent<Identity> event) {
    log.info(
        "Set change stream channel. changeStreamClass is {}, collectionName is identity",
        event.getSource());
    watcherConfigProperties.setChangeStreamChannel(event.getSource(), "identity");
  }
}
