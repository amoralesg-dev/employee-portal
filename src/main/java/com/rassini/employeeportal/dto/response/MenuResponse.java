package com.rassini.employeeportal.dto.response;

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

    /** ID del menú padre; null si es menú raíz. */
    private Long parentId;

    /** Submenús hijos. Solo se pobla en consultas jerárquicas. */
    private List<MenuResponse> children;
}
