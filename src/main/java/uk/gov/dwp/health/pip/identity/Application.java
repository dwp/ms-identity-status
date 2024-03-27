package uk.gov.dwp.health.pip.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableMongoRepositories(basePackages = {"uk.gov.dwp.health.pip.identity"})
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
