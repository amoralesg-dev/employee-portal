package com.rassini.employeeportal.repository;

import com.rassini.employeeportal.entity.PortalMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortalMediaRepository extends JpaRepository<PortalMediaEntity, Long> {
    Optional<PortalMediaEntity> findByStorageKey(String storageKey);
}
