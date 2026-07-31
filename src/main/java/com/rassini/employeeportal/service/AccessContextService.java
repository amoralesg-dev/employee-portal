package com.rassini.employeeportal.service;

import com.rassini.employeeportal.dto.response.UserAccessContextResponse;

/**
 * Servicio para obtener el contexto de acceso consolidado de un usuario.
 * <p>
 * Consolida roles, permisos y menús autorizados en una sola respuesta.
 */
public interface AccessContextService {

    /**
     * Obtiene el contexto de acceso completo para un usuario dado.
     *
     * @param userId ID del usuario
     * @return contexto con roles, permisos y árbol de menús autorizados
     */
    UserAccessContextResponse getAccessContext(Long userId);
}
