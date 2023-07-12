package uk.gov.dwp.health.pip.identity.dto.requests.create;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.javafaker.Faker;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class CreateIdentityRequest {

  private static final String[] idvStatuses = {"verified", "unverified"};

  @Default
  @JsonProperty("subject_id")
  private String subjectId = faker().bothify("?????#####@gmail.com");

  @Default
  @JsonProperty("identity_id")
  private String identityId = UUID.randomUUID().toString();

  @Default private String channel = "oidv";

  @Default
  @JsonProperty("idv_status")
  private String idvStatus = idvStatuses[faker().random().nextInt(idvStatuses.length)];

  @Default private String nino = faker().regexify("AC[0-9]{6}[A-D]");

  @Default
  @JsonProperty("application_id")
  private String applicationId = "123456789asdfghjklpoiuyt";

  @Default
  @JsonProperty("error_message")
  private String errorMessage = "";

  @Default
  @JsonProperty("date_time")
  private String dateTime =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());

  private static Faker faker() {
    return new Faker(new Locale("en-GB"));
  }
}
