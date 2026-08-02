package com.rassini.employeeportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "currentPassword es obligatorio")
    private String currentPassword;
    
    @NotBlank(message = "newPassword es obligatorio")
    private String newPassword;
    
    @NotBlank(message = "confirmPassword es obligatorio")
    private String confirmPassword;
}
