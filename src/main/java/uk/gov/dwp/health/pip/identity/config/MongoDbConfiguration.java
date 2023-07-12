package uk.gov.dwp.health.pip.identity.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.RequiredArgsConstructor;
import org.bson.UuidRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.SpringDataMongoDB;

@Configuration
@RequiredArgsConstructor
public class MongoDbConfiguration {

  private final MongoProperties mongoProperties;
  private static final ServerApiVersion API_VERSION = ServerApiVersion.V1;

  @Value("${feature.mongo.versioned.api.enabled:true}")
  private boolean isMongoVersionedApiEnabled;

  @Bean
  public MongoClient mongoClient() {
    MongoClientSettings.Builder clientSettings =
        MongoClientSettings.builder()
            .uuidRepresentation(UuidRepresentation.JAVA_LEGACY)
            .applyConnectionString(new ConnectionString(mongoProperties.getUri()));
    if (isMongoVersionedApiEnabled) {
      clientSettings.serverApi(buildServerApi());
    }
    return MongoClients.create(clientSettings.build(), SpringDataMongoDB.driverInformation());
  }

  private ServerApi buildServerApi() {
    return ServerApi.builder().strict(true).deprecationErrors(true).version(API_VERSION).build();
  }
}
