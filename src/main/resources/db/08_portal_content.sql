-- =====================================================================
-- MIGRACIÓN 08: CONTENIDO DINÁMICO DEL HOMEPAGE (COMUNICACIÓN INTERNA)
-- Esquema: iam
-- Motor: MySQL 8.x
-- =====================================================================

SET NAMES utf8mb4;
USE iam;

-- 1. Metadata de medios (imágenes almacenadas en filesystem externo)
CREATE TABLE IF NOT EXISTS portal_media (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    storage_key VARCHAR(100) NOT NULL UNIQUE COMMENT 'Nombre físico UUID.ext relativo a PORTAL_MEDIA_PATH',
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL COMMENT 'image/jpeg | image/png | image/webp',
    size_bytes BIGINT NOT NULL,
    uploaded_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_portal_media_user FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Contenido común del portal (no traducible)
CREATE TABLE IF NOT EXISTS portal_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_type VARCHAR(30) NOT NULL COMMENT 'HERO_NEWS | NEWS | CAMPAIGN | WEEKLY_INFO | EVENT | QUICK_ACCESS | NOTICE',
    category VARCHAR(20) NULL COMMENT 'SEGURIDAD | PERSONAS | NEGOCIO | CULTURA',
    status VARCHAR(15) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT | PUBLISHED | INACTIVE',
    priority INT NOT NULL DEFAULT 100,
    publish_from DATETIME NULL,
    publish_to DATETIME NULL,
    image_id BIGINT NULL,
    link_url VARCHAR(2000) NULL,
    link_new_tab TINYINT(1) NOT NULL DEFAULT 0,
    icon VARCHAR(50) NULL,
    event_start DATETIME NULL,
    event_end DATETIME NULL,
    event_location VARCHAR(200) NULL,
    teams_url VARCHAR(2000) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    KEY ix_portal_content_window (status, publish_from, publish_to),
    KEY ix_portal_content_type_prio (content_type, priority),
    CONSTRAINT fk_portal_content_image FOREIGN KEY (image_id) REFERENCES portal_media(id) ON DELETE SET NULL,
    CONSTRAINT fk_portal_content_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_portal_content_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Asegurar VARCHAR(2000) en instalaciones existentes (idempotente)
ALTER TABLE portal_content MODIFY COLUMN link_url VARCHAR(2000) NULL;

-- Asegurar columna teams_url en instalaciones existentes (idempotente)
SET @has_teams_url := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'portal_content'
      AND COLUMN_NAME = 'teams_url'
);
SET @sql_add_teams_url := IF(
    @has_teams_url = 0,
    'ALTER TABLE portal_content ADD COLUMN teams_url VARCHAR(2000) NULL AFTER event_location',
    'SELECT 1'
);
PREPARE stmt_add_teams_url FROM @sql_add_teams_url;
EXECUTE stmt_add_teams_url;
DEALLOCATE PREPARE stmt_add_teams_url;

-- 2.1 Confirmaciones de asistencia a eventos (RSVP)
CREATE TABLE IF NOT EXISTS portal_event_attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portal_content_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL COMMENT 'GOING | NOT_GOING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_portal_event_attendance (portal_content_id, user_id),
    KEY ix_pea_content_status (portal_content_id, status),
    CONSTRAINT fk_pea_content FOREIGN KEY (portal_content_id) REFERENCES portal_content(id) ON DELETE CASCADE,
    CONSTRAINT fk_pea_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Traducciones por idioma (es-MX obligatorio en app, en-US opcional)
CREATE TABLE IF NOT EXISTS portal_content_translation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portal_content_id BIGINT NOT NULL,
    language VARCHAR(5) NOT NULL COMMENT 'es-MX | en-US',
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500) NULL,
    body TEXT NULL,
    link_label VARCHAR(100) NULL,
    UNIQUE KEY uk_portal_content_translation (portal_content_id, language),
    CONSTRAINT fk_pct_content FOREIGN KEY (portal_content_id) REFERENCES portal_content(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Audiencia por Business Units (N:M)
CREATE TABLE IF NOT EXISTS portal_content_business_units (
    portal_content_id BIGINT NOT NULL,
    business_unit_id BIGINT NOT NULL,
    PRIMARY KEY (portal_content_id, business_unit_id),
    CONSTRAINT fk_pcbu_content FOREIGN KEY (portal_content_id) REFERENCES portal_content(id) ON DELETE CASCADE,
    CONSTRAINT fk_pcbu_bu FOREIGN KEY (business_unit_id) REFERENCES business_units(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Audiencia por Roles (N:M)
CREATE TABLE IF NOT EXISTS portal_content_roles (
    portal_content_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (portal_content_id, role_id),
    CONSTRAINT fk_pcr_content FOREIGN KEY (portal_content_id) REFERENCES portal_content(id) ON DELETE CASCADE,
    CONSTRAINT fk_pcr_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Aplicación lógica COMUNICACION, permisos, rol COMUNICACION_INTERNA y menús jerárquicos
INSERT INTO applications (code, name, description, active, is_internal, client_id, client_secret, redirect_uri, created_at, updated_at)
SELECT 'COMUNICACION', 'Comunicación', 'Administración de contenido y comunicación interna', 1, 1, NULL, NULL, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM applications WHERE code = 'COMUNICACION');

UPDATE applications
SET name = 'Comunicación',
    description = 'Administración de contenido y comunicación interna',
    updated_at = NOW()
WHERE code = 'COMUNICACION';

SET @app_id := (SELECT id FROM applications WHERE code = 'COMUNICACION' LIMIT 1);

-- 6.1 Permisos (crear si no existen; si ya existían bajo otra aplicación, reasociar a COMUNICACION)
INSERT INTO permissions (code, description, application_id, created_at)
SELECT t.code, t.description, @app_id, NOW()
FROM (
    SELECT 'PORTAL_CONTENT_READ'    AS code, 'Ver contenido del portal en administración'  AS description UNION ALL
    SELECT 'PORTAL_CONTENT_CREATE'  AS code, 'Crear contenido del portal en borrador'      AS description UNION ALL
    SELECT 'PORTAL_CONTENT_UPDATE'  AS code, 'Editar contenido del portal'                 AS description UNION ALL
    SELECT 'PORTAL_CONTENT_PUBLISH' AS code, 'Publicar y despublicar contenido del portal' AS description UNION ALL
    SELECT 'PORTAL_CONTENT_DELETE'       AS code, 'Eliminar contenido del portal'                    AS description UNION ALL
    SELECT 'PORTAL_MEDIA_UPLOAD'         AS code, 'Subir imágenes para el portal'                    AS description UNION ALL
    SELECT 'PORTAL_EVENT_ATTENDANCE_READ' AS code, 'Consultar confirmaciones de asistencia a eventos' AS description
) t
WHERE @app_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = t.code);

UPDATE permissions
SET application_id = @app_id
WHERE @app_id IS NOT NULL
  AND code IN (
      'PORTAL_CONTENT_READ',
      'PORTAL_CONTENT_CREATE',
      'PORTAL_CONTENT_UPDATE',
      'PORTAL_CONTENT_PUBLISH',
      'PORTAL_CONTENT_DELETE',
      'PORTAL_MEDIA_UPLOAD',
      'PORTAL_EVENT_ATTENDANCE_READ'
  )
  AND application_id <> @app_id;

-- 6.2 Rol COMUNICACION_INTERNA con los 6 permisos explícitos
INSERT INTO roles (code, name, description, created_at)
SELECT 'COMUNICACION_INTERNA', 'Comunicación Interna', 'Administra el contenido dinámico del homepage del portal', NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'COMUNICACION_INTERNA');

UPDATE roles
SET name = 'Comunicación Interna',
    description = 'Administra el contenido dinámico del homepage del portal'
WHERE code = 'COMUNICACION_INTERNA';

UPDATE permissions SET description = 'Ver contenido del portal en administración' WHERE code = 'PORTAL_CONTENT_READ';
UPDATE permissions SET description = 'Subir imágenes para el portal' WHERE code = 'PORTAL_MEDIA_UPLOAD';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'PORTAL_CONTENT_READ',
    'PORTAL_CONTENT_CREATE',
    'PORTAL_CONTENT_UPDATE',
    'PORTAL_CONTENT_PUBLISH',
    'PORTAL_CONTENT_DELETE',
    'PORTAL_MEDIA_UPLOAD',
    'PORTAL_EVENT_ATTENDANCE_READ'
)
WHERE r.code = 'COMUNICACION_INTERNA';

-- 6.3 Menú padre: COMUNICACION
INSERT INTO menus (code, label, route, icon, order_index, parent_id, target_type, open_in_new_tab, app_type, auth_type, application_id)
SELECT 'COMUNICACION', 'Comunicación', NULL, 'pi pi-megaphone', 90, NULL,
       'INTERNO', 0, 'INTERNA', 'NONE', @app_id
WHERE @app_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM menus WHERE code = 'COMUNICACION');

UPDATE menus
SET label = 'Comunicación',
    route = NULL,
    parent_id = NULL,
    application_id = @app_id
WHERE @app_id IS NOT NULL
  AND code = 'COMUNICACION';

SET @parent_menu_id := (SELECT id FROM menus WHERE code = 'COMUNICACION' LIMIT 1);

-- 6.4 Menú hijo: PORTAL_CONTENT (crear si no existe; si ya existía, reasociar a COMUNICACION y parent_id)
INSERT INTO menus (code, label, route, icon, order_index, parent_id, target_type, open_in_new_tab, app_type, auth_type, application_id)
SELECT 'PORTAL_CONTENT', 'Contenido del Portal', '/contenido-portal', 'pi pi-file-edit', 1, @parent_menu_id,
       'INTERNO', 0, 'INTERNA', 'NONE', @app_id
WHERE @app_id IS NOT NULL
  AND @parent_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM menus WHERE code = 'PORTAL_CONTENT');

UPDATE menus
SET label = 'Contenido del Portal',
    route = '/contenido-portal',
    parent_id = @parent_menu_id,
    application_id = @app_id
WHERE @app_id IS NOT NULL
  AND @parent_menu_id IS NOT NULL
  AND code = 'PORTAL_CONTENT';

-- 6.5 Visibilidad: solo el menú hijo se liga al permiso; AccessContextServiceImpl incluye automáticamente el padre
INSERT IGNORE INTO permission_menu (permission_id, menu_id)
SELECT p.id, m.id
FROM permissions p
JOIN menus m ON m.code = 'PORTAL_CONTENT'
WHERE p.code = 'PORTAL_CONTENT_READ';

-- =====================================================================
-- ROLLBACK SCRIPT (SOLO DOCUMENTACIÓN - NO EJECUTAR AUTOMÁTICAMENTE)
-- =====================================================================
-- DELETE pm FROM permission_menu pm JOIN menus m ON pm.menu_id = m.id WHERE m.code IN ('PORTAL_CONTENT', 'COMUNICACION');
-- DELETE rp FROM role_permissions rp JOIN roles r ON rp.role_id = r.id WHERE r.code = 'COMUNICACION_INTERNA';
-- DELETE FROM menus WHERE code = 'PORTAL_CONTENT';
-- DELETE FROM menus WHERE code = 'COMUNICACION';
-- DELETE FROM permissions WHERE code IN ('PORTAL_CONTENT_READ','PORTAL_CONTENT_CREATE','PORTAL_CONTENT_UPDATE','PORTAL_CONTENT_PUBLISH','PORTAL_CONTENT_DELETE','PORTAL_MEDIA_UPLOAD');
-- DELETE FROM roles WHERE code = 'COMUNICACION_INTERNA';
-- DELETE FROM applications WHERE code = 'COMUNICACION';
-- DROP TABLE IF EXISTS portal_content_roles;
-- DROP TABLE IF EXISTS portal_content_business_units;
-- DROP TABLE IF EXISTS portal_content_translation;
-- DROP TABLE IF EXISTS portal_content;
-- DROP TABLE IF EXISTS portal_media;
