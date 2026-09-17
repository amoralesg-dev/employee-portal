# Referencia de Implementación SSO — Portal Pagos (`MS_PAGOS`)

Este documento documenta el caso de referencia de cliente configurado en la Fase 2 para **Portal Pagos** (`MS_PAGOS`).

---

## 1. Configuración del Cliente en Base de Datos

El cliente fue registrado en las tablas relacionales del esquema `iam`:

```sql
-- 1. Aplicación
SELECT * FROM iam.applications WHERE code = 'MS_PAGOS';

-- 2. Cliente OAuth2
SELECT * FROM iam.oauth_clients WHERE client_id = 'ms-pagos-client';

-- 3. URIs autorizadas
SELECT * FROM iam.oauth_client_redirect_uris WHERE oauth_client_id = (SELECT id FROM iam.oauth_clients WHERE client_id = 'ms-pagos-client');
```

### Parámetros Registrados
| Parámetro | Valor |
| :--- | :--- |
| **`clientId`** | `ms-pagos-client` |
| **`clientSecret`** | `null` (Público / SPA) |
| **`clientName`** | `Portal Pagos Client (SPA)` |
| **`applicationCode`** | `MS_PAGOS` |
| **`authenticationMethods`** | `none` |
| **`grantTypes`** | `authorization_code,refresh_token` |
| **`scopes`** | `openid,profile,email,roles,permissions,business_units` |
| **`requireProofKey` (PKCE)** | `true` (Obligatorio) |
| **`redirectUris`** | `http://localhost:4201/callback` |
| **`postLogoutRedirectUris`** | `http://localhost:4201/login` |

---

## 2. Parámetros de Petición de Autorización (Authorization Code + PKCE)

Para iniciar el flujo desde la SPA de `ms-pagos`:

```http
GET /employee-portal/oauth2/authorize?
    response_type=code&
    client_id=ms-pagos-client&
    redirect_uri=http%3A%2F%2Flocalhost%3A4201%2Fcallback&
    scope=openid%20profile%20email%20roles%20permissions%20business_units&
    code_challenge=ZoSHTYKq1mb9ydY8XPXYWULhyxp_1fqzVxRyMAwUx8M&
    code_challenge_method=S256&
    state=dYL_Go3P_yG7sOr_eJNv-Q
Host: localhost:8083
```

---

## 3. Canje de Token

```http
POST /employee-portal/oauth2/token
Host: localhost:8083
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&client_id=ms-pagos-client
&code=SplxlOBeZQQYbYS6WxSbIA
&redirect_uri=http%3A%2F%2Flocalhost%3A4201%2Fcallback
&code_verifier=6IaXsHcc2kLmuYJ2hfR3y0MEt04m_5fipfRVWTTTb2w
```

### Claims Obtenidos en el Access Token (RS256)
```json
{
  "sub": "test_demo",
  "aud": "ms-pagos-client",
  "employeeId": "15",
  "email": "test_demo@rassini.com",
  "roles": ["ROLE_DEMO_OPERATOR"],
  "permissions": ["PORTAL_DEMO_VIEW", "PORTAL_DEMO_ACCESS"],
  "businessUnits": [
    { "id": 1, "code": "0111", "name": "Planta San Martin" },
    { "id": 2, "code": "0301", "name": "Planta Xalostoc" },
    { "id": 3, "code": "09",   "name": "Corporativo" },
    { "id": 4, "code": "1000", "name": "Planta Puebla" },
    { "id": 5, "code": "99",   "name": "Servicios Compartidos" }
  ],
  "hasAllBusinessUnits": false,
  "bu": "0111,0301,09,1000,99"
}
```

---

## 4. Flujo de Cierre de Sesión (RP-Initiated Logout)

Para cerrar la sesión corporativa y redirigir al usuario al portal de pagos:

```http
GET /employee-portal/connect/logout?
    id_token_hint=<ID_TOKEN_DEL_USUARIO>&
    post_logout_redirect_uri=http%3A%2F%2Flocalhost%3A4201%2Flogin&
    state=state_logout_123
Host: localhost:8083
```
