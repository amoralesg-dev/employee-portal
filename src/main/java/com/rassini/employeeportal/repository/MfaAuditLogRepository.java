package com.rassini.employeeportal.repository;

import com.rassini.employeeportal.entity.MfaAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MfaAuditLogRepository extends JpaRepository<MfaAuditLogEntity, Long> {
}
