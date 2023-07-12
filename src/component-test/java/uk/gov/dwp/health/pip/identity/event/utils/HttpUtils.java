package uk.gov.dwp.health.pip.identity.event.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpUtils {

  private final HttpClient httpClient;
  private final String wireMockHost;

  public HttpUtils(String wireMockHost) {
    httpClient = HttpClient.newHttpClient();
    this.wireMockHost = wireMockHost;
  }

  private HttpResponse<String> makePostRequest(
      String host, String path, HttpRequest.BodyPublisher postBody)
      throws IOException, InterruptedException {
    var httpRequest = HttpRequest.newBuilder().POST(postBody).uri(URI.create(host + path)).build();

    return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
  }

  public void resetWireMockScenarios() throws IOException, InterruptedException {
    makePostRequest(wireMockHost, "/__admin/scenarios/reset", HttpRequest.BodyPublishers.noBody());
  }
}
