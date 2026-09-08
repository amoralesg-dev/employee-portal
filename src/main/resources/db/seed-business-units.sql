-- ==========================================================
-- DML - SEMILLA CARGA INICIAL DE BUSINESS UNITS (DEFINITIVO)
-- ==========================================================
-- IMPORTANTE:
-- business_units.code debe mantenerse como VARCHAR.
-- No convertir a INT.
-- Deben preservarse los ceros a la izquierda:
-- 09
-- 02
-- 0103
-- 0111
-- ==========================================================

START TRANSACTION;

-- 1. REGISTRO DE BUSINESS UNITS RAÍZ
INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '0111', 'Corporativo', NULL, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '0111');

INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '09', 'Piedras Negras', NULL, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '09');

INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '99', 'PN99', NULL, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '99');

INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '0301', 'Bypasa', NULL, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '0301');

INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '1000', 'Frenos', NULL, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '1000');

INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '1850', 'Brakes', NULL, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '1850');


-- 2. RECUPERAR IDS DE LAS RAÍCES PARA LA JERARQUÍA
SET @corp_id   = (SELECT id FROM business_units WHERE code = '0111' LIMIT 1);
SET @pn_id     = (SELECT id FROM business_units WHERE code = '09' LIMIT 1);
SET @frenos_id = (SELECT id FROM business_units WHERE code = '1000' LIMIT 1);


-- 3. REGISTRO DE HIJOS DE CORPORATIVO (0111)
INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '0103', '0103', @corp_id, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '0103');

INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '0109', '0109', @corp_id, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '0109');

INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '0110', '0110', @corp_id, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '0110');

INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '0112', '0112', @corp_id, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '0112');

INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '0114', '0114', @corp_id, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '0114');

INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '0115', '0115', @corp_id, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '0115');

INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '0117', '0117', @corp_id, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '0117');

INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '0120', '0120', @corp_id, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '0120');


-- 4. REGISTRO DE HIJOS DE PIEDRAS NEGRAS (09)
INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '02', '02', @pn_id, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '02');

INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '72', '72', @pn_id, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '72');

INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '10', '10', @pn_id, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '10');


-- 5. REGISTRO DE HIJOS DE FRENOS (1000)
INSERT INTO business_units (code, name, parent_id, enabled, created_at, updated_at)
SELECT '1001', '1001', @frenos_id, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM business_units WHERE code = '1001');

COMMIT;
