package com.rassini.employeeportal.mapper;

import com.rassini.employeeportal.dto.request.PermissionRequest;
import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.dto.response.PermissionResponse;
import com.rassini.employeeportal.entity.PermissionEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper manual para convertir entre {@link PermissionEntity} y sus DTOs.
 * <p>
 * Proporciona dos variantes de respuesta:
 * <ul>
 *   <li>{@code toResponse} — incluye menús asignados (sin hijos)</li>
 *   <li>{@code toResponseShallow} — sin menús, para evitar ciclos al anidar dentro de RoleResponse</li>
 * </ul>
 */
@Component
public class PermissionMapper {

    private final MenuMapper menuMapper;

    public PermissionMapper(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    /**
     * Convierte un {@link PermissionRequest} a {@link PermissionEntity}.
     */
    public PermissionEntity toEntity(PermissionRequest request) {
        if (request == null) {
            return null;
        }
        return PermissionEntity.builder()
                .code(request.getCode())
                .description(request.getDescription())
                .build();
    }

    /**
     * Convierte un {@link PermissionEntity} a {@link PermissionResponse} incluyendo menús (sin hijos).
     */
    public PermissionResponse toResponse(PermissionEntity entity) {
        if (entity == null) {
            return null;
        }
        List<MenuResponse> menus = entity.getMenus() != null
                ? entity.getMenus().stream()
                    .map(menuMapper::toResponseShallow)
                    .toList()
                : Collections.emptyList();

        return PermissionResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .applicationId(entity.getApplication() != null ? entity.getApplication().getId() : null)
                .menus(menus)
                .build();
    }

    /**
     * Convierte un {@link PermissionEntity} a {@link PermissionResponse} SIN menús.
     * Usado para evitar ciclos cuando se anida dentro de RoleResponse.
     */
    public PermissionResponse toResponseShallow(PermissionEntity entity) {
        if (entity == null) {
            return null;
        }
        return PermissionResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .applicationId(entity.getApplication() != null ? entity.getApplication().getId() : null)
                .build();
    }
}
