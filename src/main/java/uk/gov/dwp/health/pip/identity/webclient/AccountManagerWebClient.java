package uk.gov.dwp.health.pip.identity.webclient;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.dwp.health.pip.identity.exception.AccountNotFoundException;
import uk.gov.dwp.health.pip.identity.model.AccountDetailsResponse;

@Service
@Slf4j
public class AccountManagerWebClient {

  private static final String ACCOUNT_DETAILS_FROM_EMAIL_PATH = "/v4/account/details/email/";
  private final WebClient webClient;

  public AccountManagerWebClient(@Qualifier("accManagerWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  public Optional<AccountDetailsResponse> getAccountDetailsFromEmail(String email) {
    log.info("About to make get details request from ms-account-manager");
    AccountDetailsResponse[] response =
        webClient
            .get()
            .uri(ACCOUNT_DETAILS_FROM_EMAIL_PATH + email)
            .retrieve()
            .bodyToMono(AccountDetailsResponse[].class)
            .block();

    log.info("Response received from get details request from ms-account-manager");

    if (response == null || ArrayUtils.isEmpty(response)) {
      throw new AccountNotFoundException("No Account found for email");
    }
    return Optional.of(response[0]);
  }
}
