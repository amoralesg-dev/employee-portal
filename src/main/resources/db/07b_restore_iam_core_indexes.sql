-- =====================================================================
-- MIGRACIÓN 07b: RESTAURACIÓN DE PK / AUTO_INCREMENT / ÍNDICES CORE IAM
-- Esquema: iam
-- Motor: MySQL 8.x
-- =====================================================================
-- MOTIVO:
-- IAM DEV perdió las PRIMARY KEY, AUTO_INCREMENT, índices UNIQUE/KEY y
-- Foreign Keys en las tablas base del esquema IAM respecto de IAM LOCAL:
--   1. users
--   2. business_units
--   3. roles
--   4. applications
--   5. permissions
--   6. menus
--   7. role_permissions
--   8. permission_menu
--   9. user_roles
--
-- Esto provoca:
-- - MySQL Error 1822 al crear Foreign Keys desde portal_* en 08_portal_content.sql
-- - MySQL Error 1364 (Field 'id' doesn't have a default value) en INSERT sin id
-- - Pérdida de idempotencia en INSERT IGNORE sobre role_permissions / permission_menu
--
-- ALCANCE Y SEGURIDAD:
-- - 100% idempotente (verifica INFORMATION_SCHEMA antes de cada ALTER).
-- - Seguro de re-ejecutar aunque users, business_units y roles ya estén reparadas.
-- - NO borra datos, NO recrea tablas, NO hace TRUNCATE, NO modifica IDs.
-- - NO toca Póliza, Workflow ni CxP.
-- =====================================================================

SET NAMES utf8mb4;
USE iam;

-- ---------------------------------------------------------------------
-- 1. TABLA: users
-- ---------------------------------------------------------------------
SET @has_pk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'PRIMARY');
SET @sql := IF(@has_pk = 0,
    'ALTER TABLE users ADD PRIMARY KEY (id), MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT',
    'ALTER TABLE users MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'username');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE users ADD UNIQUE KEY username (username)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'email');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE users ADD UNIQUE KEY email (email)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. TABLA: business_units
-- ---------------------------------------------------------------------
SET @has_pk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'business_units' AND INDEX_NAME = 'PRIMARY');
SET @sql := IF(@has_pk = 0,
    'ALTER TABLE business_units ADD PRIMARY KEY (id), MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT',
    'ALTER TABLE business_units MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'business_units' AND INDEX_NAME = 'code');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE business_units ADD UNIQUE KEY code (code)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'business_units' AND INDEX_NAME = 'fk_bu_parent');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE business_units ADD KEY fk_bu_parent (parent_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'business_units' AND CONSTRAINT_NAME = 'fk_bu_parent' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@has_fk = 0, 'ALTER TABLE business_units ADD CONSTRAINT fk_bu_parent FOREIGN KEY (parent_id) REFERENCES business_units (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 3. TABLA: roles
-- ---------------------------------------------------------------------
SET @has_pk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'roles' AND INDEX_NAME = 'PRIMARY');
SET @sql := IF(@has_pk = 0,
    'ALTER TABLE roles ADD PRIMARY KEY (id), MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT',
    'ALTER TABLE roles MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'roles' AND INDEX_NAME = 'code');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE roles ADD UNIQUE KEY code (code)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 4. TABLA: applications
-- ---------------------------------------------------------------------
SET @has_pk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'applications' AND INDEX_NAME = 'PRIMARY');
SET @sql := IF(@has_pk = 0,
    'ALTER TABLE applications ADD PRIMARY KEY (id), MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT',
    'ALTER TABLE applications MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'applications' AND INDEX_NAME = 'code');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE applications ADD UNIQUE KEY code (code)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'applications' AND INDEX_NAME = 'client_id');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE applications ADD UNIQUE KEY client_id (client_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 5. TABLA: permissions
-- ---------------------------------------------------------------------
SET @has_pk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permissions' AND INDEX_NAME = 'PRIMARY');
SET @sql := IF(@has_pk = 0,
    'ALTER TABLE permissions ADD PRIMARY KEY (id), MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT',
    'ALTER TABLE permissions MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permissions' AND INDEX_NAME = 'code');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE permissions ADD UNIQUE KEY code (code)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permissions' AND INDEX_NAME = 'fk_permissions_applications');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE permissions ADD KEY fk_permissions_applications (application_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permissions' AND CONSTRAINT_NAME = 'fk_permissions_applications' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@has_fk = 0, 'ALTER TABLE permissions ADD CONSTRAINT fk_permissions_applications FOREIGN KEY (application_id) REFERENCES applications (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 6. TABLA: menus
-- ---------------------------------------------------------------------
SET @has_pk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'menus' AND INDEX_NAME = 'PRIMARY');
SET @sql := IF(@has_pk = 0,
    'ALTER TABLE menus ADD PRIMARY KEY (id), MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT',
    'ALTER TABLE menus MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'menus' AND INDEX_NAME = 'code');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE menus ADD UNIQUE KEY code (code)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'menus' AND INDEX_NAME = 'fk_menu_parent');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE menus ADD KEY fk_menu_parent (parent_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'menus' AND INDEX_NAME = 'fk_menus_applications');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE menus ADD KEY fk_menus_applications (application_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'menus' AND CONSTRAINT_NAME = 'fk_menu_parent' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@has_fk = 0, 'ALTER TABLE menus ADD CONSTRAINT fk_menu_parent FOREIGN KEY (parent_id) REFERENCES menus (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'menus' AND CONSTRAINT_NAME = 'fk_menus_applications' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@has_fk = 0, 'ALTER TABLE menus ADD CONSTRAINT fk_menus_applications FOREIGN KEY (application_id) REFERENCES applications (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 7. TABLA: role_permissions (N:M)
-- ---------------------------------------------------------------------
SET @has_pk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role_permissions' AND INDEX_NAME = 'PRIMARY');
SET @sql := IF(@has_pk = 0, 'ALTER TABLE role_permissions ADD PRIMARY KEY (role_id, permission_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role_permissions' AND INDEX_NAME = 'fk_role_permissions_permission');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE role_permissions ADD KEY fk_role_permissions_permission (permission_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role_permissions' AND CONSTRAINT_NAME = 'fk_role_permissions_role' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@has_fk = 0, 'ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role_permissions' AND CONSTRAINT_NAME = 'fk_role_permissions_permission' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@has_fk = 0, 'ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 8. TABLA: permission_menu (N:M)
-- ---------------------------------------------------------------------
SET @has_pk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permission_menu' AND INDEX_NAME = 'PRIMARY');
SET @sql := IF(@has_pk = 0, 'ALTER TABLE permission_menu ADD PRIMARY KEY (permission_id, menu_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permission_menu' AND INDEX_NAME = 'fk_permission_menu_menu');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE permission_menu ADD KEY fk_permission_menu_menu (menu_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permission_menu' AND CONSTRAINT_NAME = 'fk_permission_menu_permission' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@has_fk = 0, 'ALTER TABLE permission_menu ADD CONSTRAINT fk_permission_menu_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permission_menu' AND CONSTRAINT_NAME = 'fk_permission_menu_menu' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@has_fk = 0, 'ALTER TABLE permission_menu ADD CONSTRAINT fk_permission_menu_menu FOREIGN KEY (menu_id) REFERENCES menus (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 9. TABLA: user_roles (N:M)
-- ---------------------------------------------------------------------
SET @has_pk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_roles' AND INDEX_NAME = 'PRIMARY');
SET @sql := IF(@has_pk = 0, 'ALTER TABLE user_roles ADD PRIMARY KEY (user_id, role_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_roles' AND INDEX_NAME = 'fk_user_roles_role');
SET @sql := IF(@has_idx = 0, 'ALTER TABLE user_roles ADD KEY fk_user_roles_role (role_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_roles' AND CONSTRAINT_NAME = 'fk_user_roles_user' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@has_fk = 0, 'ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fk := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_roles' AND CONSTRAINT_NAME = 'fk_user_roles_role' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @sql := IF(@has_fk = 0, 'ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
