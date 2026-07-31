package com.rassini.employeeportal.mapper;

import com.rassini.employeeportal.dto.request.MenuRequest;
import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.entity.MenuEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper manual para convertir entre {@link MenuEntity} y sus DTOs.
 * <p>
 * Proporciona dos variantes de respuesta:
 * <ul>
 *   <li>{@code toResponse} — incluye hijos recursivamente</li>
 *   <li>{@code toResponseShallow} — sin hijos, para evitar ciclos cuando se anida en PermissionResponse</li>
 * </ul>
 */
@Component
public class MenuMapper {

    /**
     * Convierte un {@link MenuRequest} a {@link MenuEntity}.
     * <p>
     * El campo {@code parent} NO se asigna aquí — el service debe resolverlo por {@code parentId}.
     */
    public MenuEntity toEntity(MenuRequest request) {
        if (request == null) {
            return null;
        }
        return MenuEntity.builder()
                .code(request.getCode())
                .label(request.getLabel())
                .route(request.getRoute())
                .icon(request.getIcon())
                .orderIndex(request.getOrderIndex())
                .build();
    }

    /**
     * Convierte un {@link MenuEntity} a {@link MenuResponse} incluyendo hijos recursivamente.
     */
    public MenuResponse toResponse(MenuEntity entity) {
        if (entity == null) {
            return null;
        }
        List<MenuResponse> children = entity.getChildren() != null && !entity.getChildren().isEmpty()
                ? entity.getChildren().stream()
                    .map(this::toResponse)
                    .toList()
                : Collections.emptyList();

        return MenuResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .label(entity.getLabel())
                .route(entity.getRoute())
                .icon(entity.getIcon())
                .orderIndex(entity.getOrderIndex())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .children(children)
                .build();
    }

    /**
     * Convierte un {@link MenuEntity} a {@link MenuResponse} SIN hijos.
     * Usado para evitar ciclos cuando se anida dentro de PermissionResponse.
     */
    public MenuResponse toResponseShallow(MenuEntity entity) {
        if (entity == null) {
            return null;
        }
        return MenuResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .label(entity.getLabel())
                .route(entity.getRoute())
                .icon(entity.getIcon())
                .orderIndex(entity.getOrderIndex())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .build();
    }
}
