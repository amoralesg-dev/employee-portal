package com.rassini.employeeportal.service;

import com.rassini.employeeportal.dto.request.PermissionRequest;
import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.dto.response.PermissionResponse;

import java.util.List;
import java.util.Set;

/**
 * Servicio para gestión de permisos.
 */
public interface PermissionService {

    List<PermissionResponse> getPermissions();

    PermissionResponse getPermissionById(Long id);

    PermissionResponse createPermission(PermissionRequest request);

    PermissionResponse updatePermission(Long id, PermissionRequest request);

    void deletePermission(Long id);

    List<MenuResponse> getPermissionMenus(Long id);

    PermissionResponse replacePermissionMenus(Long id, Set<Long> menuIds);
}
