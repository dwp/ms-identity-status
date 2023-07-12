package uk.gov.dwp.health.pip.identity.config;

import org.bson.Document;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoHealthIndicator extends AbstractHealthIndicator {
  private static final String MAX_WIRE_VERSION = "maxWireVersion";
  private final MongoTemplate mongoTemplate;

  public MongoHealthIndicator(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) throws Exception {
    Document result = this.mongoTemplate.executeCommand("{ hello: 1 }");
    builder.up().withDetail(MAX_WIRE_VERSION, result.getInteger(MAX_WIRE_VERSION));
  }
}
