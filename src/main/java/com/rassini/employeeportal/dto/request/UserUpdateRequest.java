package com.rassini.employeeportal.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * DTO de solicitud para actualizar datos de un usuario existente.
 * <p>
 * Solo permite modificar username, email y enabled.
 * NUNCA expone ni modifica passwordHash.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @Size(max = 100, message = "El nombre de usuario no puede exceder 100 caracteres")
    private String username;

    @Email(message = "El formato del email no es válido")
    @Size(max = 150, message = "El email no puede exceder 150 caracteres")
    private String email;

    private Boolean hasAllBusinessUnits;
    private Set<Long> businessUnitIds;
}
