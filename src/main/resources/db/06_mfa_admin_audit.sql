CREATE TABLE IF NOT EXISTS mfa_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mfa_audit_admin FOREIGN KEY (admin_id) REFERENCES users(id),
    CONSTRAINT fk_mfa_audit_target FOREIGN KEY (target_user_id) REFERENCES users(id)
);
