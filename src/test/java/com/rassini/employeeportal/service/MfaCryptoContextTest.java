package com.rassini.employeeportal.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class MfaCryptoContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Configuration
    static class TestConfig {
        @Bean
        public MfaCryptoService mfaCryptoService(@Value("${app.security.mfa.encryption-key:}") String key) {
            return new MfaCryptoService(key);
        }
    }

    @Test
    void contextoNoIniciaSinLlave() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("es requerida");
        });
    }

    @Test
    void contextoNoIniciaConBase64Invalido() {
        contextRunner.withPropertyValues("app.security.mfa.encryption-key=Invalid@@Base64!!")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("valido");
                });
    }

    @Test
    void contextoNoIniciaConLlaveDistintaA32Bytes() {
        String invalidLength = Base64.getEncoder().encodeToString(new byte[16]);
        contextRunner.withPropertyValues("app.security.mfa.encryption-key=" + invalidLength)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("32 bytes");
                });
    }

    @Test
    void contextoIniciaConLlaveValida() {
        String validKey = Base64.getEncoder().encodeToString(new byte[32]);
        contextRunner.withPropertyValues("app.security.mfa.encryption-key=" + validKey)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(MfaCryptoService.class)).isNotNull();
                });
    }
}
