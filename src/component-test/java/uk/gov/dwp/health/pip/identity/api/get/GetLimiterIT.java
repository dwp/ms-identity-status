package uk.gov.dwp.health.pip.identity.api.get;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import uk.gov.dwp.health.pip.identity.api.ApiTest;
import uk.gov.dwp.health.pip.identity.config.MongoClientConnection;
import uk.gov.dwp.health.pip.identity.entity.Registration;
import uk.gov.dwp.health.pip.identity.dto.requests.create.CreateIdentityRequest;
import uk.gov.dwp.health.pip.identity.dto.responses.LimiterResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.dwp.health.pip.identity.utils.UrlBuilderUtil.getLimiterUrl;
import static uk.gov.dwp.health.pip.identity.utils.UrlBuilderUtil.identityRegister;

class GetLimiterIT extends ApiTest {

  private MongoTemplate mongoTemplate;

  @BeforeEach
  void beforeEach() {
    mongoTemplate = MongoClientConnection.getMongoTemplate();
    mongoTemplate.dropCollection("registration");
    mongoTemplate.dropCollection("identity");
  }

  @Test
  void when_getting_limiter() {
    var registration = Registration.builder().count(1).build();
    mongoTemplate.save(registration);

    var response = getRequest(getLimiterUrl());
    var limiterResponse = response.as(LimiterResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(limiterResponse.isLimitReached()).isFalse();
  }

  @Test
  void when_limit_reached_and_schedule_triggered() {
    verifyLimitReached();

    await().atMost(1, TimeUnit.MINUTES).until(() -> isCount(0));

    verifyNewRegistration();
  }

  private void verifyLimitReached() {
    var registration = Registration.builder().count(11).build();
    mongoTemplate.save(registration);

    var response = getRequest(getLimiterUrl());
    var limiterResponse = response.as(LimiterResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(limiterResponse.isLimitReached()).isTrue();
  }

  private void verifyNewRegistration() {

    // payload = {"sub": "identity_no_account@test.com", "vot": "P0.Cl.Cm", "iat": 1516239022}
    String token =
            "eyJzdWIiOiJpZGVudGl0eV9ub19hY2NvdW50QHRlc3QuY29tIiwidm90IjoiUDAuQ2wuQ20iLCJpYXQiOjE1MTYyMzkwMjJ9.eyJzdWIiOiJpZGVudGl0eV9ub19hY2NvdW50QHRlc3QuY29tIiwidm90IjoiUDAuQ2wuQ20iLCJpYXQiOjE1MTYyMzkwMjJ9.c1ZwVhXCl93L7MhSmsPDbtLeo-dEzJoX7zvzxuEv0wU";


    var createAccountRequest = CreateIdentityRequest.builder().build();
    var response = postRequestWithHeader(
            identityRegister(),
            createAccountRequest,
            "x-id-token",
            token
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(isCount(1)).isTrue();
  }

  private boolean isCount(int expectedCount) {
    MongoCollection<Document> collection = mongoTemplate.getCollection("registration");

    Document query = new Document("count", expectedCount);
    List<Document> results = new ArrayList<>();

    collection.find(query).into(results);

    return results.size() == 1;
  }
}
