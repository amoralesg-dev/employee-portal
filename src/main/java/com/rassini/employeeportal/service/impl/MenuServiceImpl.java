package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.request.MenuRequest;
import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.entity.MenuEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.MenuMapper;
import com.rassini.employeeportal.repository.ApplicationRepository;
import com.rassini.employeeportal.entity.ApplicationEntity;
import com.rassini.employeeportal.repository.MenuRepository;
import com.rassini.employeeportal.service.MenuService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación de {@link MenuService}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final ApplicationRepository applicationRepository;
    private final MenuMapper menuMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MenuResponse> getMenus() {
        return menuRepository.findAll().stream()
                .map(menuMapper::toResponseShallow)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MenuResponse getMenuById(Long id) {
        MenuEntity menu = findMenuOrThrow(id);
        return menuMapper.toResponse(menu);
    }

    @Override
    public MenuResponse createMenu(MenuRequest request) {
        if (menuRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException("El código de menú '" + request.getCode() + "' ya existe");
        }

        MenuEntity entity = menuMapper.toEntity(request);

        ApplicationEntity application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Aplicación", "id", request.getApplicationId()));
        entity.setApplication(application);

        if (request.getParentId() != null) {
            MenuEntity parent = menuRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menú padre", "id", request.getParentId()));
            entity.setParent(parent);
        }

        MenuEntity saved = menuRepository.save(entity);
        return menuMapper.toResponse(saved);
    }

    @Override
    public MenuResponse updateMenu(Long id, MenuRequest request) {
        MenuEntity menu = findMenuOrThrow(id);

        if (!menu.getCode().equals(request.getCode()) && menuRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException("El código de menú '" + request.getCode() + "' ya existe");
        }

        menu.setCode(request.getCode());
        menu.setLabel(request.getLabel());
        menu.setRoute(request.getRoute());
        menu.setIcon(request.getIcon());
        menu.setOrderIndex(request.getOrderIndex());

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

        MenuEntity saved = menuRepository.save(menu);
        return menuMapper.toResponse(saved);
    }

    @Override
    public void deleteMenu(Long id) {
        MenuEntity menu = findMenuOrThrow(id);
        menuRepository.delete(menu);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuResponse> getMenuTree() {
        List<MenuEntity> roots = menuRepository.findByParentIsNullOrderByOrderIndexAsc();
        return roots.stream()
                .map(menuMapper::toResponse)
                .toList();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private MenuEntity findMenuOrThrow(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menú", "id", id));
    }
}
