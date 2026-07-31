package com.rassini.employeeportal.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Springdoc OpenAPI / Swagger UI.
 * Swagger UI disponible en: /employee-portal/swagger-ui.html
 * API Docs disponible en:   /employee-portal/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI employeePortalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Portal API")
                        .description("API backend del Portal de Empleados")
                        .version("v1"));
    }
}
