package com.rassini.employeeportal.repository;

import com.rassini.employeeportal.entity.PortalContentCategory;
import com.rassini.employeeportal.entity.PortalContentEntity;
import com.rassini.employeeportal.entity.PortalContentStatus;
import com.rassini.employeeportal.entity.PortalContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PortalContentRepository extends JpaRepository<PortalContentEntity, Long> {

    @Query("""
            SELECT DISTINCT c FROM PortalContentEntity c
            LEFT JOIN c.translations t
            WHERE (:type IS NULL OR c.contentType = :type)
              AND (:status IS NULL OR c.status = :status)
              AND (:category IS NULL OR c.category = :category)
              AND (:q IS NULL OR :q = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(t.summary) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<PortalContentEntity> searchAdmin(
            @Param("type") PortalContentType type,
            @Param("status") PortalContentStatus status,
            @Param("category") PortalContentCategory category,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT c FROM PortalContentEntity c
            LEFT JOIN FETCH c.translations
            LEFT JOIN FETCH c.businessUnits
            LEFT JOIN FETCH c.roles
            LEFT JOIN FETCH c.image
            WHERE c.status = :status
              AND (c.publishFrom IS NULL OR c.publishFrom <= :now)
              AND (c.publishTo IS NULL OR c.publishTo > :now)
            """)
    List<PortalContentEntity> findActivePublished(
            @Param("status") PortalContentStatus status,
            @Param("now") LocalDateTime now
    );
}
