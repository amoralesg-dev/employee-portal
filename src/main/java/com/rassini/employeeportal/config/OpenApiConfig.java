package com.rassini.employeeportal.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Portal API")
                        .description("API backend del Portal de Empleados")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }
}
