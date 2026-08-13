CREATE TABLE business_units (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    parent_id BIGINT NULL,
    enabled TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bu_parent FOREIGN KEY (parent_id) REFERENCES business_units(id)
);

CREATE TABLE user_business_unit (
    user_id BIGINT NOT NULL,
    business_unit_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, business_unit_id),
    CONSTRAINT fk_ubu_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ubu_bu FOREIGN KEY (business_unit_id) REFERENCES business_units(id)
);
