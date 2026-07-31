package com.rassini.employeeportal.repository;

import com.rassini.employeeportal.entity.MenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link MenuEntity}.
 */
@Repository
public interface MenuRepository extends JpaRepository<MenuEntity, Long> {

    Optional<MenuEntity> findByCode(String code);

    /**
     * Retorna los menús raíz (sin padre) ordenados por {@code orderIndex}.
     */
    List<MenuEntity> findByParentIsNullOrderByOrderIndexAsc();

    /**
     * Retorna los menús cuyos IDs estén en la colección dada.
     */
    List<MenuEntity> findByIdIn(Collection<Long> ids);
}
