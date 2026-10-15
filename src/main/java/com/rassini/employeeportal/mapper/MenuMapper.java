package com.rassini.employeeportal.mapper;

import com.rassini.employeeportal.dto.request.MenuParameterRequest;
import com.rassini.employeeportal.dto.request.MenuRequest;
import com.rassini.employeeportal.dto.response.MenuParameterResponse;
import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.entity.MenuEntity;
import com.rassini.employeeportal.entity.MenuParameterEntity;
import com.rassini.employeeportal.entity.TargetType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper manual para convertir entre {@link MenuEntity} y sus DTOs.
 */
@Component
public class MenuMapper {

    /**
     * Convierte un {@link MenuRequest} a {@link MenuEntity}.
     */
    public MenuEntity toEntity(MenuRequest request) {
        if (request == null) {
            return null;
        }
        return MenuEntity.builder()
                .code(request.getCode())
                .label(request.getLabel())
                .route(request.getRoute())
                .icon(request.getIcon())
                .orderIndex(request.getOrderIndex())
                .targetType(request.getTargetType() != null ? request.getTargetType() : TargetType.INTERNO)
                .externalUrl(request.getExternalUrl())
                .openInNewTab(request.getOpenInNewTab() != null ? request.getOpenInNewTab() : false)
                .appType(request.getAppType() != null ? request.getAppType() : com.rassini.employeeportal.entity.AppType.INTERNA)
                .authType(request.getAuthType() != null ? request.getAuthType() : com.rassini.employeeportal.entity.AuthType.NONE)
                .build();
    }

    /**
     * Convierte un {@link MenuEntity} a {@link MenuResponse} incluyendo hijos recursivamente.
     */
    public MenuResponse toResponse(MenuEntity entity) {
        if (entity == null) {
            return null;
        }
        List<MenuResponse> children = entity.getChildren() != null && !entity.getChildren().isEmpty()
                ? entity.getChildren().stream()
                    .map(this::toResponse)
                    .sorted(java.util.Comparator.comparing(MenuResponse::getOrderIndex, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                    .toList()
                : Collections.emptyList();

        List<MenuParameterResponse> parameterResponses = entity.getParameters() != null
                ? entity.getParameters().stream()
                    .map(this::toParameterResponse)
                    .toList()
                : Collections.emptyList();

        return MenuResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .label(entity.getLabel())
                .route(entity.getRoute())
                .icon(entity.getIcon())
                .orderIndex(entity.getOrderIndex())
                .targetType(entity.getTargetType())
                .externalUrl(entity.getExternalUrl())
                .openInNewTab(entity.getOpenInNewTab())
                .appType(entity.getAppType())
                .authType(entity.getAuthType())
                .parameters(parameterResponses)
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .applicationId(entity.getApplication() != null ? entity.getApplication().getId() : null)
                .children(children)
                .build();
    }

    /**
     * Convierte un {@link MenuEntity} a {@link MenuResponse} SIN hijos.
     */
    public MenuResponse toResponseShallow(MenuEntity entity) {
        if (entity == null) {
            return null;
        }
        List<MenuParameterResponse> parameterResponses = entity.getParameters() != null
                ? entity.getParameters().stream()
                    .map(this::toParameterResponse)
                    .toList()
                : Collections.emptyList();

        return MenuResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .label(entity.getLabel())
                .route(entity.getRoute())
                .icon(entity.getIcon())
                .orderIndex(entity.getOrderIndex())
                .targetType(entity.getTargetType())
                .externalUrl(entity.getExternalUrl())
                .openInNewTab(entity.getOpenInNewTab())
                .appType(entity.getAppType())
                .authType(entity.getAuthType())
                .parameters(parameterResponses)
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .applicationId(entity.getApplication() != null ? entity.getApplication().getId() : null)
                .build();
    }

    public MenuParameterResponse toParameterResponse(MenuParameterEntity param) {
        if (param == null) return null;
        return MenuParameterResponse.builder()
                .id(param.getId())
                .paramName(param.getParamName())
                .paramValue(param.getParamValue())
                .active(param.getActive())
                .build();
    }

    public MenuParameterEntity toParameterEntity(MenuParameterRequest req, MenuEntity menu) {
        if (req == null) return null;
        return MenuParameterEntity.builder()
                .id(req.getId())
                .menu(menu)
                .paramName(req.getParamName())
                .paramValue(req.getParamValue())
                .active(req.getActive() != null ? req.getActive() : true)
                .build();
    }
}
