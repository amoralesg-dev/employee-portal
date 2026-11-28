package com.rassini.employeeportal.dto.response;

import com.rassini.employeeportal.entity.PortalEventAttendanceStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventAttendanceResponse {
    private Long contentId;
    private PortalEventAttendanceStatus status;
    private LocalDateTime respondedAt;
    private boolean canRespond;
}
