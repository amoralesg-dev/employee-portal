-- =====================================================================
-- MIGRACIÓN: EXTENSIÓN DE MENÚS Y APLICACIONES PARA ARQUITECTURA SSO IAM
-- Esquema: iam
-- Motor: MySQL 8.x
-- =====================================================================

USE iam;

-- 1. Extender tabla `applications` para preparación de clientes OAuth2 / OIDC
ALTER TABLE applications
    ADD COLUMN client_id VARCHAR(100) NULL UNIQUE COMMENT 'Identificador de cliente OAuth2 / OIDC',
    ADD COLUMN client_secret VARCHAR(255) NULL COMMENT 'Hash o secreto del cliente OAuth2',
    ADD COLUMN redirect_uri VARCHAR(500) NULL COMMENT 'URI de redirección post-autenticación OIDC',
    ADD COLUMN is_internal TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1: Aplicación interna Rassini, 0: Aplicación de terceros o SaaS';

-- 2. Extender tabla `menus` para navegación hacia aplicaciones externas
ALTER TABLE menus
    ADD COLUMN target_type VARCHAR(20) NOT NULL DEFAULT 'INTERNO' COMMENT 'INTERNO | EXTERNO',
    ADD COLUMN external_url VARCHAR(500) NULL COMMENT 'URL destino cuando target_type es EXTERNO',
    ADD COLUMN open_in_new_tab TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1: Abre en _blank, 0: Abre en _self',
    ADD COLUMN app_type VARCHAR(30) NOT NULL DEFAULT 'INTERNA' COMMENT 'INTERNA | TERCERO | SAAS',
    ADD COLUMN auth_type VARCHAR(30) NOT NULL DEFAULT 'NONE' COMMENT 'NONE | SSO_IAM | OIDC | CREDENCIALES_PROPIAS';

-- 3. Crear tabla hija `menu_parameters` para parámetros de contexto NO sensibles
CREATE TABLE IF NOT EXISTS menu_parameters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_id BIGINT NOT NULL,
    param_name VARCHAR(100) NOT NULL COMMENT 'Nombre del parámetro de URL (ej. bu, lang, theme, username)',
    param_value VARCHAR(255) NOT NULL COMMENT 'Valor o variable de contexto permitida ej: ${BUSINESS_UNIT}',
    active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1: Activo, 0: Inactivo',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_menu_parameters_menu FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE,
    CONSTRAINT uk_menu_param UNIQUE (menu_id, param_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- ROLLBACK SCRIPT
-- =====================================================================
-- DROP TABLE IF EXISTS iam.menu_parameters;
-- ALTER TABLE iam.menus 
--     DROP COLUMN target_type,
--     DROP COLUMN external_url,
--     DROP COLUMN open_in_new_tab,
--     DROP COLUMN app_type,
--     DROP COLUMN auth_type;
-- ALTER TABLE iam.applications 
--     DROP COLUMN client_id,
--     DROP COLUMN client_secret,
--     DROP COLUMN redirect_uri,
--     DROP COLUMN is_internal;
