package uk.gov.dwp.health.pip.identity.config;

import com.amazonaws.regions.Regions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.dwp.health.crypto.CryptoConfig;
import uk.gov.dwp.health.crypto.CryptoDataManager;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.pip.identity.config.properties.CryptoConfigProperties;
import uk.gov.dwp.health.pip.identity.exception.CryptoConfigException;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Configuration
public class KmsConfig {

  private final CryptoConfigProperties cryptoConfigProperties;

  public KmsConfig(CryptoConfigProperties cryptoConfigProperties) {
    this.cryptoConfigProperties = cryptoConfigProperties;
  }

  @Bean
  public CryptoDataManager cryptoDataManager() {
    try {
      var config = createConfigurationOverride();
      config.setDataKeyId(this.cryptoConfigProperties.getMessageDataKeyId());
      return new CryptoDataManager(config);
    } catch (IOException
        | NoSuchAlgorithmException
        | InvalidKeyException
        | CryptoException
        | NoSuchPaddingException
        | IllegalBlockSizeException e) {
      final String msg =
          String.format("Failed to config DataCryptoManager for Messaging %s", e.getMessage());
      log.error(msg);
      throw new CryptoConfigException(msg);
    }
  }

  private CryptoConfig createConfigurationOverride()
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IOException,
          IllegalBlockSizeException {
    var config = new CryptoConfig();
    if (this.cryptoConfigProperties.getKmsOverride() != null
        && !this.cryptoConfigProperties.getKmsOverride().isBlank()) {
      config.setKmsEndpointOverride(this.cryptoConfigProperties.getKmsOverride());
    }
    if (this.cryptoConfigProperties.getRegion() != null
        && !this.cryptoConfigProperties.getRegion().isBlank()) {
      config.setRegion(Regions.fromName(this.cryptoConfigProperties.getRegion()));
    }
    config.setCacheKmsDataKeys(this.cryptoConfigProperties.isKmsKeyCache());
    return config;
  }
}
