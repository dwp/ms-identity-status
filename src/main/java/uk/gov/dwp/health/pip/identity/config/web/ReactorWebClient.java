package uk.gov.dwp.health.pip.identity.config.web;

import static reactor.core.publisher.Mono.error;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import uk.gov.dwp.health.monitoring.logging.LoggerContext;
import uk.gov.dwp.health.pip.identity.exception.ConflictException;
import uk.gov.dwp.health.pip.identity.exception.GenericRuntimeException;

@Slf4j
@RequiredArgsConstructor
public class ReactorWebClient {

  private final LoggerContext loggerContext;
  private final WebClient client;

  private static Function<ClientResponse, Mono<? extends Throwable>> convertToConflictException() {
    return response ->
        error(new ConflictException("Conflict occurred while processing the request"));
  }

  private static Function<ClientResponse, Mono<? extends Throwable>> convertToServiceException() {
    return response ->
        error(new GenericRuntimeException("Server error: " + response.statusCode().value()));
  }

  private static Predicate<HttpStatusCode> isConflict(String path) {
    return httpStatus -> {
      log.info("Got response status {}  from {}", httpStatus, path);
      return httpStatus == HttpStatus.CONFLICT;
    };
  }

  private static Predicate<HttpStatusCode> is5xx(String path) {
    return httpStatus -> {
      log.info("Got response status {}  from {}", httpStatus, path);
      return httpStatus.is5xxServerError();
    };
  }

  public <T> Mono<T> post(String path, Class<T> responseType, String body) {
    return client
        .post()
        .uri(path)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header("x-dwp-correlation-id", loggerContext.get("correlationId"))
        .body(Mono.just(body), String.class)
        .retrieve()
        .onStatus(is5xx(path), convertToServiceException())
        .onStatus(isConflict(path), convertToConflictException())
        .bodyToMono(responseType);
  }

  public <T> Mono<T> get(String path, Class<T> responseType) {
    return client
        .get()
        .uri(path)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .header("x-dwp-correlation-id", loggerContext.get("correlationId"))
        .retrieve()
        .onStatus(is5xx(path), convertToServiceException())
        .onStatus(isConflict(path), convertToConflictException())
        .bodyToMono(responseType)
        .retryWhen(
            Retry.fixedDelay(2, Duration.ofSeconds(1))
                .filter(GenericRuntimeException.class::isInstance));
  }
}
