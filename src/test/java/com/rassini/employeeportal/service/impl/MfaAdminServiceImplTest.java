package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.entity.MfaAuditLogEntity;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.repository.MfaAuditLogRepository;
import com.rassini.employeeportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaAdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MfaAuditLogRepository mfaAuditLogRepository;

    @InjectMocks
    private MfaAdminServiceImpl mfaAdminService;

    private UserEntity admin;
    private UserEntity target;

    @BeforeEach
    void setUp() {
        admin = UserEntity.builder().id(1L).username("admin").mfaEnabled(false).build();
        target = UserEntity.builder().id(2L).username("target").mfaEnabled(false).mfaRequired(false).build();
    }

    @Test
    void enableMfa_Success() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        mfaAdminService.enableMfa(2L, "admin");

        assertTrue(target.getMfaEnabled());
        verify(userRepository).save(target);
        verify(mfaAuditLogRepository).save(any(MfaAuditLogEntity.class));
    }

    @Test
    void enableMfa_AlreadyConfigured_ThrowsBadRequest() {
        target.setMfaEnabled(true);
        target.setMfaSecret("v1:encrypted");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThrows(ResponseStatusException.class, () -> mfaAdminService.enableMfa(2L, "admin"));
    }

    @Test
    void disableMfa_Success() {
        target.setMfaEnabled(true);
        target.setMfaSecret("v1:encrypted");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        mfaAdminService.disableMfa(2L, "admin");

        assertFalse(target.getMfaEnabled());
        assertNull(target.getMfaSecret());
        verify(userRepository).save(target);
        verify(mfaAuditLogRepository).save(any(MfaAuditLogEntity.class));
    }

    @Test
    void resetMfa_Success() {
        target.setMfaEnabled(true);
        target.setMfaSecret("v1:encrypted");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        mfaAdminService.resetMfa(2L, "admin");

        assertTrue(target.getMfaEnabled());
        assertNull(target.getMfaSecret());
        verify(userRepository).save(target);
        verify(mfaAuditLogRepository).save(any(MfaAuditLogEntity.class));
    }

    @Test
    void requireMfa_WhenDisabled_EnablesAndRequires() {
        target.setMfaEnabled(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        mfaAdminService.requireMfa(2L, "admin");

        assertTrue(target.getMfaRequired());
        assertTrue(target.getMfaEnabled());
        verify(userRepository).save(target);
        verify(mfaAuditLogRepository).save(any(MfaAuditLogEntity.class));
    }

    @Test
    void requireMfa_WhenAlreadyEnabled_RequiresMfa() {
        target.setMfaEnabled(true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        mfaAdminService.requireMfa(2L, "admin");

        assertTrue(target.getMfaRequired());
        assertTrue(target.getMfaEnabled());
    }

    @Test
    void optionalMfa_Success() {
        target.setMfaRequired(true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        mfaAdminService.optionalMfa(2L, "admin");

        assertFalse(target.getMfaRequired());
        verify(userRepository).save(target);
        verify(mfaAuditLogRepository).save(any(MfaAuditLogEntity.class));
    }

    @Test
    void selfAction_ThrowsForbidden() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(ResponseStatusException.class, () -> mfaAdminService.enableMfa(1L, "admin"));
    }

    @Test
    void adminNotFound_ThrowsUnauthorized() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> mfaAdminService.enableMfa(2L, "unknown"));
    }

    @Test
    void targetNotFound_ThrowsResourceNotFound() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> mfaAdminService.enableMfa(99L, "admin"));
    }
}
