package uk.gov.dwp.health.pip.identity.api.patch;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.dwp.health.identity.status.openapi.model.ApplicationIdDto;
import uk.gov.dwp.health.pip.identity.api.ApiTest;
import uk.gov.dwp.health.pip.identity.config.MongoClientConnection;
import uk.gov.dwp.health.pip.identity.entity.Identity;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dwp.health.pip.identity.utils.UrlBuilderUtil.updateApplicationIdUrl;

public class UpdateApplicationIdIT extends ApiTest {

    private static final String ID = "4bce57c491efc3ac3bc3e6f1";

    private ApplicationIdDto applicationIdDto;

    @BeforeAll
    static void beforeAll() {
        MongoClientConnection.emptyMongoIdentityCollection();
        MongoClientConnection.getMongoTemplate()
                .save(Identity.builder()
                        .id(ID)
                        .build());
    }

    @BeforeEach
    void beforeEach(){
        applicationIdDto = new ApplicationIdDto();
        applicationIdDto.setApplicationId(ID);
    }

    @Test
    void shouldReturn202StatusCodeForUpdateApplicationId(){

        Response response = patchRequest(updateApplicationIdUrl(ID), applicationIdDto);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED.value());

    }

    @Test
    public void shouldReturn404StatusCodeWhenTheIdentityIdIsNotFound() {
        int actualResponseCode = patchRequest(updateApplicationIdUrl("4bce57c491efc3ac3bc3e6f2"), applicationIdDto).statusCode();
        assertThat(actualResponseCode).isEqualTo(404);
    }

    @Test
    public void shouldReturn400StatusCodeWhenTheIdentityIdInvalidFormat() {
        int actualResponseCode = patchRequest(updateApplicationIdUrl("invalid-format"), applicationIdDto).statusCode();
        assertThat(actualResponseCode).isEqualTo(400);
    }

    @Test
    public void shouldReturn500StatusCodeWhenTheApplicationIdInvalidFormat() {

        applicationIdDto.setApplicationId("invalid-format");
        int actualResponseCode = patchRequest(updateApplicationIdUrl(ID), applicationIdDto).statusCode();

        assertThat(actualResponseCode).isEqualTo(500);
    }
}
