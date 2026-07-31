# AGENTS.md — Reglas del Proyecto Employee Portal

Este archivo define las reglas que todos los agentes de IA y desarrolladores deben seguir en este proyecto.

## Reglas Obligatorias

### Package base
- **Package base obligatorio**: `com.rassini.employeeportal`
- Toda clase Java debe pertenecer a este package o a un subpackage del mismo
- No crear clases fuera de `com.rassini.employeeportal.*`

### Base de datos
- **Motor**: MySQL 8.x únicamente
- **Esquema**: `iam`
- **PROHIBIDO** usar H2, PostgreSQL o MongoDB
- **PROHIBIDO** usar `ddl-auto=create`
- **PROHIBIDO** usar `ddl-auto=update`
- `ddl-auto` debe ser siempre `none`
- No crear tablas nuevas sin una historia de usuario aprobada

### Seguridad / Privacidad
- **NUNCA** exponer el campo `password_hash` en ningún DTO de respuesta
- **NUNCA** exponer el campo `password_hash` en logs
- No incluir información sensible en responses de error públicos

### Modelo físico
- No inventar tablas nuevas que no existan en el esquema `iam`
- Tablas aprobadas del modelo: `users`, `roles`, `permissions`, `menus`, `user_roles`, `role_permissions`, `permission_menu`
- Toda entidad JPA debe mapearse a una tabla existente en el esquema

### Configuración
- No usar `application.properties` — solo YAML por ambiente
- Perfiles válidos: `dev`, `qa`, `prod`
- No hardcodear credenciales — usar `REPLACE_WITH_USER` / `REPLACE_WITH_PASSWORD` como placeholders

### Dependencias
- No agregar dependencias sin justificación explícita
- No usar Flyway hasta que sea parte del plan aprobado
- No usar Spring Security hasta que sea parte del plan aprobado

### Tests
- No usar `@SpringBootTest` que levante contexto completo a menos que sea necesario
- Usar `@WebMvcTest` para controllers
- Usar `@DataJpaTest` para repositories (cuando se configure H2 solo para tests si aplica)

## Herramientas de IA

Los agentes deben:
1. Leer este archivo antes de comenzar cualquier tarea
2. Respetar la estructura de paquetes definida
3. No agregar lógica compleja sin validar contra el plan de fases en `PLAN.md`
4. No modificar el `pom.xml` sin verificar compatibilidad con Spring Boot 3.4.x / Java 21
