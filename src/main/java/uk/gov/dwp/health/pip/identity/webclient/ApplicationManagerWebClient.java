package uk.gov.dwp.health.pip.identity.webclient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.pip.identity.config.web.ReactorWebClient;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationManagerWebClient {
  private static final String APPLICATION_MATCHER_PATH = "/v1/application/matcher";
  private static final String APPLICATION_ID = "application_id";
  private static final String NINO_INPUT = "{\"nino\":\"NINO_VALUE\"}";

  private final ReactorWebClient webClient;

  public Optional<Object> getApplicationId(String nino) {
    log.info("About to call ms-application-manager");
    String requestJson = NINO_INPUT.replace("NINO_VALUE", nino);
    Optional<Object> response =
        webClient
            .post(APPLICATION_MATCHER_PATH, String.class, requestJson)
            .map(this::deserializeApplicationIdResponse)
            .block();
    log.info("Response received from ms-application-manager");
    return response;
  }

  private Optional<Object> deserializeApplicationIdResponse(String applicationIdResponse) {
    JsonParser jsonParser = JsonParserFactory.getJsonParser();
    if (StringUtils.isBlank(applicationIdResponse)) {
      return Optional.empty();
    }
    return Optional.ofNullable(jsonParser.parseMap(applicationIdResponse).get(APPLICATION_ID));
  }
}
