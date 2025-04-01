package uk.gov.dwp.health.pip.identity.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.dwp.health.identity.status.openapi.model.UpliftDto;
import uk.gov.dwp.health.mongo.changestream.extension.MongoChangeStreamIdentifier;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Getter
@Setter
@Document(collection = "identity")
public class Identity extends MongoChangeStreamIdentifier {

  @Id private String id;

  @PersistenceCreator
  public Identity(String id, String subjectId, UUID identityId, LocalDateTime dateTime,
      String channel, String idvStatus, String nino, String applicationID, String errorMessage,
      String vot) {
    this.id = id;
    this.subjectId = subjectId;
    this.identityId = identityId;
    this.dateTime = dateTime;
    this.channel = channel;
    this.idvStatus = idvStatus;
    this.nino = nino;
    this.applicationID = applicationID;
    this.errorMessage = errorMessage;
    this.vot = vot;
    this.upliftDetails = null;
  }

  @Field(value = "subjectId")
  @Indexed
  private String subjectId;

  @Field(value = "identityId")
  @Indexed
  @Deprecated
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

  // If this field is set then the identity recrod was uplifted by this staffId
  @Field(value = "upliftDetails")
  private UpliftDto upliftDetails;

}
