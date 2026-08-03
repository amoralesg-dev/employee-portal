CREATE TABLE applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

ALTER TABLE permissions ADD COLUMN application_id BIGINT;
ALTER TABLE menus ADD COLUMN application_id BIGINT;

-- Backfill existing records to a default application
INSERT INTO applications (code, name, description) VALUES ('EMPLOYEE_PORTAL', 'Employee Portal', 'Portal de Empleados');
SET @app_id = (SELECT id FROM applications WHERE code = 'EMPLOYEE_PORTAL');

UPDATE permissions SET application_id = @app_id WHERE application_id IS NULL;
UPDATE menus SET application_id = @app_id WHERE application_id IS NULL;

-- Enforce NOT NULL constraint
ALTER TABLE permissions MODIFY application_id BIGINT NOT NULL;
ALTER TABLE menus MODIFY application_id BIGINT NOT NULL;

-- Add foreign key constraints
ALTER TABLE permissions ADD CONSTRAINT fk_permissions_applications FOREIGN KEY (application_id) REFERENCES applications(id);
ALTER TABLE menus ADD CONSTRAINT fk_menus_applications FOREIGN KEY (application_id) REFERENCES applications(id);
