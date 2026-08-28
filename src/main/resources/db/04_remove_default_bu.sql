-- ==========================================================
-- DML - ELIMINAR DEFAULT BUSINESS UNIT
-- ==========================================================
-- Este script elimina la llave foránea y la columna
-- default_business_unit_id de la tabla users.

ALTER TABLE users DROP FOREIGN KEY fk_user_default_bu;
ALTER TABLE users DROP INDEX idx_users_default_bu;
ALTER TABLE users DROP COLUMN default_business_unit_id;
