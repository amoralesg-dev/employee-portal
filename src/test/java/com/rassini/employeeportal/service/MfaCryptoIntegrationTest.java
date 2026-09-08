package com.rassini.employeeportal.service;

import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class MfaCryptoIntegrationTest {

    @TestConfiguration
    static class Config {
        @Bean
        public MfaCryptoService mfaCryptoService() {
            String testKey = Base64.getEncoder().encodeToString(new byte[32]);
            return new MfaCryptoService(testKey);
        }
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MfaCryptoService mfaCryptoService;

    @Test
    void guardaMfaSecretCifradoYPermiteRecuperar() {
        // Arrange
        String rawSecret = "JBSWY3DPEHPK3PXP";
        String encrypted = mfaCryptoService.encrypt(rawSecret);

        UserEntity user = UserEntity.builder()
                .username("test_mfa")
                .email("test_mfa@rassini.com")
                .passwordHash("hash")
                .enabled(true)
                .mfaEnabled(true)
                .mfaSecret(encrypted)
                .build();

        // Act - Guardar usando JPA
        UserEntity savedUser = userRepository.saveAndFlush(user);

        // Assert - Verificar en la base de datos cruda
        String rawDbValue = jdbcTemplate.queryForObject(
                "SELECT mfa_secret FROM users WHERE id = ?",
                String.class,
                savedUser.getId()
        );

        // Confirma que inicia con v1:
        assertThat(rawDbValue).startsWith("v1:");
        // Confirma que el secreto base32 no esta en texto plano
        assertThat(rawDbValue).doesNotContain(rawSecret);

        // Act - Recuperar via Entity
        UserEntity retrievedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        String retrievedEncrypted = retrievedUser.getMfaSecret();
        
        // Desencriptar usando el servicio
        String decrypted = mfaCryptoService.decrypt(retrievedEncrypted);

        // Assert
        assertThat(decrypted).isEqualTo(rawSecret);
    }
}
