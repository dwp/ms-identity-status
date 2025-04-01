package uk.gov.dwp.health.pip.identity.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CognitoToken {

  @JsonProperty("access_token")
  private String accessToken;
  @JsonProperty("expires_in")
  private String expiresIn;
  @JsonProperty("token_type")
  private String tokenType;

}
