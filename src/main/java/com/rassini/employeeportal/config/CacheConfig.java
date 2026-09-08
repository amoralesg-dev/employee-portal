package com.rassini.employeeportal.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String MFA_SETUP_CACHE = "mfaSetupCache";
    public static final String MFA_ATTEMPTS_CACHE = "mfaAttemptsCache";
    public static final String MFA_CONSUMED_CACHE = "mfaConsumedCache";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(MFA_SETUP_CACHE, MFA_ATTEMPTS_CACHE, MFA_CONSUMED_CACHE);

        // Configuracion compartida para MVP
        // Idealmente, se podrian configurar diferentes TTL por cache, pero para el MVP
        // usamos 10 minutos global (MFA_SETUP exige 10 min, y los intentos expiran despues del limite).
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(20, TimeUnit.MINUTES)
                .maximumSize(10000));

        return cacheManager;
    }
}
