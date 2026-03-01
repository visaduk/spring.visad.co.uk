package uk.co.visad.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.co.visad.util.FileEncryptionUtil;

import java.util.Base64;

/**
 * Creates a FileEncryptionUtil bean only when app.encryption.enabled=true
 * and a 256-bit Base64-encoded key is supplied via APP_ENCRYPTION_KEY.
 */
@Configuration
@ConditionalOnProperty(name = "app.encryption.enabled", havingValue = "true")
public class EncryptionConfig {

    @Value("${app.encryption.key}")
    private String encryptionKeyBase64;

    @Bean
    public FileEncryptionUtil fileEncryptionUtil() {
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
        return new FileEncryptionUtil(keyBytes);
    }
}
