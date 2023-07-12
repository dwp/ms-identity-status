package uk.gov.dwp.health.pip.identity.config.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.dwp.health.logging.LoggerContext;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;

import java.util.function.Function;
import java.util.function.Predicate;

import static reactor.core.publisher.Mono.error;

@Slf4j
@RequiredArgsConstructor
public class ReactorWebClient {

  private final LoggerContext loggerContext;
  private final WebClient client;

  public <T> Mono<T> post(String path, Class<T> responseType, String body) {
    Predicate<HttpStatus> is5xx =
        httpStatus -> {
          log.info("Got response status {}  from {}", httpStatus, path);
          return httpStatus.is5xxServerError();
        };

    Predicate<HttpStatus> isConflict =
        httpStatus -> {
          log.info("Got response status {}  from {}", httpStatus, path);
          return httpStatus == HttpStatus.CONFLICT;
        };

    Function<ClientResponse, Mono<? extends Throwable>> convertToServiceException =
        response -> error(new GenericRuntimeException("Server error: " + response.rawStatusCode()));

    Function<ClientResponse, Mono<? extends Throwable>> convertToConflictException =
        response -> error(new ConflictException("Conflict occurred while processing the request"));

    return client
        .post()
        .uri(path)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header("x-dwp-correlation-id", loggerContext.get("correlationId"))
        .body(Mono.just(body), String.class)
        .retrieve()
        .onStatus(is5xx, convertToServiceException)
        .onStatus(isConflict, convertToConflictException)
        .bodyToMono(responseType);
  }
}
