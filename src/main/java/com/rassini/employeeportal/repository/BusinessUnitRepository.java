package com.rassini.employeeportal.repository;

import com.rassini.employeeportal.entity.BusinessUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessUnitRepository extends JpaRepository<BusinessUnitEntity, Long> {
    Optional<BusinessUnitEntity> findByCode(String code);
    List<BusinessUnitEntity> findByParentIsNull();
    List<BusinessUnitEntity> findByEnabledTrue();
}
