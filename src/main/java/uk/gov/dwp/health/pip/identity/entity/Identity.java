package uk.gov.dwp.health.pip.identity.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.dwp.health.mongo.changestream.extension.MongoChangeStreamIdentifier;

@AllArgsConstructor
@Builder
@Getter
@Setter
@Document(collection = "identity")
public class Identity extends MongoChangeStreamIdentifier {
  @Id private String id;

  @Field(value = "subjectId")
  @Indexed
  private String subjectId;

  @Field(value = "identityId")
  @Indexed
  private UUID identityId;

  @Field(value = "dateTime")
  private LocalDateTime dateTime;

  @Field(value = "channel")
  private String channel;

  @Field(value = "idvStatus")
  private String idvStatus;

  @Field(value = "nino")
  @Indexed
  private String nino;

  @Field("applicationID")
  @Indexed(unique = true)
  private String applicationID;

  @Field("errorMessage")
  private String errorMessage;

  @Field(value = "vot")
  private String vot;
}
