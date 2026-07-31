package com.rassini.employeeportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de solicitud para crear o actualizar un permiso.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionRequest {

    @NotBlank(message = "El código del permiso es obligatorio")
    @Size(max = 100, message = "El código del permiso no puede exceder 100 caracteres")
    private String code;

    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String description;
}
