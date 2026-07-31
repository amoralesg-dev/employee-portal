package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.PermissionRequest;
import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.dto.response.PermissionResponse;
import com.rassini.employeeportal.entity.MenuEntity;
import com.rassini.employeeportal.entity.PermissionEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.MenuMapper;
import com.rassini.employeeportal.mapper.PermissionMapper;
import com.rassini.employeeportal.repository.MenuRepository;
import com.rassini.employeeportal.repository.PermissionRepository;
import com.rassini.employeeportal.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementación de {@link PermissionService}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final MenuRepository menuRepository;
    private final PermissionMapper permissionMapper;
    private final MenuMapper menuMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissions() {
        return permissionRepository.findAll().stream()
                .map(permissionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(Long id) {
        PermissionEntity permission = findPermissionOrThrow(id);
        return permissionMapper.toResponse(permission);
    }

    @Override
    public PermissionResponse createPermission(PermissionRequest request) {
        if (permissionRepository.existsByCode(request.getCode())) {
            throw new BusinessException("El código de permiso '" + request.getCode() + "' ya existe");
        }

        PermissionEntity entity = permissionMapper.toEntity(request);
        entity.setCreatedAt(LocalDateTime.now());

        PermissionEntity saved = permissionRepository.save(entity);
        return permissionMapper.toResponse(saved);
    }

    @Override
    public PermissionResponse updatePermission(Long id, PermissionRequest request) {
        PermissionEntity permission = findPermissionOrThrow(id);

        if (!permission.getCode().equals(request.getCode()) && permissionRepository.existsByCode(request.getCode())) {
            throw new BusinessException("El código de permiso '" + request.getCode() + "' ya existe");
        }

        permission.setCode(request.getCode());
        permission.setDescription(request.getDescription());

        PermissionEntity saved = permissionRepository.save(permission);
        return permissionMapper.toResponse(saved);
    }

    @Override
    public void deletePermission(Long id) {
        PermissionEntity permission = findPermissionOrThrow(id);
        permissionRepository.delete(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuResponse> getPermissionMenus(Long id) {
        PermissionEntity permission = findPermissionOrThrow(id);
        return permission.getMenus().stream()
                .map(menuMapper::toResponseShallow)
                .toList();
    }

    @Override
    public PermissionResponse replacePermissionMenus(Long id, Set<Long> menuIds) {
        PermissionEntity permission = findPermissionOrThrow(id);

        List<MenuEntity> menus = menuRepository.findAllById(menuIds);
        if (menus.size() != menuIds.size()) {
            throw new BusinessException("Uno o más IDs de menú no existen");
        }

        permission.setMenus(new HashSet<>(menus));

        PermissionEntity saved = permissionRepository.save(permission);
        return permissionMapper.toResponse(saved);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private PermissionEntity findPermissionOrThrow(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso", "id", id));
    }
}
