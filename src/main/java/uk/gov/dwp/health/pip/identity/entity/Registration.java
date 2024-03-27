package uk.gov.dwp.health.pip.identity.entity;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Builder
@Document(collection = "registration")
public class Registration {

  @Id private String id;

  @Field(value = "count")
  private int count;
}
