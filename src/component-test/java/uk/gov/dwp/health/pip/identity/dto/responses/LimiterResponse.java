package uk.gov.dwp.health.pip.identity.dto.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Data
@NoArgsConstructor
public class LimiterResponse {

  @JsonProperty("limit_reached")
  private boolean limitReached;
}
