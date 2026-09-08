-- ==========================================================
-- DDL - ESQUEMA COMPLETO IAM (MySQL 8.x)
-- ==========================================================
-- Esquema: iam
-- Motor:   MySQL 8.x (InnoDB / utf8mb4)
--
-- Este script crea TODAS las tablas que mapean las entidades JPA
-- del proyecto (package com.rassini.employeeportal.entity):
--   users, roles, permissions, menus, applications, business_units
-- y sus tablas puente:
--   user_roles, role_permissions, permission_menu, user_business_unit
--
-- NOTA: es idempotente por tabla (DROP IF EXISTS + CREATE).
-- Ejecutar contra la base 'iam' antes de levantar la aplicación
-- (ddl-auto=none).
-- ==========================================================

-- ------------------------------------------------------------------
-- 1. APPLICATIONS
--    (se crea primero: permissions y menus dependen de ella)
-- ------------------------------------------------------------------
DROP TABLE IF EXISTS applications;

CREATE TABLE applications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(50) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    active      TINYINT(1) NOT NULL DEFAULT 1,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------
-- 2. USERS
-- ------------------------------------------------------------------
DROP TABLE IF EXISTS user_business_unit;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    username               VARCHAR(100) NOT NULL UNIQUE,
    email                  VARCHAR(150) NOT NULL UNIQUE,
    password_hash          VARCHAR(255) NOT NULL,
    enabled                TINYINT(1) DEFAULT 1,
    force_password_change  TINYINT(1) DEFAULT 0,
    has_all_business_units TINYINT(1) NOT NULL DEFAULT 0,
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------
-- 3. ROLES
-- ------------------------------------------------------------------
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS roles;

CREATE TABLE roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(50) NOT NULL UNIQUE,
    name        VARCHAR(100),
    description VARCHAR(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------
-- 4. PERMISSIONS
-- ------------------------------------------------------------------
DROP TABLE IF EXISTS permission_menu;
DROP TABLE IF EXISTS permissions;

CREATE TABLE permissions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    code           VARCHAR(100) NOT NULL UNIQUE,
    description    VARCHAR(255),
    application_id BIGINT NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_permissions_applications
        FOREIGN KEY (application_id) REFERENCES applications(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------
-- 5. MENUS
-- ------------------------------------------------------------------
DROP TABLE IF EXISTS menus;

CREATE TABLE menus (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    code           VARCHAR(100) NOT NULL UNIQUE,
    label          VARCHAR(100),
    route          VARCHAR(200),
    icon           VARCHAR(50),
    order_index    INT,
    parent_id      BIGINT NULL,
    application_id BIGINT NOT NULL,
    CONSTRAINT fk_menus_parent
        FOREIGN KEY (parent_id) REFERENCES menus(id),
    CONSTRAINT fk_menus_applications
        FOREIGN KEY (application_id) REFERENCES applications(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------
-- 6. BUSINESS UNITS
-- ------------------------------------------------------------------
DROP TABLE IF EXISTS business_units;

CREATE TABLE business_units (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(50) NOT NULL UNIQUE,
    name       VARCHAR(150) NOT NULL,
    parent_id  BIGINT NULL,
    enabled    TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bu_parent FOREIGN KEY (parent_id) REFERENCES business_units(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------
-- 7. TABLAS PUENTE (ManyToMany)
-- ------------------------------------------------------------------

-- users <-> roles
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- roles <-> permissions
CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role       FOREIGN KEY (role_id)       REFERENCES roles(id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- permissions <-> menus
CREATE TABLE permission_menu (
    permission_id BIGINT NOT NULL,
    menu_id       BIGINT NOT NULL,
    PRIMARY KEY (permission_id, menu_id),
    CONSTRAINT fk_permission_menu_permission FOREIGN KEY (permission_id) REFERENCES permissions(id),
    CONSTRAINT fk_permission_menu_menu       FOREIGN KEY (menu_id)       REFERENCES menus(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- users <-> business_units
CREATE TABLE user_business_unit (
    user_id          BIGINT NOT NULL,
    business_unit_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, business_unit_id),
    CONSTRAINT fk_ubu_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ubu_bu   FOREIGN KEY (business_unit_id) REFERENCES business_units(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
