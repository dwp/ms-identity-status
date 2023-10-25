package uk.gov.dwp.health.pip.identity.webclient;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.dwp.health.pip.identity.exception.AccountNotFoundException;
import uk.gov.dwp.health.pip.identity.model.AccountDetailsResponse;

@ExtendWith(MockitoExtension.class)
class AccountManagerWebClientTest {

    @Mock private WebClient webClient;
    AccountManagerWebClient managerWebClient;

    @BeforeEach
    void setUp() {
        managerWebClient = new AccountManagerWebClient(webClient);
    }
    
    @Test
    void shouldReturn200ForGetDetails() {

        final var uriSpecMock = mock(WebClient.RequestHeadersUriSpec.class);
        final var headerSpecMock = mock(WebClient.RequestHeadersSpec.class);

        final var responseSpecMock = mock(WebClient.ResponseSpec.class);
        AccountDetailsResponse[] accountResponse =
                {AccountDetailsResponse.of("5ed0d430716609122be7a4d6")};

        when(webClient.get()).thenReturn(uriSpecMock);
        when(uriSpecMock.uri("/v4/account/details/email/test@email.com")).thenReturn(headerSpecMock);
        when(headerSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.bodyToMono(AccountDetailsResponse[].class)).thenReturn(Mono.just(accountResponse));

        var accountDetailsResponse =
                managerWebClient.getAccountDetailsFromEmail("test@email.com").get();
        assertThat(accountDetailsResponse.getAccountId()).isEqualTo("5ed0d430716609122be7a4d6");
    }

    @Test
    void shouldThrowExceptionForAccountManagerReturnsEmptyArray() {

        final var uriSpecMock = mock(WebClient.RequestHeadersUriSpec.class);
        final var headerSpecMock = mock(WebClient.RequestHeadersSpec.class);

        final var responseSpecMock = mock(WebClient.ResponseSpec.class);
        AccountDetailsResponse[] accountResponse = {};

        when(webClient.get()).thenReturn(uriSpecMock);
        when(uriSpecMock.uri("/v4/account/details/email/test@email.com")).thenReturn(headerSpecMock);
        when(headerSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.bodyToMono(AccountDetailsResponse[].class)).thenReturn(Mono.just(accountResponse));

        assertThrows(AccountNotFoundException.class, ()-> managerWebClient.getAccountDetailsFromEmail("test@email.com"));

    }

}
