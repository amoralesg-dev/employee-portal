package com.rassini.employeeportal.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * DTO de solicitud para asignar menús a un permiso.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignMenusRequest {

    @NotEmpty(message = "Debe especificar al menos un ID de menú")
    private Set<Long> menuIds;
}
