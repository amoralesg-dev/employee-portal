package com.rassini.employeeportal.dto.request;

import com.rassini.employeeportal.entity.PortalEventAttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventAttendanceRequest {

    @NotNull(message = "El estado de asistencia es obligatorio")
    private PortalEventAttendanceStatus status;
}
