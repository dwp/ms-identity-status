package uk.gov.dwp.health.pip.identity.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.dwp.health.pip.identity.entity.Identity;

import java.util.Optional;

@Repository
public interface IdentityRepository extends CrudRepository<Identity, String> {
  Optional<Identity> findByNino(String s);

  Optional<Identity> findBySubjectId(String s);

  Optional<Identity> findByApplicationID(String s);

  Optional<Identity> findById(String s);
}
