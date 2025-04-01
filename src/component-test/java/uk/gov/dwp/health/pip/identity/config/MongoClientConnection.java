package uk.gov.dwp.health.pip.identity.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.springframework.data.mongodb.core.MongoTemplate;

import static uk.gov.dwp.health.pip.identity.utils.EnvironmentUtil.getEnv;

public class MongoClientConnection {
  public static MongoTemplate getMongoTemplate() {
    ConnectionString connectionString =
        new ConnectionString(
            "mongodb://"
                + getEnv("MONGODB_HOST", "localhost")
                + ":"
                + getEnv("MONGODB_PORT", "27017")
                + "/identity");

    MongoClientSettings mongoClientSettings =
        MongoClientSettings.builder()
            .uuidRepresentation(UuidRepresentation.JAVA_LEGACY)
            .applyConnectionString(connectionString)
            .build();

    MongoClient mongoClient = MongoClients.create(mongoClientSettings);

    return new MongoTemplate(mongoClient, "identity");
  }

  public static void emptyMongoIdentityCollection() {
    MongoCollection<Document> mongoCollection = getMongoTemplate().getCollection("identity");
    mongoCollection.drop();
  }
}
