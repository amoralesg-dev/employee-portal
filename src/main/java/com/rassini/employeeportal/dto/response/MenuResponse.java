package com.rassini.employeeportal.dto.response;

import com.rassini.employeeportal.entity.AppType;
import com.rassini.employeeportal.entity.AuthType;
import com.rassini.employeeportal.entity.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO de respuesta para un menú.
 * <p>
 * Soporta jerarquía mediante {@code parentId} y {@code children}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuResponse {

    private Long id;
    private String code;
    private String label;
    private String route;
    private String icon;
    private Integer orderIndex;

    @Builder.Default
    private TargetType targetType = TargetType.INTERNO;

    private String externalUrl;

    @Builder.Default
    private Boolean openInNewTab = false;

    @Builder.Default
    private AppType appType = AppType.INTERNA;

    @Builder.Default
    private AuthType authType = AuthType.NONE;

    /** URL final resuelta con parámetros de contexto no sensibles. */
    private String resolvedUrl;

    /** Parámetros de URL configurados. */
    private List<MenuParameterResponse> parameters;

    /** ID del menú padre; null si es menú raíz. */
    private Long parentId;

    /** ID de la aplicación a la que pertenece el menú. */
    private Long applicationId;

    /** Submenús hijos. Solo se pobla en consultas jerárquicas. */
    private List<MenuResponse> children;
}
