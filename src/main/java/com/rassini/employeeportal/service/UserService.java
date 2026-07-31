package com.rassini.employeeportal.service;

import com.rassini.employeeportal.dto.request.UpdateStatusRequest;
import com.rassini.employeeportal.dto.request.UserRequest;
import com.rassini.employeeportal.dto.request.UserUpdateRequest;
import com.rassini.employeeportal.dto.response.RoleResponse;
import com.rassini.employeeportal.dto.response.UserResponse;

import java.util.List;
import java.util.Set;

/**
 * Servicio para gestión de usuarios.
 */
public interface UserService {

    List<UserResponse> getUsers();

    UserResponse getUserById(Long id);

    UserResponse createUser(UserRequest request);

    UserResponse updateUser(Long id, UserUpdateRequest request);

    UserResponse updateStatus(Long id, UpdateStatusRequest request);

    List<RoleResponse> getUserRoles(Long id);

    UserResponse replaceUserRoles(Long id, Set<Long> roleIds);
}
