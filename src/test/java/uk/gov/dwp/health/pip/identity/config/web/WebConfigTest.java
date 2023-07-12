package uk.gov.dwp.health.pip.identity.config.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.netty.http.client.HttpClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebConfigTest {
  @InjectMocks private WebConfig webConfig;
  @Mock private HttpClientBuilder clientBuilder;

  @Test
  void shouldCreateHttpClient() {
    when(clientBuilder.buildClient(any())).thenReturn(HttpClient.create());

    webConfig.applicationManagerWebClient("test", 300);

    verify(clientBuilder).buildClient(300);
    verifyNoMoreInteractions(clientBuilder);
  }
}
