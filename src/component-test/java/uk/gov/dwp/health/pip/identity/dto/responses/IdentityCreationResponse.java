package uk.gov.dwp.health.pip.identity.dto.responses;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Data
@NoArgsConstructor
public class IdentityCreationResponse {
  private String ref;
}