package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.response.AdminEventAttendanceSummaryResponse;
import com.rassini.employeeportal.dto.response.EventAttendanceResponse;
import com.rassini.employeeportal.entity.*;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.repository.PortalContentRepository;
import com.rassini.employeeportal.repository.PortalEventAttendanceRepository;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.PortalEventAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortalEventAttendanceServiceImpl implements PortalEventAttendanceService {

    private final PortalContentRepository contentRepository;
    private final PortalEventAttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public EventAttendanceResponse getMyAttendance(Long userId, Long contentId) {
        LocalDateTime now = LocalDateTime.now();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", userId));
        PortalContentEntity content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", contentId));

        if (content.getContentType() != PortalContentType.EVENT || !PortalContentVisibilityHelper.matchesAudience(content, user)) {
            throw new ResourceNotFoundException("Evento", "id", contentId);
        }

        boolean canRespond = PortalContentVisibilityHelper.isPublishedAndActive(content, now)
                && PortalContentVisibilityHelper.isEventNotPast(content, now);
        Optional<PortalEventAttendanceEntity> existing = attendanceRepository.findByPortalContentIdAndUserId(contentId, userId);

        if (!canRespond && existing.isEmpty()) {
            throw new ResourceNotFoundException("Evento", "id", contentId);
        }

        return EventAttendanceResponse.builder()
                .contentId(contentId)
                .status(existing.map(PortalEventAttendanceEntity::getStatus).orElse(null))
                .respondedAt(existing.map(a -> a.getUpdatedAt() != null ? a.getUpdatedAt() : a.getCreatedAt()).orElse(null))
                .canRespond(canRespond)
                .build();
    }

    @Override
    @Transactional
    public EventAttendanceResponse upsertMyAttendance(Long userId, Long contentId, PortalEventAttendanceStatus status) {
        if (status == null) {
            throw new BusinessException("El estado de asistencia es obligatorio");
        }
        LocalDateTime now = LocalDateTime.now();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", userId));
        PortalContentEntity content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", contentId));

        if (content.getContentType() != PortalContentType.EVENT) {
            throw new BusinessException("La confirmación de asistencia solo aplica para eventos");
        }
        if (!PortalContentVisibilityHelper.matchesAudience(content, user)) {
            throw new ResourceNotFoundException("Evento", "id", contentId);
        }
        if (!PortalContentVisibilityHelper.isPublishedAndActive(content, now)
                || !PortalContentVisibilityHelper.isEventNotPast(content, now)) {
            throw new BusinessException("Este evento ya no admite cambios en la confirmación de asistencia");
        }

        PortalEventAttendanceEntity entity = attendanceRepository.findByPortalContentIdAndUserId(contentId, userId)
                .orElseGet(() -> PortalEventAttendanceEntity.builder()
                        .portalContent(content)
                        .user(user)
                        .createdAt(now)
                        .build());

        entity.setStatus(status);
        entity.setUpdatedAt(now);
        PortalEventAttendanceEntity saved = attendanceRepository.save(entity);

        return EventAttendanceResponse.builder()
                .contentId(contentId)
                .status(saved.getStatus())
                .respondedAt(saved.getUpdatedAt() != null ? saved.getUpdatedAt() : saved.getCreatedAt())
                .canRespond(true)
                .build();
    }

    @Override
    @Transactional
    public void deleteMyAttendance(Long userId, Long contentId) {
        LocalDateTime now = LocalDateTime.now();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", userId));
        PortalContentEntity content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", contentId));

        if (content.getContentType() != PortalContentType.EVENT || !PortalContentVisibilityHelper.matchesAudience(content, user)) {
            throw new ResourceNotFoundException("Evento", "id", contentId);
        }
        if (!PortalContentVisibilityHelper.isPublishedAndActive(content, now)
                || !PortalContentVisibilityHelper.isEventNotPast(content, now)) {
            throw new BusinessException("Este evento ya no admite cambios en la confirmación de asistencia");
        }

        attendanceRepository.deleteByPortalContentIdAndUserId(contentId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminEventAttendanceSummaryResponse getAdminAttendance(Long contentId, Pageable pageable) {
        PortalContentEntity content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Contenido", "id", contentId));

        if (content.getContentType() != PortalContentType.EVENT) {
            throw new BusinessException("La consulta de confirmaciones solo aplica para contenido de tipo EVENT");
        }

        long goingCount = attendanceRepository.countByPortalContentIdAndStatus(contentId, PortalEventAttendanceStatus.GOING);
        long notGoingCount = attendanceRepository.countByPortalContentIdAndStatus(contentId, PortalEventAttendanceStatus.NOT_GOING);

        Page<AdminEventAttendanceSummaryResponse.AttendeeItem> page = attendanceRepository
                .findByContentIdWithUser(contentId, pageable)
                .map(a -> AdminEventAttendanceSummaryResponse.AttendeeItem.builder()
                        .userId(a.getUser().getId())
                        .displayName(null) // users aún no cuenta con columna display_name
                        .username(a.getUser().getUsername())
                        .email(a.getUser().getEmail())
                        .status(a.getStatus())
                        .respondedAt(a.getUpdatedAt() != null ? a.getUpdatedAt() : a.getCreatedAt())
                        .build());

        return AdminEventAttendanceSummaryResponse.builder()
                .contentId(contentId)
                .goingCount(goingCount)
                .notGoingCount(notGoingCount)
                .attendees(page)
                .build();
    }
}
