package uk.gov.dwp.health.pip.identity.config.web;

import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.stereotype.Component;
import reactor.netty.http.client.HttpClient;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Component
public class HttpClientBuilder {
  public HttpClient buildClient(Integer timeout) {
    return HttpClient.create()
        .option(CONNECT_TIMEOUT_MILLIS, timeout)
        .responseTimeout(ofMillis(timeout))
        .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(timeout, MILLISECONDS)));
  }
}
