package com.rassini.employeeportal.mapper;

import com.rassini.employeeportal.dto.request.UserRequest;
import com.rassini.employeeportal.dto.response.RoleResponse;
import com.rassini.employeeportal.dto.response.UserResponse;
import com.rassini.employeeportal.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper manual para convertir entre {@link UserEntity} y sus DTOs.
 * <p>
 * NUNCA mapea el campo {@code passwordHash} hacia DTOs de respuesta.
 * Evita ciclos en relaciones ManyToMany mapeando roles de forma superficial.
 */
@Component
public class UserMapper {

    private final RoleMapper roleMapper;

    public UserMapper(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    /**
     * Convierte un {@link UserRequest} a {@link UserEntity}.
     * <p>
     * El campo {@code passwordHash} se deja como el valor del password del request
     * (el service debe encriptarlo antes de persistir).
     */
    public UserEntity toEntity(UserRequest request) {
        if (request == null) {
            return null;
        }
        return UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(request.getPassword())
                .enabled(true)
                .build();
    }

    /**
     * Convierte un {@link UserEntity} a {@link UserResponse}.
     * <p>
     * NUNCA incluye passwordHash. Los roles se mapean de forma superficial
     * (sin incluir permisos del rol) para evitar ciclos.
     */
    public UserResponse toResponse(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        List<RoleResponse> roles = entity.getRoles() != null
                ? entity.getRoles().stream()
                    .map(roleMapper::toResponseShallow)
                    .toList()
                : Collections.emptyList();

        return UserResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .enabled(entity.getEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .roles(roles)
                .build();
    }
}
