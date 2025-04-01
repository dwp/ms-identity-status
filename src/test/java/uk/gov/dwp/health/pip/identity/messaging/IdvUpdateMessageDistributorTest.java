package uk.gov.dwp.health.pip.identity.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.pip.identity.entity.Identity;
import uk.gov.dwp.health.pip.identity.model.IdentityRequestUpdateSchemaV1;
import uk.gov.dwp.health.pip.identity.service.CoordinatorService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.dwp.health.pip.identity.utils.IdentityStatusCalculator.VERIFIED;

@ExtendWith(MockitoExtension.class)
public class IdvUpdateMessageDistributorTest {
  private final UUID identityId = UUID.randomUUID();

  @Mock
  private CoordinatorService coordinatorService;

  @Mock
  private UpdatePipCsIdentityMessagePublisher updatePipCsIdentityMessagePublisher;
  @Mock
  private UpdateCoordinatorIdentityMessagePublisher updateCoordinatorIdentityMessagePublisher;

  @InjectMocks
  private IdvUpdateMessageDistributor idvUpdateMessageDistributor;


  private IdentityRequestUpdateSchemaV1 payload;


  @BeforeEach
  void jsonRequestPayload() {
    payload = new IdentityRequestUpdateSchemaV1();
    payload.setTimestamp(getCurrentDate());
    payload.setIdentityId(identityId);
    payload.setIdvOutcome(IdentityRequestUpdateSchemaV1.IdvOutcome.fromValue("verified"));
    payload.setNino("RN000003A");
    payload.setSubjectId("positive@dwp.gov.uk");
    payload.setChannel(IdentityRequestUpdateSchemaV1.Channel.fromValue("oidv"));

  }

    @Test
    void shouldPublishPipcsMessagesIfVotValueIsP2AndPipcsApplication() {
      when(coordinatorService.isPipcsApplication(anyString()))
          .thenReturn(true);
      payload.setIdvOutcome(null);
      payload.setVot(IdentityRequestUpdateSchemaV1.Vot.P_2_CL_CM);

      Identity verifiedIdentity = Identity.builder()
          .vot("P2.Cl.Cm")
          .nino("RN000001A")
          .applicationID("5ed0d430716609122be7a4d8")
          .identityId(identityId)
          .idvStatus(VERIFIED)
          .build();

      idvUpdateMessageDistributor.distribute(payload,verifiedIdentity);

      verify(updatePipCsIdentityMessagePublisher, times(1))
              .publishMessage("5ed0d430716609122be7a4d8", "verified", String.valueOf(identityId));

    }


  @Test
  void shouldPublishCoordinatorMessagesIfVotValueIsP2AndPipServiceApplication() {
    when(coordinatorService.isPipServiceApplication(anyString()))
        .thenReturn(true);
    payload.setIdvOutcome(null);
    payload.setVot(IdentityRequestUpdateSchemaV1.Vot.P_2_CL_CM);

    Identity verifiedIdentity = Identity.builder()
        .vot("P2.Cl.Cm")
        .nino("RN000001A")
        .applicationID("5ed0d430716609122be7a4d8")
        .identityId(identityId)
        .idvStatus(VERIFIED)
        .build();

    idvUpdateMessageDistributor.distribute(payload,verifiedIdentity);

    verify(updateCoordinatorIdentityMessagePublisher, times(1))
        .publishMessage("5ed0d430716609122be7a4d8", "verified", String.valueOf(identityId));
  }

    @Test
    void shouldPublishPipcsMessageIfIDVOutComeValueIsVerifiedAndPipcsApplication() {
      when(coordinatorService.isPipcsApplication(anyString()))
          .thenReturn(true);

      Identity verifiedIdentity = Identity.builder()
          .idvStatus("verified")
          .applicationID("5ed0d430716609122be7a4d8")
          .identityId(identityId)
          .build();

      idvUpdateMessageDistributor.distribute(payload,verifiedIdentity);

      verify(updatePipCsIdentityMessagePublisher, times(1))
          .publishMessage("5ed0d430716609122be7a4d8", "verified", String.valueOf(identityId));
    }


  @Test
  void shouldPublishCoordinatorMessageIfIDVOutComeValueIsVerifiedAndPipServiceApplication() {
    when(coordinatorService.isPipServiceApplication(anyString()))
        .thenReturn(true);

    Identity verifiedIdentity = Identity.builder()
        .idvStatus("verified")
        .applicationID("5ed0d430716609122be7a4d8")
        .identityId(identityId)
        .build();

    idvUpdateMessageDistributor.distribute(payload,verifiedIdentity);

    verify(updateCoordinatorIdentityMessagePublisher, times(1))
        .publishMessage("5ed0d430716609122be7a4d8", "verified", String.valueOf(identityId));

  }

    @Test
    void shouldNotPublishMessageIfApplicationIsNotPipcsOrPipService() {
      final String applicationID = "5ed0d430716609122be7a4d8";

      Identity verifiedIdentity = Identity.builder()
          .idvStatus("verified")
          .applicationID(applicationID)
          .identityId(identityId)
          .build();

      when(coordinatorService.isPipcsApplication(applicationID))
          .thenReturn(false);

      when(coordinatorService.isPipcsApplication(applicationID))
          .thenReturn(false);

      idvUpdateMessageDistributor.distribute(payload,verifiedIdentity);

      verify(updateCoordinatorIdentityMessagePublisher, never()).publishMessage(any(), any(), any());

      verify(updatePipCsIdentityMessagePublisher, never()).publishMessage(any(), any(), any());
    }

  @Test
  void shouldNotPublishAnyMessageIfVotValueIsNotP2() {

    payload.setIdvOutcome(null);
    payload.setVot(IdentityRequestUpdateSchemaV1.Vot.P_0_CL_CM);

    Identity unverifiedIdentity = Identity.builder().build();

    idvUpdateMessageDistributor.distribute(payload,unverifiedIdentity);

    verify(updateCoordinatorIdentityMessagePublisher, never()).publishMessage(any(), any(), any());

    verify(updatePipCsIdentityMessagePublisher, never()).publishMessage(any(), any(), any());
  }


  private String getCurrentDate() {
    DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    LocalDateTime localDateTime = LocalDateTime.now();
    return localDateTime.format(format);
  }



}
