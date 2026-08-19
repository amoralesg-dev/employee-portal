package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.dto.response.UserAccessContextResponse;
import com.rassini.employeeportal.dto.response.BusinessUnitResponse;
import com.rassini.employeeportal.entity.MenuEntity;
import com.rassini.employeeportal.entity.PermissionEntity;
import com.rassini.employeeportal.entity.RoleEntity;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.AccessContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación de {@link AccessContextService}.
 * <p>
 * Consolida el contexto de acceso de un usuario:
 * <ol>
 *   <li>Obtener usuario con sus roles</li>
 *   <li>Consolidar permisos únicos de todos los roles</li>
 *   <li>Consolidar menús únicos de todos los permisos</li>
 *   <li>Construir árbol jerárquico de menús usando {@code parent_id}</li>
 *   <li>Ordenar por {@code order_index}</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AccessContextServiceImpl implements AccessContextService {

    private final UserRepository userRepository;

    @Override
    public UserAccessContextResponse getAccessContext(Long userId) {
        // 1. Obtener usuario
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", userId));

        // 2. Obtener roles (códigos únicos)
        Set<RoleEntity> roles = user.getRoles();
        List<String> roleCodes = roles.stream()
                .map(RoleEntity::getCode)
                .sorted()
                .toList();

        // 3. Consolidar permisos sin duplicados
        Set<PermissionEntity> allPermissions = roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .collect(Collectors.toSet());

        List<String> permissionCodes = allPermissions.stream()
                .map(PermissionEntity::getCode)
                .sorted()
                .toList();

        log.info("EVIDENCIA - allPermissions: {}", permissionCodes);

        // 4. Consolidar menús sin duplicados
        Set<MenuEntity> allMenus = allPermissions.stream()
                .flatMap(permission -> permission.getMenus().stream())
                .collect(Collectors.toSet());

        log.info("EVIDENCIA - allMenus: {}", allMenus.stream().map(MenuEntity::getCode).toList());

        // 5. Construir árbol usando parent_id y ordenar por order_index
        List<MenuResponse> menuTree = buildMenuTree(allMenus);

        log.info("EVIDENCIA - buildMenuTree: {}", menuTree);

        // 6. Consolidar unidades de negocio
        List<BusinessUnitResponse> businessUnits = user.getBusinessUnits().stream()
                .map(bu -> BusinessUnitResponse.builder()
                        .id(bu.getId())
                        .code(bu.getCode())
                        .name(bu.getName())
                        .parentId(bu.getParent() != null ? bu.getParent().getId() : null)
                        .enabled(bu.getEnabled())
                        .createdAt(bu.getCreatedAt())
                        .updatedAt(bu.getUpdatedAt())
                        .build())
                .toList();

        return UserAccessContextResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .roles(roleCodes)
                .permissions(permissionCodes)
                .menus(menuTree)
                .businessUnits(businessUnits)
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Construye un árbol de menús a partir de un conjunto plano de entidades.
     * <p>
     * Incluye padres implícitos para mantener la jerarquía completa,
     * aun si el padre no está directamente asignado a un permiso.
     * Ordena por {@code orderIndex} en cada nivel.
     */
    private List<MenuResponse> buildMenuTree(Set<MenuEntity> menus) {
        if (menus.isEmpty()) {
            return Collections.emptyList();
        }

        // Recopilar todos los menús y sus ancestros para mantener jerarquía completa
        Map<Long, MenuEntity> menuMap = new LinkedHashMap<>();
        for (MenuEntity menu : menus) {
            menuMap.put(menu.getId(), menu);
            // Incluir ancestros que podrían no estar en el set de permisos
            MenuEntity current = menu.getParent();
            while (current != null && !menuMap.containsKey(current.getId())) {
                menuMap.put(current.getId(), current);
                current = current.getParent();
            }
        }

        // Construir map de hijos agrupados por parentId
        Map<Long, List<MenuEntity>> childrenByParentId = new LinkedHashMap<>();
        List<MenuEntity> roots = new ArrayList<>();

        for (MenuEntity menu : menuMap.values()) {
            if (menu.getParent() == null) {
                roots.add(menu);
            } else {
                childrenByParentId
                        .computeIfAbsent(menu.getParent().getId(), k -> new ArrayList<>())
                        .add(menu);
            }
        }

        // Ordenar raíces y cada grupo de hijos por orderIndex
        Comparator<MenuEntity> byOrderIndex = Comparator
                .comparing(MenuEntity::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder()));
        roots.sort(byOrderIndex);
        childrenByParentId.values().forEach(list -> list.sort(byOrderIndex));

        // Convertir recursivamente a DTOs
        return roots.stream()
                .map(root -> buildMenuResponseRecursive(root, childrenByParentId))
                .toList();
    }

    private MenuResponse buildMenuResponseRecursive(MenuEntity entity, Map<Long, List<MenuEntity>> childrenByParentId) {
        List<MenuEntity> children = childrenByParentId.getOrDefault(entity.getId(), Collections.emptyList());
        List<MenuResponse> childResponses = children.stream()
                .map(child -> buildMenuResponseRecursive(child, childrenByParentId))
                .toList();

        return MenuResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .label(entity.getLabel())
                .route(entity.getRoute())
                .icon(entity.getIcon())
                .orderIndex(entity.getOrderIndex())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .children(childResponses)
                .build();
    }
}
