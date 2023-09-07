package uk.gov.dwp.health.pip.identity.webclient;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.dwp.health.pip.identity.config.web.ReactorWebClient;

@ExtendWith(MockitoExtension.class)
class ApplicationManagerWebClientTest {

  @Mock private ReactorWebClient webClient;
  ApplicationManagerWebClient managerWebClient;

  @BeforeEach
  void setUp() {
    managerWebClient = new ApplicationManagerWebClient(webClient);
  }

  @Test
  void shouldReturnValidApplicationIdOptional() {
    when(webClient.post("/v1/application/matcher", String.class, "{\"nino\":\"RN000004A\"}"))
        .thenReturn(Mono.just("{\"application_id\":\"123\"}"));
    Optional<Object> applicationId = managerWebClient.getApplicationId("RN000004A");
    assertThat(applicationId).hasValue("123");
  }

  @Test
  void shouldEmptyOptionalForNullResponse() {
    when(webClient.post("/v1/application/matcher", String.class, "{\"nino\":\"RN000004A\"}"))
        .thenReturn(Mono.just("{\"application_id\": null}"));
    Optional<Object> applicationId = managerWebClient.getApplicationId("RN000004A");
    assertThat(applicationId).isEmpty();
  }

  @Test
  void shouldEmptyOptionalForEmptyResponse() {
    when(webClient.post("/v1/application/matcher", String.class, "{\"nino\":\"RN000004A\"}"))
        .thenReturn(Mono.just(""));
    Optional<Object> applicationId = managerWebClient.getApplicationId("RN000004A");
    assertThat(applicationId).isEmpty();
  }
}
