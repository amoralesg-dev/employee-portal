package com.rassini.employeeportal.dto.mfa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MfaDisableRequest {
    @NotBlank
    private String password;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "El codigo debe tener exactamente 6 digitos")
    private String code;
}
