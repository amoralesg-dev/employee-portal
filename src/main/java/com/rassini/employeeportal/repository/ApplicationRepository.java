package com.rassini.employeeportal.repository;

import com.rassini.employeeportal.entity.ApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {
    Optional<ApplicationEntity> findByCode(String code);
    List<ApplicationEntity> findByActiveTrue();
}
