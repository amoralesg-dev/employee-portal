package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.MenuParameterRequest;
import com.rassini.employeeportal.dto.request.MenuRequest;
import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.entity.ApplicationEntity;
import com.rassini.employeeportal.entity.MenuEntity;
import com.rassini.employeeportal.entity.MenuParameterEntity;
import com.rassini.employeeportal.entity.TargetType;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.MenuMapper;
import com.rassini.employeeportal.repository.ApplicationRepository;
import com.rassini.employeeportal.repository.MenuRepository;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.MenuService;
import com.rassini.employeeportal.service.MenuUrlResolverService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementación de {@link MenuService}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final MenuMapper menuMapper;
    private final MenuUrlResolverService menuUrlResolverService;

    @Override
    @Transactional(readOnly = true)
    public List<MenuResponse> getMenus() {
        UserEntity currentUser = getCurrentUserOrNull();
        return menuRepository.findAll().stream()
                .map(menu -> {
                    MenuResponse resp = menuMapper.toResponseShallow(menu);
                    resp.setResolvedUrl(menuUrlResolverService.resolveUrl(menu, currentUser));
                    return resp;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MenuResponse getMenuById(Long id) {
        MenuEntity menu = findMenuOrThrow(id);
        UserEntity currentUser = getCurrentUserOrNull();
        MenuResponse resp = menuMapper.toResponse(menu);
        resp.setResolvedUrl(menuUrlResolverService.resolveUrl(menu, currentUser));
        enrichChildrenWithResolvedUrl(resp, currentUser);
        return resp;
    }

    @Override
    public MenuResponse createMenu(MenuRequest request) {
        if (menuRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException("El código de menú '" + request.getCode() + "' ya existe");
        }

        validateMenuRequest(request);

        MenuEntity entity = menuMapper.toEntity(request);

        ApplicationEntity application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Aplicación", "id", request.getApplicationId()));
        entity.setApplication(application);

        if (request.getParentId() != null) {
            MenuEntity parent = menuRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menú padre", "id", request.getParentId()));
            entity.setParent(parent);
        }

        // Parámetros de contexto
        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            Set<MenuParameterEntity> params = new HashSet<>();
            for (MenuParameterRequest pReq : request.getParameters()) {
                menuUrlResolverService.validateParameterSafety(pReq.getParamName(), pReq.getParamValue());
                params.add(menuMapper.toParameterEntity(pReq, entity));
            }
            entity.setParameters(params);
        }

        MenuEntity saved = menuRepository.save(entity);
        UserEntity currentUser = getCurrentUserOrNull();
        MenuResponse resp = menuMapper.toResponse(saved);
        resp.setResolvedUrl(menuUrlResolverService.resolveUrl(saved, currentUser));
        return resp;
    }

    @Override
    public MenuResponse updateMenu(Long id, MenuRequest request) {
        MenuEntity menu = findMenuOrThrow(id);

        if (!menu.getCode().equals(request.getCode()) && menuRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException("El código de menú '" + request.getCode() + "' ya existe");
        }

        validateMenuRequest(request);

        menu.setCode(request.getCode());
        menu.setLabel(request.getLabel());
        menu.setRoute(request.getRoute());
        menu.setIcon(request.getIcon());
        menu.setOrderIndex(request.getOrderIndex());
        menu.setTargetType(request.getTargetType() != null ? request.getTargetType() : TargetType.INTERNO);
        menu.setExternalUrl(request.getExternalUrl());
        menu.setOpenInNewTab(request.getOpenInNewTab() != null ? request.getOpenInNewTab() : false);
        menu.setAppType(request.getAppType() != null ? request.getAppType() : com.rassini.employeeportal.entity.AppType.INTERNA);
        menu.setAuthType(request.getAuthType() != null ? request.getAuthType() : com.rassini.employeeportal.entity.AuthType.NONE);

        ApplicationEntity application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Aplicación", "id", request.getApplicationId()));
        menu.setApplication(application);

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BusinessException("Un menú no puede ser su propio padre");
            }
            MenuEntity parent = menuRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menú padre", "id", request.getParentId()));
            menu.setParent(parent);
        } else {
            menu.setParent(null);
        }

        // Actualizar parámetros
        menu.getParameters().clear();
        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            for (MenuParameterRequest pReq : request.getParameters()) {
                menuUrlResolverService.validateParameterSafety(pReq.getParamName(), pReq.getParamValue());
                menu.getParameters().add(menuMapper.toParameterEntity(pReq, menu));
            }
        }

        MenuEntity saved = menuRepository.save(menu);
        UserEntity currentUser = getCurrentUserOrNull();
        MenuResponse resp = menuMapper.toResponse(saved);
        resp.setResolvedUrl(menuUrlResolverService.resolveUrl(saved, currentUser));
        return resp;
    }

    @Override
    public void deleteMenu(Long id) {
        MenuEntity menu = findMenuOrThrow(id);
        menuRepository.delete(menu);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuResponse> getMenuTree() {
        UserEntity currentUser = getCurrentUserOrNull();
        List<MenuEntity> roots = menuRepository.findByParentIsNullOrderByOrderIndexAsc();
        return roots.stream()
                .map(root -> {
                    MenuResponse resp = menuMapper.toResponse(root);
                    resp.setResolvedUrl(menuUrlResolverService.resolveUrl(root, currentUser));
                    enrichChildrenWithResolvedUrl(resp, currentUser);
                    return resp;
                })
                .toList();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void validateMenuRequest(MenuRequest request) {
        if (request.getTargetType() == TargetType.EXTERNO) {
            if (request.getExternalUrl() == null || request.getExternalUrl().isBlank()) {
                throw new BusinessException("La URL externa es obligatoria para menús de tipo EXTERNO");
            }
        }
    }

    private void enrichChildrenWithResolvedUrl(MenuResponse parent, UserEntity currentUser) {
        if (parent.getChildren() != null && !parent.getChildren().isEmpty()) {
            for (MenuResponse child : parent.getChildren()) {
                if (child.getTargetType() == TargetType.EXTERNO && child.getExternalUrl() != null) {
                    menuRepository.findById(child.getId()).ifPresent(childEntity -> {
                        child.setResolvedUrl(menuUrlResolverService.resolveUrl(childEntity, currentUser));
                    });
                }
                enrichChildrenWithResolvedUrl(child, currentUser);
            }
        }
    }

    private MenuEntity findMenuOrThrow(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menú", "id", id));
    }

    private UserEntity getCurrentUserOrNull() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return userRepository.findByUsername(auth.getName()).orElse(null);
            }
        } catch (Exception e) {
            // Contexto no autenticado o llamada de prueba
        }
        return null;
    }
}
