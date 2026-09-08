package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.service.MfaAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MfaAdminController {

    private final MfaAdminService mfaAdminService;

    @PostMapping("/{userId}/mfa/enable")
    @PreAuthorize("hasAuthority('USER_MFA_ADMIN')")
    @Operation(summary = "Habilitar MFA a usuario", description = "Habilita el MFA obligando al usuario a enrolarse en el próximo login.")
    public ResponseEntity<Map<String, String>> enableMfa(@PathVariable Long userId, Authentication authentication) {
        mfaAdminService.enableMfa(userId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "MFA habilitado correctamente para el usuario."));
    }

    @PostMapping("/{userId}/mfa/disable")
    @PreAuthorize("hasAuthority('USER_MFA_ADMIN')")
    @Operation(summary = "Deshabilitar MFA a usuario", description = "Deshabilita el MFA y borra el secreto, obligando a nuevo enrolamiento si se vuelve a habilitar.")
    public ResponseEntity<Map<String, String>> disableMfa(@PathVariable Long userId, Authentication authentication) {
        mfaAdminService.disableMfa(userId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "MFA deshabilitado correctamente para el usuario."));
    }

    @PostMapping("/{userId}/mfa/reset")
    @PreAuthorize("hasAuthority('USER_MFA_ADMIN')")
    @Operation(summary = "Reiniciar MFA a usuario", description = "Elimina la configuración actual y obliga a generar y escanear un nuevo QR.")
    public ResponseEntity<Map<String, String>> resetMfa(@PathVariable Long userId, Authentication authentication) {
        mfaAdminService.resetMfa(userId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "MFA reiniciado correctamente para el usuario."));
    }

    @PostMapping("/{userId}/mfa/require")
    @PreAuthorize("hasAuthority('USER_MFA_ADMIN')")
    @Operation(summary = "Requerir MFA a usuario", description = "Marca el MFA como obligatorio, impidiendo que el usuario lo desactive.")
    public ResponseEntity<Map<String, String>> requireMfa(@PathVariable Long userId, Authentication authentication) {
        mfaAdminService.requireMfa(userId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "MFA marcado como obligatorio correctamente para el usuario."));
    }

    @PostMapping("/{userId}/mfa/optional")
    @PreAuthorize("hasAuthority('USER_MFA_ADMIN')")
    @Operation(summary = "Hacer MFA opcional", description = "Quita la obligatoriedad de MFA, permitiendo al usuario desactivarlo voluntariamente.")
    public ResponseEntity<Map<String, String>> optionalMfa(@PathVariable Long userId, Authentication authentication) {
        mfaAdminService.optionalMfa(userId, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Obligatoriedad de MFA removida correctamente."));
    }
}
