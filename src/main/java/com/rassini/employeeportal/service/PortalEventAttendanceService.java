package com.rassini.employeeportal.service;

import com.rassini.employeeportal.dto.response.AdminEventAttendanceSummaryResponse;
import com.rassini.employeeportal.dto.response.EventAttendanceResponse;
import com.rassini.employeeportal.entity.PortalEventAttendanceStatus;
import org.springframework.data.domain.Pageable;

public interface PortalEventAttendanceService {

    EventAttendanceResponse getMyAttendance(Long userId, Long contentId);

    EventAttendanceResponse upsertMyAttendance(Long userId, Long contentId, PortalEventAttendanceStatus status);

    void deleteMyAttendance(Long userId, Long contentId);

    AdminEventAttendanceSummaryResponse getAdminAttendance(Long contentId, Pageable pageable);
}
