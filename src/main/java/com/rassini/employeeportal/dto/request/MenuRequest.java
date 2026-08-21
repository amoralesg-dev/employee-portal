package com.rassini.employeeportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de solicitud para crear o actualizar un menú.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuRequest {

    @NotBlank(message = "El código del menú es obligatorio")
    @Size(max = 100, message = "El código del menú no puede exceder 100 caracteres")
    private String code;

    @Size(max = 100, message = "La etiqueta no puede exceder 100 caracteres")
    private String label;

    @Size(max = 200, message = "La ruta no puede exceder 200 caracteres")
    private String route;

    @Size(max = 50, message = "El icono no puede exceder 50 caracteres")
    private String icon;

    private Integer orderIndex;

    /** ID del menú padre; null si es menú raíz. */
    private Long parentId;

    @NotNull(message = "El ID de la aplicación es obligatorio")
    private Long applicationId;
}
