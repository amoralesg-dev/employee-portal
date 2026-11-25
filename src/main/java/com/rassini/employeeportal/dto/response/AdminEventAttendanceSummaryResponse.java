package com.rassini.employeeportal.dto.response;

import com.rassini.employeeportal.entity.PortalEventAttendanceStatus;
import lombok.*;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminEventAttendanceSummaryResponse {
    private Long contentId;
    private long goingCount;
    private long notGoingCount;
    private Page<AttendeeItem> attendees;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendeeItem {
        private Long userId;
        private String displayName;
        private String username;
        private String email;
        private PortalEventAttendanceStatus status;
        private LocalDateTime respondedAt;
    }
}
