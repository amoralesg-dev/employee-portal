package com.rassini.employeeportal.service;

import com.rassini.employeeportal.entity.MenuEntity;
import com.rassini.employeeportal.entity.UserEntity;

public interface MenuUrlResolverService {

    /**
     * Resuelve la URL para un menú dado y el usuario autenticado (si existe).
     * Si el menú es INTERNO, retorna null.
     * Si es EXTERNO, concatena parámetros de contexto no sensibles resueltos.
     */
    String resolveUrl(MenuEntity menu, UserEntity currentUser);

    /**
     * Valida que una lista o valor de parámetro no contenga variables o nombres prohibidos.
     */
    void validateParameterSafety(String paramName, String paramValue);

    /**
     * Retorna la lista de placeholders soportados para parámetros de contexto.
     */
    java.util.List<com.rassini.employeeportal.dto.response.PlaceholderResponse> getAvailablePlaceholders();
}
