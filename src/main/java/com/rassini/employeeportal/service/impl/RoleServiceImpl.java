package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.RoleRequest;
import com.rassini.employeeportal.dto.response.PermissionResponse;
import com.rassini.employeeportal.dto.response.RoleResponse;
import com.rassini.employeeportal.entity.PermissionEntity;
import com.rassini.employeeportal.entity.RoleEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.PermissionMapper;
import com.rassini.employeeportal.mapper.RoleMapper;
import com.rassini.employeeportal.repository.PermissionRepository;
import com.rassini.employeeportal.repository.RoleRepository;
import com.rassini.employeeportal.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementación de {@link RoleService}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        RoleEntity role = findRoleOrThrow(id);
        return roleMapper.toResponse(role);
    }

    @Override
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw new BusinessException("El código de rol '" + request.getCode() + "' ya existe");
        }

        RoleEntity entity = roleMapper.toEntity(request);
        entity.setCreatedAt(LocalDateTime.now());

        RoleEntity saved = roleRepository.save(entity);
        return roleMapper.toResponse(saved);
    }

    @Override
    public RoleResponse updateRole(Long id, RoleRequest request) {
        RoleEntity role = findRoleOrThrow(id);

        if (!role.getCode().equals(request.getCode()) && roleRepository.existsByCode(request.getCode())) {
            throw new BusinessException("El código de rol '" + request.getCode() + "' ya existe");
        }

        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());

        RoleEntity saved = roleRepository.save(role);
        return roleMapper.toResponse(saved);
    }

    @Override
    public void deleteRole(Long id) {
        RoleEntity role = findRoleOrThrow(id);
        roleRepository.delete(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getRolePermissions(Long id) {
        RoleEntity role = findRoleOrThrow(id);
        return role.getPermissions().stream()
                .map(permissionMapper::toResponseShallow)
                .toList();
    }

    @Override
    public RoleResponse replaceRolePermissions(Long id, Set<Long> permissionIds) {
        RoleEntity role = findRoleOrThrow(id);

        List<PermissionEntity> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new BusinessException("Uno o más IDs de permiso no existen");
        }

        role.setPermissions(new HashSet<>(permissions));

        RoleEntity saved = roleRepository.save(role);
        return roleMapper.toResponse(saved);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private RoleEntity findRoleOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", "id", id));
    }
}
