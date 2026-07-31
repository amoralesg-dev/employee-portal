package com.rassini.employeeportal.service;

import com.rassini.employeeportal.dto.request.RoleRequest;
import com.rassini.employeeportal.dto.response.PermissionResponse;
import com.rassini.employeeportal.dto.response.RoleResponse;

import java.util.List;
import java.util.Set;

/**
 * Servicio para gestión de roles.
 */
public interface RoleService {

    List<RoleResponse> getRoles();

    RoleResponse getRoleById(Long id);

    RoleResponse createRole(RoleRequest request);

    RoleResponse updateRole(Long id, RoleRequest request);

    void deleteRole(Long id);

    List<PermissionResponse> getRolePermissions(Long id);

    RoleResponse replaceRolePermissions(Long id, Set<Long> permissionIds);
}
