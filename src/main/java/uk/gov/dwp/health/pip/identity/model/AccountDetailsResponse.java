package uk.gov.dwp.health.pip.identity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountDetailsResponse {

  @JsonProperty("ref")
  private String accountId;
}
