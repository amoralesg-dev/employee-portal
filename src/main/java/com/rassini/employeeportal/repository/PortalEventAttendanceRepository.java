package com.rassini.employeeportal.repository;

import com.rassini.employeeportal.entity.PortalEventAttendanceEntity;
import com.rassini.employeeportal.entity.PortalEventAttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortalEventAttendanceRepository extends JpaRepository<PortalEventAttendanceEntity, Long> {

    Optional<PortalEventAttendanceEntity> findByPortalContentIdAndUserId(Long portalContentId, Long userId);

    void deleteByPortalContentIdAndUserId(Long portalContentId, Long userId);

    long countByPortalContentIdAndStatus(Long portalContentId, PortalEventAttendanceStatus status);

    @Query(
            value = "SELECT a FROM PortalEventAttendanceEntity a JOIN FETCH a.user WHERE a.portalContent.id = :contentId ORDER BY COALESCE(a.updatedAt, a.createdAt) DESC, a.id DESC",
            countQuery = "SELECT COUNT(a) FROM PortalEventAttendanceEntity a WHERE a.portalContent.id = :contentId"
    )
    Page<PortalEventAttendanceEntity> findByContentIdWithUser(@Param("contentId") Long contentId, Pageable pageable);
}
