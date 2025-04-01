package uk.gov.dwp.health.pip.identity.config.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.pip.identity.config.properties.RestClientProperties;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RestTemplateConfig {
  private final RestClientProperties restClientProperties;

  @Bean
  public RestTemplate restTemplate() {
    RestTemplate restTemplate = new RestTemplate(httpRequestFactory());
    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setSupportedMediaTypes(
        Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM));
    restTemplate.getMessageConverters().add(converter);
    return restTemplate;
  }

  private HttpComponentsClientHttpRequestFactory httpRequestFactory() {
    var factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(restClientProperties.getConnectionTimeout());
    var httpClient = HttpClientBuilder.create().disableRedirectHandling().build();
    factory.setHttpClient(httpClient);
    return factory;
  }
}
