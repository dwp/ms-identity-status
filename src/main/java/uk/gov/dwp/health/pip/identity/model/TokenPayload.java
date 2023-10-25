package uk.gov.dwp.health.pip.identity.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class TokenPayload {

  @JsonProperty("sub")
  @NotNull
  @Pattern(regexp = "(^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$)")
  private String sub;

  @JsonProperty("vot")
  private VotEnum vot;

  @JsonProperty("guid")
  private String guid;

  public enum VotEnum {
    P0_CL_CM("P0.Cl.Cm"),

    P1_CL_CM("P1.Cl.Cm"),

    P2_CL_CM("P2.Cl.Cm");

    private final String value;

    VotEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static VotEnum fromValue(String value) {
      for (VotEnum b : VotEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }
}
