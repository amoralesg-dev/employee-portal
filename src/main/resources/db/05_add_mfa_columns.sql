-- ==========================================================
-- DML - AGREGAR COLUMNAS PARA MFA
-- ==========================================================
-- Este script agrega las columnas necesarias para MFA en la tabla users
-- MVP: mfa_enabled, mfa_secret, mfa_enabled_at

ALTER TABLE users 
ADD COLUMN mfa_enabled TINYINT(1) NOT NULL DEFAULT 0,
ADD COLUMN mfa_secret VARCHAR(255) DEFAULT NULL,
ADD COLUMN mfa_enabled_at TIMESTAMP DEFAULT NULL;
