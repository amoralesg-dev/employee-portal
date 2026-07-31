package com.rassini.employeeportal.mapper;

import com.rassini.employeeportal.dto.request.RoleRequest;
import com.rassini.employeeportal.dto.response.PermissionResponse;
import com.rassini.employeeportal.dto.response.RoleResponse;
import com.rassini.employeeportal.entity.RoleEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper manual para convertir entre {@link RoleEntity} y sus DTOs.
 * <p>
 * Proporciona dos variantes de respuesta:
 * <ul>
 *   <li>{@code toResponse} — incluye permisos (sin menús anidados)</li>
 *   <li>{@code toResponseShallow} — sin permisos, para evitar ciclos al anidar dentro de UserResponse</li>
 * </ul>
 */
@Component
public class RoleMapper {

    private final PermissionMapper permissionMapper;

    public RoleMapper(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    /**
     * Convierte un {@link RoleRequest} a {@link RoleEntity}.
     */
    public RoleEntity toEntity(RoleRequest request) {
        if (request == null) {
            return null;
        }
        return RoleEntity.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    /**
     * Convierte un {@link RoleEntity} a {@link RoleResponse} incluyendo permisos (sin menús).
     */
    public RoleResponse toResponse(RoleEntity entity) {
        if (entity == null) {
            return null;
        }
        List<PermissionResponse> permissions = entity.getPermissions() != null
                ? entity.getPermissions().stream()
                    .map(permissionMapper::toResponseShallow)
                    .toList()
                : Collections.emptyList();

        return RoleResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .permissions(permissions)
                .build();
    }

    /**
     * Convierte un {@link RoleEntity} a {@link RoleResponse} SIN permisos.
     * Usado para evitar ciclos cuando se anida dentro de UserResponse.
     */
    public RoleResponse toResponseShallow(RoleEntity entity) {
        if (entity == null) {
            return null;
        }
        return RoleResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
