package com.rassini.employeeportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Propiedades CORS leídas desde application-{profile}.yml
 * bajo el prefijo app.cors
 */
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * Orígenes permitidos. Se configura por perfil en application-{dev|qa|prod}.yml.
     * Ejemplo:
     *   app.cors.allowed-origins[0]: http://localhost:4200
     */
    private List<String> allowedOrigins = List.of("http://localhost:4200");

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
