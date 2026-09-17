# Plan de Provisionamiento de Infraestructura: SDK_DEMO
**Aplicación de Referencia y Harness Visual del SDK Corporativo (`@rassini/rassini-ui`)**

---

## 1. Identificación de la Aplicación (`applicationCode`)

### Preguntas Clave
- **¿Existe actualmente?**
  **NO.** Tras consultar los registros de la base de datos `iam.applications` y los clientes OAuth existentes, `SDK_DEMO` no está registrada en el ecosistema.
- **¿Está registrado?**
  **NO.** Las aplicaciones registradas actualmente son `EMPLOYEE_PORTAL` y `MS_PAGOS`.
- **¿Dónde está registrado?**
  Debe registrarse en la tabla `iam.applications` (`code`, `name`, `description`, `active`, `is_internal`).
- **¿Qué impacto tiene agregarlo?**
  - Permite agrupar los permisos (`permissions.application_id`) y menús (`menus.application_id`) específicos de esta aplicación.
  - Habilita la vinculación de clientes OAuth2 (`oauth_clients.application_id`).
  - Activa la validación estricta en el SDK corporativo (`CorporateOidcConfig.applicationCode = 'SDK_DEMO'`), garantizando el aislamiento de estado y `sessionStorage` (`rassini:SDK_DEMO:{username}:activeBusinessUnit`).
  - **Cero impacto negativo** en aplicaciones existentes (`EMPLOYEE_PORTAL`, `MS_PAGOS`).
- **¿Cómo se eliminaría posteriormente?**
  Mediante un script de rollback transaccional que elimina en orden inverso: permisos/menús asociados -> cliente OAuth -> aplicación `SDK_DEMO`.

---

## 2. Cliente OAuth2 / OIDC

### Análisis del Authorization Server Actual (Spring Authorization Server embebido en 8083)
- **¿Existe un cliente para `SDK_DEMO`?**
  **NO.** Únicamente existe `ms-pagos-client` (ID: 1).
- **¿Cuál es su `clientId` propuesto?**
  `sdk-demo-client`.
- **¿Cuál es su tipo?**
  **Public Client (SPA)**. En `iam.oauth_clients`, el método de autenticación es `clientAuthenticationMethods = 'none'`. No requiere ni almacena `clientSecret` en el frontend.
- **¿Requiere PKCE?**
  **SÍ, OBLIGATORIO.** `require_proof_key = 1` con desafío `code_challenge_method = 'S256'`.
- **¿Usa Authorization Code?**
  **SÍ.** `authorization_grant_types = 'authorization_code,refresh_token'`.
- **¿Qué scopes tiene?**
  `openid,profile,email,roles,permissions,business_units,offline_access`.
- **¿Qué issuer usará?**
  `http://localhost:8083/employee-portal`.

### Propuesta Técnica Definitiva
```yaml
applicationCode: SDK_DEMO
clientId: sdk-demo-client
clientName: SDK Demo Reference Application (SPA)
grantTypes: authorization_code,refresh_token
clientType: public (client_authentication_methods: none)
pkce: S256 obligatorio (require_proof_key: true)
requireAuthorizationConsent: false
accessTokenTimeToLiveSeconds: 3600 (1 hora)
refreshTokenTimeToLiveSeconds: 86400 (24 horas)
reuseRefreshTokens: false
scopes:
  - openid
  - profile
  - email
  - roles
  - permissions
  - business_units
  - offline_access
```

> [!IMPORTANT]
> **NO se reutiliza `ms-pagos-client`**. Cada aplicación corporativa posee su propio `clientId` para garantizar aislamiento de auditoría, control de scopes y ciclo de vida de tokens.

---

## 3. Redirect URI & Allowed Origins

### Propuesta de Endpoints
- **Redirect URI Principal**: `http://localhost:4205/callback`
- **Post-Logout Redirect URI**: `http://localhost:4205`
- **Allowed Origins (CORS & Interceptor)**: `http://localhost:4205`

### Análisis
- **¿Está registrada?**
  **NO.** La única redirect URI registrada actualmente en `iam.oauth_client_redirect_uris` es `http://localhost:4201/callback` para `ms-pagos-client`.
- **¿Debe registrarse?**
  **SÍ.** El Authorization Server valida en base de datos la URI exacta antes de emitir el HTTP 302 con el Authorization Code.
- **¿Dónde se registra?**
  En la tabla `iam.oauth_client_redirect_uris` con los tipos:
  - `REDIRECT`: `http://localhost:4205/callback`
  - `POST_LOGOUT`: `http://localhost:4205`
- **¿Qué tabla se afecta?**
  `iam.oauth_client_redirect_uris`.
- **¿Qué impacto tiene?**
  Permite al navegador del usuario redirigir de forma autorizada hacia la SPA `SDK_DEMO` en el puerto 4205 sin error `invalid_redirect_uri`.

---

## 4. Puerto de la Demo

### Verificación de Puertos Locales
| Puerto | Estado / Asignación | Conflicto |
|:---|:---|:---|
| `8083` | Backend IAM & Authorization Server (`employee-portal`) | No conflictivo (Servicio Backend) |
| `4200` | Frontend Employee Portal (`employee-portal-ui`) | Reservado |
| `4201` | Frontend Portal de Pagos (`ms-pagos`) | Reservado |
| `4202` | Reservado para Portal RH | Sin conflicto actual |
| `4203` | Reservado para Portal Compras | Sin conflicto actual |
| `4204` | Reservado para Portal Proveedores | Sin conflicto actual |
| **`4205`** | **Propuesto para `SDK_DEMO`** | **LIBRE (Sin procesos activos detectados)** |

**Conclusión**: El puerto `4205` está disponible y no presenta colisiones.

---

## 5. Menús Corporativos para SDK_DEMO

### Estado Actual de `test_demo`
Actualmente el usuario `test_demo` solo posee asignado un menú:
- `PORTAL_DEMO` (Target: `EXTERNO`, hacia `https://httpbin.org/get`).

### Diagnóstico
**NO es adecuado ni suficiente** para probar las capacidades del nuevo SDK (`@rassini/rassini-ui`):
1. No posee rutas internas Angular (`route` está vacío).
2. No contiene nodos hijos para navegación de árbol en el sidebar (`NavigationService`).
3. No permite validar directivas estructurales (`*rassiniHasRole`, `*rassiniHasPermission`).
4. No permite probar los guards de ruta (`rassiniAuthGuard`, `rassiniRoleGuard`, `rassiniPermissionGuard`, `rassiniBusinessUnitGuard`).

### Configuración Mínima Propuesta de Menús
Se propone registrar una estructura de árbol en `iam.menus` vinculada a la aplicación `SDK_DEMO`:

```
SDK_DEMO_ROOT (Raíz - "SDK Demo App")
├── SDK_DEMO_HOME (Ruta interna: /home, Icono: pi pi-home)
├── SDK_DEMO_NAVIGATION (Ruta interna: /navigation, Icono: pi pi-compass)
├── SDK_DEMO_CONTEXT (Ruta interna: /context, Icono: pi pi-sliders-h)
├── SDK_DEMO_GUARDS (Ruta interna: /guards, Icono: pi pi-shield)
├── SDK_DEMO_DIRECTIVES (Ruta interna: /directives, Icono: pi pi-code)
└── SDK_DEMO_LOGOUT (Ruta interna: /logout, Icono: pi pi-sign-out)
```

---

## 6. Roles y Permisos

### Estado Actual de `test_demo`
- **Rol**: `ROLE_DEMO`
- **Permiso**: `PORTAL_DEMO_ACCESS`

### Diagnóstico
Solo posee 1 permiso genérico. Para validar guards permisivos/restrictivos y directivas condicionales se requiere:
1. Un permiso concedido a `test_demo` (para validar casos positivos).
2. Un permiso NO concedido (para validar casos negativos de directivas y redirección 403 a `/access-denied`).
3. Un rol de administración para probar elevación de privilegios.

### Propuesta Mínima de Roles y Permisos
- **Permisos a crear en `iam.permissions` (asociados a `SDK_DEMO`)**:
  1. `SDK_DEMO_ACCESS`: Acceso base a la aplicación demo.
  2. `SDK_DEMO_ADMIN`: Permiso de administración avanzada dentro de la demo.
  3. `SDK_DEMO_BU_TEST`: Permiso específico para pruebas de Business Units.
- **Asignación al rol `ROLE_DEMO` (que ya tiene `test_demo`)**:
  - Se vinculan `SDK_DEMO_ACCESS` y `SDK_DEMO_BU_TEST` a `ROLE_DEMO`.
  - El permiso `SDK_DEMO_ADMIN` **NO se asigna** a `ROLE_DEMO` (para verificar que las directivas y guards restrinjan el acceso de forma real en la pantalla de la demo).
- **Vinculación a Menús (`iam.permission_menu`)**:
  - `SDK_DEMO_ROOT` -> `SDK_DEMO_ACCESS`
  - `SDK_DEMO_HOME` -> `SDK_DEMO_ACCESS`
  - `SDK_DEMO_NAVIGATION` -> `SDK_DEMO_ACCESS`
  - `SDK_DEMO_CONTEXT` -> `SDK_DEMO_ACCESS`
  - `SDK_DEMO_GUARDS` -> `SDK_DEMO_ACCESS`
  - `SDK_DEMO_DIRECTIVES` -> `SDK_DEMO_ACCESS`

---

## 7. Business Units

### Estado Actual de `test_demo`
- **`hasAllBusinessUnits`**: `false`
- **BUs Asignadas (5 Unidades)**:
  1. `0111` - Corporativo (ID: 1)
  2. `09` - Piedras Negras (ID: 2)
  3. `99` - PN99 (ID: 3)
  4. `0301` - Bypasa (ID: 4)
  5. `1000` - Frenos (ID: 5)

### Diagnóstico
**SÍ es suficiente y óptimo para las pruebas:**
- Permite demostrar que al arrancar, con múltiples BUs, `activeBusinessUnit` inicia de forma segura en `null`.
- Permite verificar el selector de BUs en la UI seleccionando una BU autorizada (p. ej. `0111`).
- Permite demostrar el fallo controlado (`UnauthorizedBusinessUnitError`) si se intenta forzar una BU no autorizada (p. ej. `9999`).
- Permite validar la resolución de variables en `ContextService`:
  - `${BUSINESS_UNIT}` -> Resuelve a la BU activa elegida en el selector.
  - `${BUSINESS_UNITS}` -> Resuelve a `0111,09,99,0301,1000`.

---

## 8. Corporate Configuration Adapter para SDK_DEMO

### Configuración Estándar Requerida
```typescript
import { CorporateOidcConfig } from '@rassini/rassini-ui';

export const SDK_DEMO_OIDC_CONFIG: CorporateOidcConfig = {
  applicationCode: 'SDK_DEMO',
  clientId: 'sdk-demo-client',
  issuer: 'http://localhost:8083/employee-portal',
  redirectUri: 'http://localhost:4205/callback',
  postLogoutRedirectUri: 'http://localhost:4205',
  scope: 'openid profile email roles permissions business_units',
  allowedApiOrigins: [
    'http://localhost:8083'
  ]
};
```

### Mecánica de Integración
- **Dónde vivirá**:
  En el nuevo proyecto `SDK_DEMO` dentro del workspace (`SDK_DEMO/src/app/app.config.ts`), inyectado mediante:
  ```typescript
  export const appConfig: ApplicationConfig = {
    providers: [
      provideRouter(routes),
      provideHttpClient(withInterceptors([rassiniTokenInterceptor])),
      provideRassiniCorporate(SDK_DEMO_OIDC_CONFIG)
    ]
  };
  ```
- **Cómo se cargará**:
  A través de `provideRassiniCorporate(SDK_DEMO_OIDC_CONFIG)`, instanciando el `StaticCorporateConfigurationAdapter` y configurando `AuthenticationService`, `NavigationService` e interceptores.
- **Cómo se eliminaría posteriormente**:
  Borrando la carpeta `SDK_DEMO` del workspace sin dejar rastro en otras librerías.

---

## 9. Especificación de Scripts SQL y Rollback Exacto

> [!CAUTION]
> **ESTOS SCRIPTS NO SE HAN EJECUTADO TODAVÍA**. Se presentan a continuación para su revisión y autorización formal previa.

### 9.1 Script de Provisionamiento (Forward)
```sql
USE iam;

-- 1. Registrar la Aplicación SDK_DEMO
INSERT INTO applications (code, name, description, active, is_internal)
VALUES ('SDK_DEMO', 'SDK Demo Application', 'Aplicación de referencia y harness visual para @rassini/rassini-ui', 1, 1);

SET @sdk_app_id = (SELECT id FROM applications WHERE code = 'SDK_DEMO');

-- 2. Registrar el Cliente OAuth2 (Public Client SPA con PKCE)
INSERT INTO oauth_clients (
    client_id,
    client_secret,
    client_name,
    application_id,
    client_authentication_methods,
    authorization_grant_types,
    scopes,
    require_proof_key,
    require_authorization_consent,
    access_token_time_to_live_seconds,
    refresh_token_time_to_live_seconds,
    reuse_refresh_tokens,
    enabled
) VALUES (
    'sdk-demo-client',
    NULL,
    'SDK Demo Reference Client (SPA)',
    @sdk_app_id,
    'none',
    'authorization_code,refresh_token',
    'openid,profile,email,roles,permissions,business_units,offline_access',
    1,
    0,
    3600,
    86400,
    0,
    1
);

SET @sdk_client_id = (SELECT id FROM oauth_clients WHERE client_id = 'sdk-demo-client');

-- 3. Registrar Redirect URIs autorizadas
INSERT INTO oauth_client_redirect_uris (oauth_client_id, uri, uri_type)
VALUES 
    (@sdk_client_id, 'http://localhost:4205/callback', 'REDIRECT'),
    (@sdk_client_id, 'http://localhost:4205', 'POST_LOGOUT');

-- 4. Registrar Permisos para SDK_DEMO
INSERT INTO permissions (code, description, application_id)
VALUES 
    ('SDK_DEMO_ACCESS', 'Acceso general a SDK Demo App', @sdk_app_id),
    ('SDK_DEMO_ADMIN', 'Administración avanzada de SDK Demo App', @sdk_app_id),
    ('SDK_DEMO_BU_TEST', 'Pruebas de Business Units en SDK Demo App', @sdk_app_id);

SET @perm_access_id = (SELECT id FROM permissions WHERE code = 'SDK_DEMO_ACCESS');
SET @perm_bu_id = (SELECT id FROM permissions WHERE code = 'SDK_DEMO_BU_TEST');
SET @role_demo_id = (SELECT id FROM roles WHERE code = 'ROLE_DEMO');

-- 5. Asignar permisos al rol ROLE_DEMO (de test_demo)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
VALUES 
    (@role_demo_id, @perm_access_id),
    (@role_demo_id, @perm_bu_id);

-- 6. Registrar Menús de Navegación para SDK_DEMO
INSERT INTO menus (code, label, route, icon, order_index, target_type, open_in_new_tab, app_type, auth_type, application_id, parent_id)
VALUES 
    ('SDK_DEMO_ROOT', 'SDK Demo', '/home', 'pi pi-box', 10, 'INTERNO', 0, 'INTERNA', 'OIDC', @sdk_app_id, NULL);

SET @sdk_menu_root_id = (SELECT id FROM menus WHERE code = 'SDK_DEMO_ROOT');

INSERT INTO menus (code, label, route, icon, order_index, target_type, open_in_new_tab, app_type, auth_type, application_id, parent_id)
VALUES 
    ('SDK_DEMO_HOME', 'Inicio & Sesión', '/home', 'pi pi-home', 1, 'INTERNO', 0, 'INTERNA', 'OIDC', @sdk_app_id, @sdk_menu_root_id),
    ('SDK_DEMO_NAVIGATION', 'Navegación & Menús', '/navigation', 'pi pi-compass', 2, 'INTERNO', 0, 'INTERNA', 'OIDC', @sdk_app_id, @sdk_menu_root_id),
    ('SDK_DEMO_CONTEXT', 'Contexto & BUs', '/context', 'pi pi-sliders-h', 3, 'INTERNO', 0, 'INTERNA', 'OIDC', @sdk_app_id, @sdk_menu_root_id),
    ('SDK_DEMO_GUARDS', 'Validación Guards', '/guards', 'pi pi-shield', 4, 'INTERNO', 0, 'INTERNA', 'OIDC', @sdk_app_id, @sdk_menu_root_id),
    ('SDK_DEMO_DIRECTIVES', 'Directivas UI', '/directives', 'pi pi-code', 5, 'INTERNO', 0, 'INTERNA', 'OIDC', @sdk_app_id, @sdk_menu_root_id);

-- 7. Asociar Menús a Permisos (permission_menu)
INSERT INTO permission_menu (permission_id, menu_id)
SELECT @perm_access_id, id FROM menus WHERE application_id = @sdk_app_id;
```

---

### 9.2 Script de Rollback Exacto (Reversible)
```sql
USE iam;

-- Desactivar llaves foráneas temporalmente para limpieza limpia y atómica
SET @sdk_app_id = (SELECT id FROM applications WHERE code = 'SDK_DEMO');
SET @sdk_client_id = (SELECT id FROM oauth_clients WHERE client_id = 'sdk-demo-client');

-- 1. Eliminar vínculos permission_menu
DELETE pm FROM permission_menu pm
JOIN menus m ON pm.menu_id = m.id
WHERE m.application_id = @sdk_app_id;

-- 2. Eliminar menús hijos y padre
DELETE FROM menus WHERE application_id = @sdk_app_id;

-- 3. Eliminar vínculos role_permissions
DELETE rp FROM role_permissions rp
JOIN permissions p ON rp.permission_id = p.id
WHERE p.application_id = @sdk_app_id;

-- 4. Eliminar permisos
DELETE FROM permissions WHERE application_id = @sdk_app_id;

-- 5. Eliminar URIs de redirección OAuth
DELETE FROM oauth_client_redirect_uris WHERE oauth_client_id = @sdk_client_id;

-- 6. Eliminar cliente OAuth
DELETE FROM oauth_clients WHERE id = @sdk_client_id;

-- 7. Eliminar aplicación SDK_DEMO
DELETE FROM applications WHERE id = @sdk_app_id;
```

---

## 10. Conclusión y Estado del Checkpoint

1. El ecosistema IAM / Authorization Server está listo para incorporar `SDK_DEMO` de forma totalmente desacoplada sin alterar `ms-pagos` ni `employee-portal-ui`.
2. El puerto `4205` está disponible y no presenta colisiones.
3. El usuario `test_demo` tiene las unidades de negocio idóneas para demostrar el comportamiento multi-BU y resolución de contexto.
4. **No se ha ejecutado ninguna sentencia SQL, no se ha creado la carpeta `SDK_DEMO` ni se ha generado código Angular**, en estricto cumplimiento de la Fase A.
