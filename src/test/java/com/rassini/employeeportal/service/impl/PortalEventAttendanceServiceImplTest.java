package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.dto.response.AdminEventAttendanceSummaryResponse;
import com.rassini.employeeportal.dto.response.EventAttendanceResponse;
import com.rassini.employeeportal.entity.*;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.repository.PortalContentRepository;
import com.rassini.employeeportal.repository.PortalEventAttendanceRepository;
import com.rassini.employeeportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortalEventAttendanceServiceImplTest {

    @Mock
    private PortalContentRepository contentRepository;
    @Mock
    private PortalEventAttendanceRepository attendanceRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PortalEventAttendanceServiceImpl service;

    private UserEntity user;
    private PortalContentEntity event;

    @BeforeEach
    void setUp() {
        BusinessUnitEntity bu = BusinessUnitEntity.builder().id(1L).code("CORP").enabled(true).build();
        user = UserEntity.builder().id(10L).username("msantillan").email("msantillan@rassini.com").businessUnits(Set.of(bu)).roles(Set.of()).build();

        event = PortalContentEntity.builder()
                .id(50L)
                .contentType(PortalContentType.EVENT)
                .status(PortalContentStatus.PUBLISHED)
                .eventStart(LocalDateTime.now().plusDays(1))
                .eventEnd(LocalDateTime.now().plusDays(1).plusHours(2))
                .businessUnits(Set.of())
                .roles(Set.of())
                .build();
    }

    @Test
    void upsertMyAttendance_goingAndToggleToNotGoing_shouldUpsertSingleRecord() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(contentRepository.findById(50L)).thenReturn(Optional.of(event));
        when(attendanceRepository.findByPortalContentIdAndUserId(50L, 10L)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventAttendanceResponse res1 = service.upsertMyAttendance(10L, 50L, PortalEventAttendanceStatus.GOING);
        assertThat(res1.getStatus()).isEqualTo(PortalEventAttendanceStatus.GOING);
        assertThat(res1.isCanRespond()).isTrue();

        PortalEventAttendanceEntity existing = PortalEventAttendanceEntity.builder()
                .id(1L).portalContent(event).user(user).status(PortalEventAttendanceStatus.GOING).createdAt(LocalDateTime.now()).build();
        when(attendanceRepository.findByPortalContentIdAndUserId(50L, 10L)).thenReturn(Optional.of(existing));

        EventAttendanceResponse res2 = service.upsertMyAttendance(10L, 50L, PortalEventAttendanceStatus.NOT_GOING);
        assertThat(res2.getStatus()).isEqualTo(PortalEventAttendanceStatus.NOT_GOING);
        assertThat(existing.getStatus()).isEqualTo(PortalEventAttendanceStatus.NOT_GOING);
    }

    @Test
    void upsertMyAttendance_shouldRejectNonEventOutOfAudienceAndNotVisible() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(contentRepository.findById(50L)).thenReturn(Optional.of(event));

        event.setContentType(PortalContentType.NEWS);
        assertThatThrownBy(() -> service.upsertMyAttendance(10L, 50L, PortalEventAttendanceStatus.GOING))
                .isInstanceOf(BusinessException.class);

        event.setContentType(PortalContentType.EVENT);
        event.setBusinessUnits(Set.of(BusinessUnitEntity.builder().id(99L).code("OTHER").enabled(true).build()));
        assertThatThrownBy(() -> service.upsertMyAttendance(10L, 50L, PortalEventAttendanceStatus.GOING))
                .isInstanceOf(ResourceNotFoundException.class);

        event.setBusinessUnits(Set.of());
        event.setStatus(PortalContentStatus.DRAFT);
        assertThatThrownBy(() -> service.upsertMyAttendance(10L, 50L, PortalEventAttendanceStatus.GOING))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getMyAttendance_onPastEventWithExistingResponse_shouldReturnReadOnly() {
        event.setEventStart(LocalDateTime.now().minusDays(2));
        event.setEventEnd(LocalDateTime.now().minusDays(1));
        PortalEventAttendanceEntity existing = PortalEventAttendanceEntity.builder()
                .id(1L).portalContent(event).user(user).status(PortalEventAttendanceStatus.GOING).createdAt(LocalDateTime.now().minusDays(3)).build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(contentRepository.findById(50L)).thenReturn(Optional.of(event));
        when(attendanceRepository.findByPortalContentIdAndUserId(50L, 10L)).thenReturn(Optional.of(existing));

        EventAttendanceResponse res = service.getMyAttendance(10L, 50L);
        assertThat(res.getStatus()).isEqualTo(PortalEventAttendanceStatus.GOING);
        assertThat(res.isCanRespond()).isFalse();
    }

    @Test
    void deleteMyAttendance_shouldDeleteWhenActive() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(contentRepository.findById(50L)).thenReturn(Optional.of(event));

        service.deleteMyAttendance(10L, 50L);
        verify(attendanceRepository).deleteByPortalContentIdAndUserId(50L, 10L);
    }

    @Test
    void getAdminAttendance_shouldReturnCountsAndPaginatedAttendees() {
        PortalEventAttendanceEntity a = PortalEventAttendanceEntity.builder()
                .id(1L).portalContent(event).user(user).status(PortalEventAttendanceStatus.GOING).createdAt(LocalDateTime.now()).build();
        PageRequest pr = PageRequest.of(0, 20);

        when(contentRepository.findById(50L)).thenReturn(Optional.of(event));
        when(attendanceRepository.countByPortalContentIdAndStatus(50L, PortalEventAttendanceStatus.GOING)).thenReturn(5L);
        when(attendanceRepository.countByPortalContentIdAndStatus(50L, PortalEventAttendanceStatus.NOT_GOING)).thenReturn(2L);
        when(attendanceRepository.findByContentIdWithUser(50L, pr)).thenReturn(new PageImpl<>(List.of(a), pr, 1));

        AdminEventAttendanceSummaryResponse res = service.getAdminAttendance(50L, pr);
        assertThat(res.getGoingCount()).isEqualTo(5L);
        assertThat(res.getNotGoingCount()).isEqualTo(2L);
        assertThat(res.getAttendees().getContent()).hasSize(1);
        assertThat(res.getAttendees().getContent().get(0).getUsername()).isEqualTo("msantillan");

        // Non-EVENT rejected
        event.setContentType(PortalContentType.NEWS);
        assertThatThrownBy(() -> service.getAdminAttendance(50L, pr)).isInstanceOf(BusinessException.class);
    }

    @Test
    void edgeCases_nullStatusRoleAudienceAndPastDelete_shouldBehaveCorrectly() {
        assertThatThrownBy(() -> service.upsertMyAttendance(10L, 50L, null))
                .isInstanceOf(BusinessException.class);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(contentRepository.findById(50L)).thenReturn(Optional.of(event));

        // Role mismatch
        event.setRoles(Set.of(RoleEntity.builder().id(99L).code("ADMIN").build()));
        assertThatThrownBy(() -> service.getMyAttendance(10L, 50L)).isInstanceOf(ResourceNotFoundException.class);

        // Past event without existing attendance -> 404 on get, BusinessException on delete
        event.setRoles(Set.of());
        event.setEventStart(LocalDateTime.now().minusDays(2));
        event.setEventEnd(LocalDateTime.now().minusDays(1));
        when(attendanceRepository.findByPortalContentIdAndUserId(50L, 10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMyAttendance(10L, 50L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.deleteMyAttendance(10L, 50L)).isInstanceOf(BusinessException.class);
    }
}
