import os
from PIL import Image, ImageDraw, ImageFont

DIAGRAMS_DIR = r"C:\workspace\employee-portal\docs\diagrams"
os.makedirs(DIAGRAMS_DIR, exist_ok=True)

def get_font(size, bold=False):
    try:
        font_name = "arialbd.ttf" if bold else "arial.ttf"
        return ImageFont.truetype(font_name, size)
    except:
        return ImageFont.load_default()

def draw_header(draw, title, subtitle, width=1200):
    draw.rectangle([(0, 0), (width, 80)], fill=(242, 101, 34)) # Rassini Orange
    font_title = get_font(24, bold=True)
    font_sub = get_font(14, bold=False)
    draw.text((30, 16), title, fill=(255, 255, 255), font=font_title)
    draw.text((30, 48), subtitle, fill=(255, 240, 230), font=font_sub)

def draw_card(draw, xy, fill=(255, 255, 255), outline=(209, 213, 219), radius=8):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=2)

# =========================================================================
# 1. ecosystem-architecture.png
# =========================================================================
def gen_ecosystem():
    w, h = 1200, 750
    img = Image.new("RGB", (w, h), (248, 249, 250))
    d = ImageDraw.Draw(img)
    draw_header(d, "Arquitectura de Ecosistema SSO & IAM Rassini", "Mapa general de componentes, responsabilidades y comunicación de red", w)

    draw_card(d, (50, 120, 280, 240), fill=(238, 242, 255), outline=(99, 102, 241))
    d.text((70, 140), "Empleado / Usuario", fill=(30, 27, 75), font=get_font(18, bold=True))
    d.text((70, 175), "• Navegador Web\n• Dispositivo Corporativo\n• Sesión SSO Activa", fill=(71, 85, 105), font=get_font(14))

    draw_card(d, (350, 120, 700, 240), fill=(254, 243, 199), outline=(245, 158, 11))
    d.text((370, 140), "Employee Portal (Angular UI)", fill=(120, 53, 15), font=get_font(18, bold=True))
    d.text((370, 175), "• Single Pane of Glass / Launcher\n• Menú Dinámico Autorizado\n• CERO Tokens en URL (Regla de Oro)", fill=(71, 85, 105), font=get_font(14))

    draw_card(d, (770, 120, 1150, 240), fill=(255, 237, 213), outline=(249, 115, 22))
    d.text((790, 140), "IAM Central (Spring Boot Backend)", fill=(154, 52, 18), font=get_font(18, bold=True))
    d.text((790, 175), "• Proveedor de Identidad (IdP / OIDC)\n• Emisión y Firma de JWTs\n• Catálogo de Menús, Roles y Permisos", fill=(71, 85, 105), font=get_font(14))

    draw_card(d, (770, 280, 1150, 390), fill=(241, 245, 249), outline=(148, 163, 184))
    d.text((790, 295), "Base de Datos MySQL: `iam`", fill=(30, 41, 59), font=get_font(18, bold=True))
    d.text((790, 330), "• users, roles, permissions, menus\n• menu_parameters, applications", fill=(71, 85, 105), font=get_font(14))

    # Satélites Internos
    d.rounded_rectangle([(50, 440), (600, 710)], radius=12, fill=(240, 253, 244), outline=(34, 197, 94), width=2)
    d.text((70, 460), "Aplicaciones Internas Rassini (Caso A)", fill=(20, 83, 45), font=get_font(18, bold=True))
    
    draw_card(d, (70, 500, 300, 590), fill=(255, 255, 255), outline=(187, 247, 208))
    d.text((85, 515), "Portal Proveedores", fill=(22, 101, 52), font=get_font(15, bold=True))
    d.text((85, 545), "SSO_IAM / OIDC Delegado", fill=(100, 116, 139), font=get_font(12))

    draw_card(d, (330, 500, 570, 590), fill=(255, 255, 255), outline=(187, 247, 208))
    d.text((345, 515), "Portal de Pagos", fill=(22, 101, 52), font=get_font(15, bold=True))
    d.text((345, 545), "SSO_IAM / OIDC Delegado", fill=(100, 116, 139), font=get_font(12))

    draw_card(d, (70, 605, 300, 695), fill=(255, 255, 255), outline=(187, 247, 208))
    d.text((85, 620), "Portal de RH", fill=(22, 101, 52), font=get_font(15, bold=True))
    d.text((85, 650), "SSO_IAM / OIDC Delegado", fill=(100, 116, 139), font=get_font(12))

    # Satélites Externos / SaaS
    d.rounded_rectangle([(650, 440), (1150, 710)], radius=12, fill=(243, 244, 246), outline=(156, 163, 175), width=2)
    d.text((670, 460), "Aplicaciones Externas / SaaS / Terceros (Caso B y C)", fill=(31, 41, 55), font=get_font(18, bold=True))

    draw_card(d, (670, 500, 880, 590), fill=(255, 255, 255), outline=(229, 231, 235))
    d.text((685, 515), "SaaS en la Nube", fill=(55, 65, 81), font=get_font(15, bold=True))
    d.text((685, 545), "Federación OIDC / SAML", fill=(100, 116, 139), font=get_font(12))

    draw_card(d, (910, 500, 1120, 590), fill=(255, 255, 255), outline=(229, 231, 235))
    d.text((925, 515), "Portal de Tercero", fill=(55, 65, 81), font=get_font(15, bold=True))
    d.text((925, 545), "Credenciales Propias", fill=(100, 116, 139), font=get_font(12))

    d.line([(280, 180), (350, 180)], fill=(99, 102, 241), width=3)
    d.line([(700, 180), (770, 180)], fill=(249, 115, 22), width=3)
    d.line([(960, 240), (960, 280)], fill=(148, 163, 184), width=3)
    d.line([(525, 240), (325, 440)], fill=(34, 197, 94), width=3)
    d.line([(525, 240), (900, 440)], fill=(156, 163, 175), width=3)

    img.save(os.path.join(DIAGRAMS_DIR, "ecosystem-architecture.png"), "PNG")
    print("1. ecosystem-architecture.png OK")

# =========================================================================
# 2. sso-sequence-flow.png
# =========================================================================
def gen_sso_sequence():
    w, h = 1200, 800
    img = Image.new("RGB", (w, h), (255, 255, 255))
    d = ImageDraw.Draw(img)
    draw_header(d, "Flujo de Secuencia SSO Delegado (OAuth2 / OIDC)", "Navegación segura y autenticación sin credenciales en URL", w)

    # Columnas (Lifelines)
    actors = [
        ("Empleado (Navegador)", 150),
        ("Employee Portal (UI)", 450),
        ("Portal Proveedores (App)", 750),
        ("IAM Central (OIDC)", 1050)
    ]
    for name, x in actors:
        draw_card(d, (x-110, 100, x+110, 140), fill=(241, 245, 249), outline=(203, 213, 225))
        d.text((x-90, 112), name, fill=(30, 41, 59), font=get_font(13, bold=True))
        d.line([(x, 140), (x, 750)], fill=(226, 232, 240), width=2)

    # Steps
    steps = [
        (150, 450, 170, "1. Clic en Menú 'Portal Proveedores' (target_type=EXTERNO)", (30, 41, 59)),
        (450, 750, 220, "2. window.open('https://portalproveedores.rassini.com/home') [SIN TOKENS EN URL]", (220, 38, 38)),
        (750, 750, 270, "3. App verifica sesión local -> NO DETECTADA", (100, 116, 139)),
        (750, 150, 320, "4. Redirección HTTP 302 a IAM: /oauth2/authorize?client_id=app_prov&...", (30, 41, 59)),
        (150, 1050, 370, "5. Aterriza en IAM con Cookie de Sesión SSO de Rassini", (249, 115, 22)),
        (1050, 1050, 420, "6. IAM valida sesión activa y autorización para cliente", (249, 115, 22)),
        (1050, 150, 470, "7. Redirige a redirect_uri con authorization_code temporal", (30, 41, 59)),
        (150, 750, 520, "8. Navegador entrega authorization_code al backend de la App", (30, 41, 59)),
        (750, 1050, 580, "9. POST /oauth2/token (code + client_secret) [Backchannel Seguro]", (16, 185, 129)),
        (1050, 750, 630, "10. IAM devuelve JWT firmado (ID Token + Access Token)", (16, 185, 129)),
        (750, 150, 690, "11. App crea sesión local y renderiza pantalla autenticada", (37, 99, 235))
    ]

    for x1, x2, y, text, color in steps:
        if x1 == x2:
            # Self action
            d.rectangle([(x1-15, y-10), (x1+220, y+20)], fill=(248, 250, 252), outline=(203, 213, 225))
            d.text((x1-5, y-3), text, fill=color, font=get_font(12, bold=True))
        else:
            d.line([(x1, y), (x2, y)], fill=color, width=2)
            arrow_dir = 1 if x2 > x1 else -1
            d.polygon([(x2, y), (x2 - 8*arrow_dir, y-5), (x2 - 8*arrow_dir, y+5)], fill=color)
            mid_x = min(x1, x2) + 20
            d.text((mid_x, y-18), text, fill=color, font=get_font(12, bold=True))

    img.save(os.path.join(DIAGRAMS_DIR, "sso-sequence-flow.png"), "PNG")
    print("2. sso-sequence-flow.png OK")

# =========================================================================
# 3. jwt-validation-flow.png
# =========================================================================
def gen_jwt_validation():
    w, h = 1200, 680
    img = Image.new("RGB", (w, h), (248, 249, 250))
    d = ImageDraw.Draw(img)
    draw_header(d, "Validación Criptográfica y Extracción de Claims del JWT", "Flujo de inspección offline en APIs y microservicios satélite", w)

    boxes = [
        (50, 140, 230, 280, "1. Petición Entrante", "Authorization:\nBearer <token>\n(Header HTTP)", (238, 242, 255), (99, 102, 241)),
        (280, 140, 480, 280, "2. Verificar Firma", "Validar firma HMAC / RSA\ncon SecretKey o JWKS\n¿Firma Alterada? -> 401", (254, 242, 242), (239, 68, 68)),
        (530, 140, 730, 280, "3. Expiración (exp)", "now() < exp (con 60s skew)\n¿Expiró? -> 401 Expired\nProceder a Refresh Token", (254, 243, 199), (245, 158, 11)),
        (780, 140, 980, 280, "4. Emisor y Audiencia", "iss == 'https://iam...'\naud contiene client_id\n¿Inválido? -> 401", (255, 237, 213), (249, 115, 22)),
        (1010, 140, 1160, 280, "5. Claims OK", "Token Válido\nContinuar a\nExtracción", (240, 253, 244), (34, 197, 94))
    ]

    for x1, y1, x2, y2, title, desc, fill, outline in boxes:
        draw_card(d, (x1, y1, x2, y2), fill=fill, outline=outline)
        d.text((x1+15, y1+15), title, fill=(30, 41, 59), font=get_font(15, bold=True))
        d.text((x1+15, y1+45), desc, fill=(71, 85, 105), font=get_font(13))

    for i in range(len(boxes)-1):
        x_start = boxes[i][2]
        x_end = boxes[i+1][0]
        y_mid = 210
        d.line([(x_start, y_mid), (x_end, y_mid)], fill=(156, 163, 175), width=3)
        d.polygon([(x_end, y_mid), (x_end-8, y_mid-5), (x_end-8, y_mid+5)], fill=(156, 163, 175))

    # Extracción y Autorización
    d.rounded_rectangle([(50, 340), (1160, 630)], radius=12, fill=(255, 255, 255), outline=(209, 213, 219), width=2)
    d.text((75, 360), "Extracción de Contexto Organizacional y Autorización en Spring Boot", fill=(30, 41, 59), font=get_font(17, bold=True))

    claims_data = [
        ("Identidad Principal", "sub: 'amoralesg'\nemployeeId: '12345'\nemail: 'amoralesg@rassini.com'", (238, 242, 255)),
        ("Autorización por Roles", "roles: ['PAYMENTS_ADMIN']\nMapeado a:\nROLE_PAYMENTS_ADMIN", (240, 253, 244)),
        ("Autorización Fina", "permissions: [\n  'PAYMENTS_VIEW',\n  'PAYMENTS_EDIT'\n]", (254, 243, 199)),
        ("Aislamiento de Planta", "businessUnits: [\n  '1850', '0111'\n]\nFiltro multi-tenancy", (255, 237, 213))
    ]

    for idx, (ctitle, cdesc, cfill) in enumerate(claims_data):
        bx1 = 80 + idx * 265
        bx2 = bx1 + 245
        draw_card(d, (bx1, 410, bx2, 600), fill=cfill, outline=(203, 213, 225))
        d.text((bx1+15, 425), ctitle, fill=(30, 41, 59), font=get_font(15, bold=True))
        d.text((bx1+15, 465), cdesc, fill=(71, 85, 105), font=get_font(13))

    img.save(os.path.join(DIAGRAMS_DIR, "jwt-validation-flow.png"), "PNG")
    print("3. jwt-validation-flow.png OK")

# =========================================================================
# 4. application-integration-flow.png
# =========================================================================
def gen_integration_flow():
    w, h = 1200, 750
    img = Image.new("RGB", (w, h), (248, 249, 250))
    d = ImageDraw.Draw(img)
    draw_header(d, "Guía Operativa de Integración 100% Administrativa (UI)", "6 Pasos para registrar y publicar una nueva aplicación en el Employee Portal", w)

    steps = [
        ("Paso 1: Crear Aplicación", "Pantalla: `/aplicaciones`\n\n• Código único (ej. PORTAL_PAGOS)\n• Nombre y Descripción\n• Client ID y Redirect URI\n• is_internal = true (Interna Rassini)", (238, 242, 255), (99, 102, 241)),
        ("Paso 2: Crear Menú", "Pantalla: `/menus`\n\n• Tipo Destino: EXTERNO\n• URL Externa (ej. https://pagos...)\n• AuthType: SSO_IAM\n• Abrir en nueva pestaña: true (_blank)\n• Params: bu = ${BUSINESS_UNIT}", (254, 243, 199), (245, 158, 11)),
        ("Paso 3: Crear Permiso", "Pantalla: `/permisos`\n\n• Código: PAGOS_ACCESS\n• Descripción de acceso funcional\n• Guardar catálogo", (240, 253, 244), (34, 197, 94)),
        ("Paso 4: Asociar Menú a Permiso", "Pantalla: `/permisos`\n\n• Acción: 'Asociar Menús'\n• Seleccionar PAGOS_MENU\n• Confirmar vinculación en modal", (255, 237, 213), (249, 115, 22)),
        ("Paso 5: Asignar a Roles", "Pantalla: `/roles`\n\n• Seleccionar Roles autorizados\n• Acción: 'Editar Permisos'\n• Marcar permiso PAGOS_ACCESS", (243, 232, 255), (168, 85, 247)),
        ("Paso 6: Validar Acceso", "Pantalla: `Employee Portal`\n\n• Login con usuario autorizado\n• Menú lateral muestra nueva opción\n• Clic abre app sin tokens en URL\n• SSO IAM autentica al usuario", (254, 242, 242), (239, 68, 68))
    ]

    # Fila 1 (Pasos 1 a 3)
    for i in range(3):
        x1 = 60 + i * 380
        x2 = x1 + 330
        y1, y2 = 130, 390
        title, desc, fill, outline = steps[i]
        draw_card(d, (x1, y1, x2, y2), fill=fill, outline=outline, radius=10)
        d.text((x1+20, y1+20), title, fill=(30, 41, 59), font=get_font(16, bold=True))
        d.text((x1+20, y1+60), desc, fill=(71, 85, 105), font=get_font(13))
        if i < 2:
            d.line([(x2, 260), (x2+50, 260)], fill=outline, width=3)
            d.polygon([(x2+50, 260), (x2+42, 255), (x2+42, 265)], fill=outline)

    # Conector Fila 1 a Fila 2
    d.line([(985, 390), (985, 440)], fill=(34, 197, 94), width=3)

    # Fila 2 (Pasos 4 a 6 en orden inverso para flujo serpenteante)
    for i in range(3):
        idx = 3 + i
        # 3 está a la derecha (x=820), 4 al centro (x=440), 5 a la izquierda (x=60)
        col_pos = 2 - i
        x1 = 60 + col_pos * 380
        x2 = x1 + 330
        y1, y2 = 450, 710
        title, desc, fill, outline = steps[idx]
        draw_card(d, (x1, y1, x2, y2), fill=fill, outline=outline, radius=10)
        d.text((x1+20, y1+20), title, fill=(30, 41, 59), font=get_font(16, bold=True))
        d.text((x1+20, y1+60), desc, fill=(71, 85, 105), font=get_font(13))
        if i < 2:
            # Flecha hacia la izquierda
            d.line([(x1, 580), (x1-50, 580)], fill=outline, width=3)
            d.polygon([(x1-50, 580), (x1-42, 575), (x1-42, 585)], fill=outline)

    img.save(os.path.join(DIAGRAMS_DIR, "application-integration-flow.png"), "PNG")
    print("4. application-integration-flow.png OK")

# =========================================================================
# 5. auth-me-sequence.png
# =========================================================================
def gen_auth_me_sequence():
    w, h = 1200, 700
    img = Image.new("RGB", (w, h), (255, 255, 255))
    d = ImageDraw.Draw(img)
    draw_header(d, "Contrato y Flujo de Ejecución del Endpoint `/auth/me`", "Consulta en tiempo real del contexto de acceso, roles, permisos y menús", w)

    actors = [
        ("Frontend Client (Angular SPA)", 250),
        ("IAM Controller (`/auth/me`)", 600),
        ("AccessContextService / DB", 950)
    ]
    for name, x in actors:
        draw_card(d, (x-130, 100, x+130, 140), fill=(241, 245, 249), outline=(203, 213, 225))
        d.text((x-110, 112), name, fill=(30, 41, 59), font=get_font(13, bold=True))
        d.line([(x, 140), (x, 650)], fill=(226, 232, 240), width=2)

    seq = [
        (250, 600, 180, "1. GET /api/v1/auth/me (Header: Authorization: Bearer <JWT>)", (30, 41, 59)),
        (600, 600, 230, "2. JwtAuthenticationFilter valida token y extrae username", (249, 115, 22)),
        (600, 950, 280, "3. Invoca AccessContextService.getAccessContext(userId)", (37, 99, 235)),
        (950, 950, 340, "4. Consolida: Roles -> Permisos -> Menús Autorizados -> Unidades de Negocio", (100, 116, 139)),
        (950, 950, 390, "5. MenuUrlResolverService resuelve variables no sensibles (${BUSINESS_UNIT})", (16, 185, 129)),
        (950, 600, 450, "6. Retorna DTO MeResponse con árbol completo y URLs resueltas", (37, 99, 235)),
        (600, 250, 520, "7. HTTP 200 OK: Payload JSON consolidado (Usuario, Roles, Menús, BUs)", (16, 185, 129)),
        (250, 250, 580, "8. Angular AuthService actualiza Signal reactivo y renderiza layout", (99, 102, 241))
    ]

    for x1, x2, y, text, color in seq:
        if x1 == x2:
            d.rectangle([(x1-15, y-10), (x1+300, y+22)], fill=(248, 250, 252), outline=(203, 213, 225))
            d.text((x1-5, y-3), text, fill=color, font=get_font(12, bold=True))
        else:
            d.line([(x1, y), (x2, y)], fill=color, width=2)
            arrow_dir = 1 if x2 > x1 else -1
            d.polygon([(x2, y), (x2 - 8*arrow_dir, y-5), (x2 - 8*arrow_dir, y+5)], fill=color)
            mid_x = min(x1, x2) + 20
            d.text((mid_x, y-18), text, fill=color, font=get_font(12, bold=True))

    img.save(os.path.join(DIAGRAMS_DIR, "auth-me-sequence.png"), "PNG")
    print("5. auth-me-sequence.png OK")

# =========================================================================
# 6. roadmap-phase1-phase2.png
# =========================================================================
def gen_roadmap():
    w, h = 1200, 720
    img = Image.new("RGB", (w, h), (248, 249, 250))
    d = ImageDraw.Draw(img)
    draw_header(d, "Roadmap Arquitectónico: Evolución SSO IAM Rassini", "De Menús Dinámicos y JWT Corporativo a Federación Abierta OIDC con PKCE", w)

    # Fase 1: Implementado
    d.rounded_rectangle([(60, 130), (570, 680)], radius=12, fill=(240, 253, 244), outline=(34, 197, 94), width=2)
    draw_card(d, (80, 150, 550, 200), fill=(34, 197, 94), outline=(22, 101, 52))
    d.text((100, 165), "FASE 1: CONSOLIDADA (Actual)", fill=(255, 255, 255), font=get_font(18, bold=True))

    f1_items = [
        ("Catálogo de Menús Dinámicos", "Soporte para menús INTERNOS y EXTERNOS con tipo_autenticacion y app_type."),
        ("Prohibición Criptográfica de Tokens en URL", "Bloqueo HTTP 400 estricto para ${JWT}, ${TOKEN}, ${PASSWORD} en query string."),
        ("Resolución de Contexto No Sensible", "Parámetros permitidos resueltos en backend: ${BUSINESS_UNIT}, ${LANGUAGE}, ${THEME}."),
        ("Administración 100% por Pantallas", "CRUD completo de Aplicaciones, Menús, Permisos y Roles sin necesidad de SQL."),
        ("Preparación OIDC en Modelo de BD", "Tabla applications con client_id, client_secret, redirect_uri e is_internal.")
    ]
    for idx, (title, desc) in enumerate(f1_items):
        by = 220 + idx * 90
        draw_card(d, (80, by, 550, by+75), fill=(255, 255, 255), outline=(187, 247, 208))
        d.text((95, by+10), "✔ " + title, fill=(20, 83, 45), font=get_font(14, bold=True))
        d.text((95, by+35), desc, fill=(71, 85, 105), font=get_font(12))

    # Fase 2: Futura
    d.rounded_rectangle([(630, 130), (1140, 680)], radius=12, fill=(238, 242, 255), outline=(99, 102, 241), width=2)
    draw_card(d, (650, 150, 1120, 200), fill=(99, 102, 241), outline=(67, 56, 202))
    d.text((670, 165), "FASE 2: EVOLUCIÓN SSO OIDC (Roadmap)", fill=(255, 255, 255), font=get_font(18, bold=True))

    f2_items = [
        ("Spring Authorization Server (OIDC Provider)", "Endpoints estándar /oauth2/authorize, /oauth2/token, /.well-known/openid-configuration."),
        ("PKCE Obligatorio (RFC 7636)", "Proof Key for Code Exchange para clientes públicos en Angular y apps móviles."),
        ("Rotación Asimétrica de Claves (JWKS)", "Firma con RSA 2048 / EC P-256; Satélites validan firma descargando llave pública."),
        ("Single Sign-Out Centralizado (RFC 7009)", "Front-channel / Back-channel logout: cerrar sesión en Portal revoca apps satélite."),
        ("Federación Empresarial SAML / Azure AD", "Conexión opcional del IAM con Microsoft Entra ID corporativo.")
    ]
    for idx, (title, desc) in enumerate(f2_items):
        by = 220 + idx * 90
        draw_card(d, (650, by, 1120, by+75), fill=(255, 255, 255), outline=(199, 210, 254))
        d.text((665, by+10), "➔ " + title, fill=(30, 27, 75), font=get_font(14, bold=True))
        d.text((665, by+35), desc, fill=(71, 85, 105), font=get_font(12))

    img.save(os.path.join(DIAGRAMS_DIR, "roadmap-phase1-phase2.png"), "PNG")
    print("6. roadmap-phase1-phase2.png OK")

gen_ecosystem()
gen_sso_sequence()
gen_jwt_validation()
gen_integration_flow()
gen_auth_me_sequence()
gen_roadmap()
print("All diagrams generated successfully!")
