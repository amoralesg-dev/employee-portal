package com.rassini.employeeportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessUnitRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must be up to 50 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must be up to 150 characters")
    private String name;

    private Long parentId;
    
    private Boolean enabled;
}
