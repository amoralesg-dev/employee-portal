# PLAN.md — Plan de Fases del Employee Portal

Documento de planificación técnica del backend del Portal de Empleados.

## Estado Actual

- **Fase 1**: ✅ Completada

---

## Fase 1: Infraestructura Base ✅

**Objetivo**: Configurar la base del proyecto Spring Boot con todas las dependencias y configuración de ambiente.

### Completado:
- [x] `pom.xml` con dependencias correctas (Spring Web, JPA, Validation, Actuator, Prometheus, Springdoc, Lombok, MySQL, Mockito)
- [x] Spring Boot 3.4.x GA con Java 21
- [x] `application.yml` — configuración común
- [x] `application-dev.yml` — perfil desarrollo (puerto 8083)
- [x] `application-qa.yml` — perfil QA (puerto 8084)
- [x] `application-prod.yml` — perfil producción (puerto 8080)
- [x] `OpenApiConfig.java` — Swagger/OpenAPI configurado
- [x] `IndexController.java` — endpoint GET /api/v1/index
- [x] Estructura de paquetes base creada
- [x] `.vscode/launch.json` — configuraciones DEV, QA, PROD
- [x] `.vscode/settings.json` — configuración Java 21
- [x] `README.md`, `AGENTS.md`, `PLAN.md`
- [x] Actuator con health, info, metrics, prometheus
- [x] Hibernate ddl-auto=none
- [x] Naming strategy snake_case

---

## Fase 2: Entidades JPA y Repositories

**Objetivo**: Mapear el modelo físico del esquema `iam` a entidades JPA y crear los repositorios base.

### Tareas pendientes:
- [ ] Crear entidad `User` → tabla `users`
- [ ] Crear entidad `Role` → tabla `roles`
- [ ] Crear entidad `Permission` → tabla `permissions`
- [ ] Crear entidad `Menu` → tabla `menus`
- [ ] Crear entidades de unión: `UserRole`, `RolePermission`, `PermissionMenu`
- [ ] Crear `UserRepository extends JpaRepository<User, Long>`
- [ ] Crear `RoleRepository extends JpaRepository<Role, Long>`
- [ ] Crear `PermissionRepository extends JpaRepository<Permission, Long>`
- [ ] Crear `MenuRepository extends JpaRepository<Menu, Long>`
- [ ] Validar que las entidades mapean correctamente a las tablas del esquema `iam`

---

## Fase 3: DTOs, Mappers y Excepciones

**Objetivo**: Definir contratos de API claros y manejo de errores uniforme.

### Tareas pendientes:
- [ ] Crear `UserRequest` y `UserResponse` DTOs
- [ ] Crear `RoleRequest` y `RoleResponse` DTOs
- [ ] Crear `PermissionRequest` y `PermissionResponse` DTOs
- [ ] Crear `MenuRequest` y `MenuResponse` DTOs
- [ ] Crear `ApiResponse<T>` wrapper genérico
- [ ] Crear `ErrorResponse` DTO
- [ ] Crear mappers (manual o MapStruct) entidad ↔ DTO
- [ ] Crear `GlobalExceptionHandler` con `@RestControllerAdvice`
- [ ] Crear excepciones: `ResourceNotFoundException`, `BusinessException`, `ValidationException`

---

## Fase 4: Services y Lógica de Acceso

**Objetivo**: Implementar la lógica de negocio en servicios.

### Tareas pendientes:
- [ ] Crear `UserService` interface + `UserServiceImpl`
- [ ] Crear `RoleService` interface + `RoleServiceImpl`
- [ ] Crear `PermissionService` interface + `PermissionServiceImpl`
- [ ] Crear `MenuService` interface + `MenuServiceImpl`
- [ ] CRUD de usuarios
- [ ] Asignación de roles a usuarios
- [ ] Asignación de permisos a roles
- [ ] Consulta de menús por perfil de usuario

---

## Fase 5: Controllers, Swagger y Pruebas

**Objetivo**: Exponer la API REST completa con documentación Swagger y pruebas automatizadas.

### Tareas pendientes:
- [ ] `UserController` con endpoints CRUD
- [ ] `RoleController` con endpoints CRUD
- [ ] `PermissionController` con endpoints CRUD
- [ ] `MenuController` con endpoints CRUD
- [ ] Documentación Swagger completa con `@Operation`, `@ApiResponse`, `@Tag`
- [ ] Tests unitarios para services con Mockito
- [ ] Tests de integración con `@WebMvcTest` para controllers
- [ ] Tests de repositorio con `@DataJpaTest`
- [ ] Validación de endpoints con Postman / Swagger UI

---

## Notas Generales

- Todos los endpoints bajo `/employee-portal/api/v1/`
- Respuestas envueltas en `ApiResponse<T>`
- Manejo uniforme de errores via `GlobalExceptionHandler`
- Logs estructurados por perfil (DEBUG en dev, INFO en qa/prod)
- Nunca exponer `password_hash` en responses
