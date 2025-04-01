package uk.gov.dwp.health.pip.identity.repository;

import static org.assertj.core.api.Assertions.*;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1.Vot;

@DataMongoTest
@ExtendWith(SpringExtension.class)
@DirtiesContext
@TestPropertySource(properties = {"de.flapdoodle.mongodb.embedded.version =5.0.5"})
class IdentityRepositoryTest {

  @Autowired private IdentityRepository repository;

  private static final String APPLICATION_ID = "5ed0d430716609122be7a4d6";
  // Unique Id to trace or debugging purposes
  private static final UUID IDENTITY_ID = UUID.randomUUID();

  @BeforeAll
  static void beforeAll(@Autowired MongoTemplate mongoTemplate) {

    DBObject objectToSave =
        BasicDBObjectBuilder.start()
            .add("subjectId", "positive@dwp.gov.uk")
            .add("dateTime", LocalDateTime.now())
            .add("nino", "RN000003A")
            .add("channel", "oidv")
            .add("idvStatus", "verified")
            .add("identityId", IDENTITY_ID)
            .add("applicationID", APPLICATION_ID)
            .add("vot", Vot.P_2_CL_CM.value())
            .get();

    mongoTemplate.save(objectToSave, "identity");
  }

  @Test
  void findByNino() {
    Optional<Identity> byNino = repository.findByNino("RN000003A");
    assertThat(byNino)
        .isNotEmpty()
        .hasValueSatisfying(
            identity -> {
              assertThat(identity.getChannel()).isEqualTo("oidv");
              assertThat(identity.getSubjectId()).isEqualTo("positive@dwp.gov.uk");
              assertThat(identity.getIdvStatus()).isEqualTo("verified");
              assertThat(identity.getIdentityId()).isEqualTo(IDENTITY_ID);
              assertThat(identity.getApplicationID()).isEqualTo(APPLICATION_ID);
              assertThat(identity.getVot()).isEqualTo(Vot.P_2_CL_CM.value());
              assertThat(identity.getUpliftDetails()).isNull();
              assertThat(identity.getErrorMessage()).isNull();
            });
  }

  @Test
  void findBySubjectId() {
    Optional<Identity> bySubjectId = repository.findBySubjectId("positive@dwp.gov.uk");
    assertThat(bySubjectId)
        .isNotEmpty()
        .hasValueSatisfying(
            identity -> {
              assertThat(identity.getNino()).isEqualTo("RN000003A");
              assertThat(identity.getChannel()).isEqualTo("oidv");
              assertThat(identity.getIdvStatus()).isEqualTo("verified");
              assertThat(identity.getIdentityId()).isEqualTo(IDENTITY_ID);
              assertThat(identity.getApplicationID()).isEqualTo(APPLICATION_ID);
              assertThat(identity.getVot()).isEqualTo(Vot.P_2_CL_CM.value());
              assertThat(identity.getUpliftDetails()).isNull();
              assertThat(identity.getErrorMessage()).isNull();
            });
  }

  @Test
  void findByApplicationID() {
    Optional<Identity> byApplicationID = repository.findByApplicationID(APPLICATION_ID);
    assertThat(byApplicationID)
        .isNotEmpty()
        .hasValueSatisfying(
            identity -> {
              assertThat(identity.getNino()).isEqualTo("RN000003A");
              assertThat(identity.getChannel()).isEqualTo("oidv");
              assertThat(identity.getIdvStatus()).isEqualTo("verified");
              assertThat(identity.getIdentityId()).isEqualTo(IDENTITY_ID);
              assertThat(identity.getSubjectId()).isEqualTo("positive@dwp.gov.uk");
              assertThat(identity.getVot()).isEqualTo(Vot.P_2_CL_CM.value());
              assertThat(identity.getUpliftDetails()).isNull();
              assertThat(identity.getErrorMessage()).isNull();
            });
  }
}
