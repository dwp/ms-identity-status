package uk.gov.dwp.health.pip.identity.repository;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import uk.gov.dwp.health.pip.identity.entity.Registration;


import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DataMongoTest
class RegistrationRepositoryCustomImplTest {

  @Resource
  private MongoTemplate mongoTemplate;
  @Resource private RegistrationRepository registrationRepository;

  @BeforeEach
  void setup() {
    mongoTemplate.dropCollection(Registration.class);
  }

  @Nested
  class AccountBeingRegisteredTest {

    @Test
    void when_first_new_account_registration() {
      registrationRepository.incrementRegistrationCount();

      var registrations = registrationRepository.findAll();

      assertThat(registrations).hasSize(1);
      assertThat(registrations.get(0).getCount()).isOne();
    }

    @Test
    void when_subsequent_new_account_registration() {
      var registration = Registration.builder().count(1).build();
      mongoTemplate.save(registration);

      registrationRepository.incrementRegistrationCount();

      var registrations = registrationRepository.findAll();

      assertThat(registrations).hasSize(1);
      assertThat(registrations.get(0).getCount()).isEqualTo(2);
    }

    @Test
    void when_further_new_account_registration() {
      var registration = Registration.builder().count(1000).build();
      mongoTemplate.save(registration);

      registrationRepository.incrementRegistrationCount();

      var registrations = registrationRepository.findAll();

      assertThat(registrations).hasSize(1);
      assertThat(registrations.get(0).getCount()).isEqualTo(1001);
    }
  }

  @Nested
  class ResettingCountTest {

    @Test
    void when_no_count_exists() {
      registrationRepository.resetRegistrationCount();

      var registrations = registrationRepository.findAll();

      assertThat(registrations).hasSize(1);
      assertThat(registrations.get(0).getCount()).isZero();
    }

    @Test
    void when_count_zero() {
      var registration = Registration.builder().count(0).build();
      mongoTemplate.save(registration);

      registrationRepository.resetRegistrationCount();

      var registrations = registrationRepository.findAll();

      assertThat(registrations).hasSize(1);
      assertThat(registrations.get(0).getCount()).isZero();
    }

    @Test
    void when_count_more_than_zero() {
      var registration = Registration.builder().count(1).build();
      mongoTemplate.save(registration);

      registrationRepository.resetRegistrationCount();

      var registrations = registrationRepository.findAll();

      assertThat(registrations).hasSize(1);
      assertThat(registrations.get(0).getCount()).isZero();
    }
  }
}
