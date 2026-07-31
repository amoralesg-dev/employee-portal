package com.rassini.employeeportal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para un rol.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    /** Permisos asignados al rol (sin relaciones anidadas profundas). */
    private List<PermissionResponse> permissions;
}
