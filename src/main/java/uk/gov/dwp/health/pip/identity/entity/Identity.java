package uk.gov.dwp.health.pip.identity.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Getter
@Setter
@Document(collection = "identity")
public class Identity {
  @Id private String id;

  @JsonProperty(value = "subject_id", required = true)
  @Indexed
  private String subjectId;

  @JsonProperty(value = "identity_id", required = true)
  @Indexed
  private UUID identityId;

  @JsonProperty(value = "date_time", required = true)
  private LocalDateTime dateTime;

  @JsonProperty(value = "channel", required = true)
  private String channel;

  @JsonProperty(value = "idv_status", required = true)
  private String idvStatus;

  @JsonProperty(value = "nino", required = true)
  @Indexed
  private String nino;

  @JsonProperty("application_id")
  private String applicationID;

  @JsonProperty("error_message")
  private String errorMessage;
}
