package uk.gov.dwp.health.pip.identity.config.web;

import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import uk.gov.dwp.health.logging.LoggerContext;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Configuration
public class WebClientConfiguration {

  @Bean
  public WebClient appManagerWebClient(
      @Value("${application.manager.base.url}") String baseUrl,
      @Value("${max.timeout.millis}") Integer timeout) {

    final HttpClient httpClient =
        HttpClient.create()
            .option(CONNECT_TIMEOUT_MILLIS, timeout)
            .responseTimeout(ofMillis(timeout))
            .doOnConnected(
                connection ->
                    connection.addHandlerLast(new ReadTimeoutHandler(timeout, MILLISECONDS)));

    return WebClient.builder()
        .baseUrl(baseUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }

  @Bean
  public ReactorWebClient applicationManagerReactorWebClient(
      LoggerContext loggerContext, WebClient applicationManagerWebClient) {
    return new ReactorWebClient(loggerContext, applicationManagerWebClient);
  }
}
