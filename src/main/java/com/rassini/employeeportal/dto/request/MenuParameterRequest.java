package com.rassini.employeeportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuParameterRequest {

    private Long id;

    @NotBlank(message = "El nombre del parámetro es obligatorio")
    @Size(max = 100, message = "El nombre del parámetro no puede exceder 100 caracteres")
    private String paramName;

    @NotBlank(message = "El valor del parámetro es obligatorio")
    @Size(max = 255, message = "El valor del parámetro no puede exceder 255 caracteres")
    private String paramValue;

    @Builder.Default
    private Boolean active = true;
}
