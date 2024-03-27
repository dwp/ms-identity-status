package uk.gov.dwp.health.pip.identity.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.dwp.health.pip.identity.entity.Registration;

@Repository
public interface RegistrationRepository
    extends MongoRepository<Registration, String>, RegistrationRepositoryCustom {}
