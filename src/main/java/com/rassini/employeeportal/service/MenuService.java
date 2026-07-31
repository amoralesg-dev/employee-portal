package com.rassini.employeeportal.service;

import com.rassini.employeeportal.dto.request.MenuRequest;
import com.rassini.employeeportal.dto.response.MenuResponse;

import java.util.List;

/**
 * Servicio para gestión de menús.
 */
public interface MenuService {

    List<MenuResponse> getMenus();

    MenuResponse getMenuById(Long id);

    MenuResponse createMenu(MenuRequest request);

    MenuResponse updateMenu(Long id, MenuRequest request);

    void deleteMenu(Long id);

    /**
     * Retorna el árbol completo de menús (raíces con hijos anidados),
     * ordenado por {@code orderIndex}.
     */
    List<MenuResponse> getMenuTree();
}
