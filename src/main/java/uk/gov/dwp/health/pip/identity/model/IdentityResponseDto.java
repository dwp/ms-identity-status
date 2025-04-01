package uk.gov.dwp.health.pip.identity.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.dwp.health.identity.status.openapi.model.IdentityResponse2;

@AllArgsConstructor(staticName = "of")
@Getter
@NoArgsConstructor
public class IdentityResponseDto {
  private boolean isCreated;
  private IdentityResponse2 identityResponse;
}
