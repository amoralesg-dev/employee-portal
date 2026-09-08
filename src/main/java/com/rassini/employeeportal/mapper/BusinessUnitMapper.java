package com.rassini.employeeportal.mapper;

import com.rassini.employeeportal.dto.response.BusinessUnitResponse;
import com.rassini.employeeportal.entity.BusinessUnitEntity;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper manual reutilizable para BusinessUnitEntity a BusinessUnitResponse.
 * Evita la duplicación de código y previene ciclos bidireccionales.
 */
@Component
public class BusinessUnitMapper {

    /**
     * Convierte una entidad de Business Unit en su DTO de respuesta superficial (sin mapear hijos).
     */
    public BusinessUnitResponse toResponse(BusinessUnitEntity entity) {
        if (entity == null) {
            return null;
        }
        return BusinessUnitResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .enabled(entity.getEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convierte una colección de entidades en una lista de DTOs superficiales.
     */
    public List<BusinessUnitResponse> toResponseList(Collection<BusinessUnitEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convierte recursivamente la entidad de Business Unit y sus hijos para representación en árbol.
     */
    public BusinessUnitResponse toResponseWithChildren(BusinessUnitEntity entity) {
        if (entity == null) {
            return null;
        }
        BusinessUnitResponse response = toResponse(entity);
        if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
            response.setChildren(entity.getChildren().stream()
                    .map(this::toResponseWithChildren)
                    .collect(Collectors.toList()));
        }
        return response;
    }
}
