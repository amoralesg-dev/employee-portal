# Employee Portal — Backend API

Backend del **Portal de Empleados Rassini**, construido con Spring Boot 3.4.x y Java 21.

## Tecnologías

| Tecnología | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.x GA |
| Spring Data JPA | — |
| MySQL | 8.x |
| Springdoc OpenAPI (Swagger) | 2.8.x |
| Spring Boot Actuator | — |
| Micrometer / Prometheus | — |
| Lombok | — |

## Base de datos

- **Motor**: MySQL 8.x
- **Esquema**: `iam`
- La conexión se configura por perfil en los archivos `application-{dev|qa|prod}.yml`
- Hibernate está configurado con `ddl-auto: none` — **no crea ni modifica tablas**

## Configuración por ambiente

Los archivos de configuración YAML están en `src/main/resources/`:

| Archivo | Puerto | Propósito |
|---|---|---|
| `application.yml` | — | Configuración común a todos los ambientes |
| `application-dev.yml` | 8083 | Desarrollo local |
| `application-qa.yml` | 8084 | Pruebas QA |
| `application-prod.yml` | 8080 | Producción |

> **Importante**: Reemplaza `REPLACE_WITH_USER` y `REPLACE_WITH_PASSWORD` en cada YAML con las credenciales reales.

## Cómo ejecutar

### Perfil DEV (puerto 8083)

```bash
# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Windows
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

### Perfil QA (puerto 8084)

```bash
# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=qa

# Windows
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=qa
```

### Perfil PROD (puerto 8080)

```bash
# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

# Windows
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod
```

### Desde VS Code / Antigravity IDE

Usa las configuraciones en `.vscode/launch.json`:
- **Employee Portal DEV** → puerto 8083
- **Employee Portal QA** → puerto 8084
- **Employee Portal PROD** → puerto 8080

## Seguridad y JWT

El backend utiliza Spring Security y JWT para proteger la API.

### Configuración del Secreto JWT
En cada ambiente o al ejecutar localmente, se debe inyectar la variable de entorno `JWT_SECRET` (mínimo 256 bits).
Si no se provee en entorno `dev`, utilizará una clave hardcodeada por defecto, pero **en producción siempre debes configurarla**.

### Endpoints
- **Públicos**: `/api/v1/auth/login`, `/api/v1/index`, `/actuator/health`, Swagger y API Docs.
- **Protegidos**: Todos los demás (ej. `/api/v1/users/**`, `/api/v1/roles/**`).

### Flujo de Login
1. Haz un POST a `/api/v1/auth/login` con:
   ```json
   {
     "username": "admin",
     "password": "miPasswordEnBcrypt"
   }
   ```
2. La API devolverá un `accessToken`.
3. Para siguientes peticiones, incluye el token en el Header:
   `Authorization: Bearer <tu_token_aqui>`

*Nota: La contraseña en base de datos debe estar hasheada con BCrypt.*

## URLs útiles (perfil DEV)

| Recurso | URL |
|---|---|
| API Index | http://localhost:8083/employee-portal/api/v1/index |
| Swagger UI | http://localhost:8083/employee-portal/swagger-ui.html |
| OpenAPI Docs | http://localhost:8083/employee-portal/v3/api-docs |
| Actuator Health | http://localhost:8083/employee-portal/actuator/health |
| Actuator Info | http://localhost:8083/employee-portal/actuator/info |
| Actuator Metrics | http://localhost:8083/employee-portal/actuator/metrics |
| Prometheus | http://localhost:8083/employee-portal/actuator/prometheus |

## Estructura del proyecto

```
src/main/java/com/rassini/employeeportal/
├── EmployeePortalApplication.java   ← Clase principal
├── config/                          ← Configuraciones Spring (OpenAPI, etc.)
├── controller/                      ← Controladores REST
├── dto/
│   ├── request/                     ← DTOs de entrada
│   └── response/                    ← DTOs de salida
├── entity/                          ← Entidades JPA
├── exception/                       ← Excepciones personalizadas
├── mapper/                          ← Mappers entidad ↔ DTO
├── repository/                      ← Repositorios JPA
├── service/                         ← Interfaces de servicios
└── service/impl/                    ← Implementaciones de servicios
```

## Compilar y correr tests

```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```
"# employee-portal" 
