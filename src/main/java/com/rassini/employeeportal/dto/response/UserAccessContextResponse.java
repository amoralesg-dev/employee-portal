package com.rassini.employeeportal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO de respuesta que consolida el contexto de acceso de un usuario:
 * sus roles, permisos y menús autorizados.
 * <p>
 * Usado para obtener en una sola llamada toda la información de autorización
 * del usuario sin exponer datos sensibles.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccessContextResponse {

    private Long userId;
    private String username;
    private List<String> roles;
    private List<String> permissions;
    private List<MenuResponse> menus;
    private List<BusinessUnitResponse> businessUnits;
}
