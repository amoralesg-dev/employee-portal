package com.rassini.employeeportal.dto.request;

import com.rassini.employeeportal.entity.PortalContentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalContentStatusRequest {
    @NotNull(message = "El estado es obligatorio")
    private PortalContentStatus status;
}
