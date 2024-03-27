package uk.gov.dwp.health.pip.identity.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import uk.gov.dwp.health.pip.identity.entity.Registration;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Repository
@RequiredArgsConstructor
public class RegistrationRepositoryCustomImpl implements RegistrationRepositoryCustom {

  private static final String COUNT = "count";

  private final MongoTemplate mongoTemplate;

  @Override
  public void incrementRegistrationCount() {
    mongoTemplate
        .update(Registration.class)
        .matching(new Query(where(COUNT).exists(true)))
        .apply(new Update().inc(COUNT, 1))
        .upsert();
  }

  @Override
  public void resetRegistrationCount() {
    mongoTemplate
        .update(Registration.class)
        .matching(new Query(where(COUNT).exists(true)))
        .apply(new Update().set(COUNT, 0))
        .upsert();
  }
}
