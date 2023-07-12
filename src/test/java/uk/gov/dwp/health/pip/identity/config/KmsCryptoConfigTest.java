package uk.gov.dwp.health.pip.identity.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.crypto.CryptoDataManager;
import uk.gov.dwp.health.pip.identity.config.properties.CryptoConfigProperties;
import uk.gov.dwp.health.pip.identity.exception.CryptoConfigException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KmsCryptoConfigTest {

  @InjectMocks private KmsConfig config;
  @Mock private CryptoConfigProperties cryptoConfigProperties;

  @Test
  @DisplayName("test create crypto data manager for messaging ")
  void testCreateCryptoDataManagerForMessaging() {
    when(cryptoConfigProperties.getMessageDataKeyId()).thenReturn("event-mock-key-id");
    when(cryptoConfigProperties.isKmsKeyCache()).thenReturn(true);
    when(cryptoConfigProperties.getKmsOverride()).thenReturn("");
    when(cryptoConfigProperties.getRegion()).thenReturn("");
    assertThat(config.cryptoDataManager()).isNotNull().isExactlyInstanceOf(CryptoDataManager.class);
    verify(cryptoConfigProperties, times(2)).getKmsOverride();
    verify(cryptoConfigProperties, times(2)).getRegion();
  }

  @Test
  @DisplayName("test create crypto data manager for messaging with overrides")
  void testCreateCryptoDataManagerForMessagingWithOverride() {
    when(cryptoConfigProperties.getMessageDataKeyId()).thenReturn("event-mock-key-id");
    when(cryptoConfigProperties.isKmsKeyCache()).thenReturn(true);
    when(cryptoConfigProperties.getKmsOverride()).thenReturn("http://localhost");
    when(cryptoConfigProperties.getRegion()).thenReturn("eu-west-2");
    assertThat(config.cryptoDataManager()).isNotNull().isExactlyInstanceOf(CryptoDataManager.class);
    verify(cryptoConfigProperties, times(3)).getKmsOverride();
    verify(cryptoConfigProperties, times(3)).getRegion();
  }

  @Test
  @DisplayName("test create crypto data manager throws CryptoConfigurationException")
  void testCreateCryptoDataManagerThrowsCryptoConfigurationException() {
    var thrown = assertThrows(CryptoConfigException.class, () -> config.cryptoDataManager());
    assertThat(thrown.getMessage()).contains("Failed to config DataCryptoManager for Messaging");
  }
}
