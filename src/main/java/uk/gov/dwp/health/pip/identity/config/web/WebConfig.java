package uk.gov.dwp.health.pip.identity.config.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import uk.gov.dwp.health.logging.LoggerContext;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig {

  private final HttpClientBuilder clientBuilder;

  @Bean
  public LoggerContext loggerContext() {
    return new LoggerContext();
  }

  @Bean("application-manager-web-client")
  public WebClient applicationManagerWebClient(
      @Value("${application.manager.base.url}") String baseUrl,
      @Value("${max.timeout.millis}") Integer timeout) {
    return webClient(baseUrl, timeout);
  }

  private WebClient webClient(String base, Integer timeout) {

    HttpClient httpClient = clientBuilder.buildClient(timeout);

    return WebClient.builder()
        .baseUrl(base)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }
}
