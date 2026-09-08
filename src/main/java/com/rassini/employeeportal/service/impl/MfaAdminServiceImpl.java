package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.entity.MfaAuditLogEntity;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.repository.MfaAuditLogRepository;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.MfaAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaAdminServiceImpl implements MfaAdminService {

    private final UserRepository userRepository;
    private final MfaAuditLogRepository mfaAuditLogRepository;

    @Override
    @Transactional
    public void enableMfa(Long targetUserId, String adminUsername) {
        UserEntity admin = getAdmin(adminUsername);
        UserEntity target = getTarget(targetUserId);
        
        checkSelfAction(admin, target);

        if (Boolean.TRUE.equals(target.getMfaEnabled()) && target.getMfaSecret() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario ya tiene MFA habilitado y configurado.");
        }

        // Habilitamos, pero si el secret está null se considerará 'Pendiente de configuración' en el frontend
        target.setMfaEnabled(true);
        userRepository.save(target);

        auditAction(admin, target, "ENABLE");
        log.info("MFA habilitado (forzado) por el admin {} al usuario {}", adminUsername, target.getUsername());
    }

    @Override
    @Transactional
    public void disableMfa(Long targetUserId, String adminUsername) {
        UserEntity admin = getAdmin(adminUsername);
        UserEntity target = getTarget(targetUserId);

        checkSelfAction(admin, target);

        // Deshabilitar completamente y borrar el secreto (forzar nuevo enrolamiento)
        target.setMfaEnabled(false);
        target.setMfaSecret(null);
        target.setMfaEnabledAt(null);
        userRepository.save(target);

        auditAction(admin, target, "DISABLE");
        log.info("MFA deshabilitado por el admin {} al usuario {}", adminUsername, target.getUsername());
    }

    @Override
    @Transactional
    public void resetMfa(Long targetUserId, String adminUsername) {
        UserEntity admin = getAdmin(adminUsername);
        UserEntity target = getTarget(targetUserId);

        checkSelfAction(admin, target);

        // Reiniciar (habilitado = true, pero secreto nulo forzando enrolamiento)
        target.setMfaEnabled(true);
        target.setMfaSecret(null);
        target.setMfaEnabledAt(null);
        userRepository.save(target);

        auditAction(admin, target, "RESET");
        log.info("MFA reseteado por el admin {} al usuario {}", adminUsername, target.getUsername());
    }

    @Override
    @Transactional
    public void requireMfa(Long targetUserId, String adminUsername) {
        UserEntity admin = getAdmin(adminUsername);
        UserEntity target = getTarget(targetUserId);

        checkSelfAction(admin, target);

        target.setMfaRequired(true);
        // Si es requerido, habilitarlo automáticamente para forzar setup
        if (!Boolean.TRUE.equals(target.getMfaEnabled())) {
            target.setMfaEnabled(true);
        }
        userRepository.save(target);

        auditAction(admin, target, "REQUIRE");
        log.info("MFA requerido (mandatory) por el admin {} al usuario {}", adminUsername, target.getUsername());
    }

    @Override
    @Transactional
    public void optionalMfa(Long targetUserId, String adminUsername) {
        UserEntity admin = getAdmin(adminUsername);
        UserEntity target = getTarget(targetUserId);

        checkSelfAction(admin, target);

        target.setMfaRequired(false);
        userRepository.save(target);

        auditAction(admin, target, "OPTIONAL");
        log.info("Obligatoriedad de MFA removida por el admin {} al usuario {}", adminUsername, target.getUsername());
    }

    private UserEntity getAdmin(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Administrador no encontrado."));
    }

    private UserEntity getTarget(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private void checkSelfAction(UserEntity admin, UserEntity target) {
        if (admin.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes modificar tu propio estado de MFA desde el panel administrativo.");
        }
    }

    private void auditAction(UserEntity admin, UserEntity target, String action) {
        MfaAuditLogEntity logEntity = MfaAuditLogEntity.builder()
                .admin(admin)
                .targetUser(target)
                .action(action)
                .createdAt(LocalDateTime.now())
                .build();
        mfaAuditLogRepository.save(logEntity);
    }
}
