package com.rassini.employeeportal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test de arranque del contexto Spring.
 * Usa propiedades de test para evitar requerir conexión a MySQL.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.cors.allowed-origins[0]=http://localhost:4200"
})
class EmployeePortalApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto Spring Boot inicia correctamente
    }

}
