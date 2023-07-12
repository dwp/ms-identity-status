package uk.gov.dwp.health.pip.identity.config.web;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.dwp.health.logging.LoggerContext;

@Component
@RequiredArgsConstructor
public class ReactorWebClientFactory {

  private final LoggerContext loggerContext;

  @Qualifier("application-manager-web-client")
  private final WebClient applicationManagerClient;

  public ReactorWebClient getClient() {
    return new ReactorWebClient(loggerContext, applicationManagerClient);
  }
}
