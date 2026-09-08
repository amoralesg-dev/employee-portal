package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.dto.request.LoginRequest;
import com.rassini.employeeportal.dto.response.LoginResponse;
import com.rassini.employeeportal.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints de autenticación")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login de usuario")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        com.rassini.employeeportal.dto.auth.LoginResult result = authService.login(request);
        if (result.isMfaRequired()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.ACCEPTED).body(result.getPendingResponse());
        }
        return ResponseEntity.ok(result.getSuccessResponse());
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Obtener usuario actual", description = "Retorna los datos y el contexto de acceso del usuario autenticado sin tokens")
    public ResponseEntity<com.rassini.employeeportal.dto.response.MeResponse> getMe(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(authService.getMe(username));
    }

    @PostMapping("/reset-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Resetear contraseña", description = "Cambia la contraseña de un usuario. Requiere autenticación.")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody com.rassini.employeeportal.dto.request.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cambiar contraseña", description = "Permite a un usuario autenticado cambiar su propia contraseña y remueve la bandera de forzar cambio.")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody com.rassini.employeeportal.dto.request.ChangePasswordRequest request, Authentication authentication) {
        String username = authentication.getName();
        authService.changePassword(username, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar token", description = "Genera un nuevo JWT de acceso utilizando un refresh token válido")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody com.rassini.employeeportal.dto.request.RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/mfa/setup")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Configurar MFA", description = "Inicia la configuracion de MFA generando un QR")
    public ResponseEntity<com.rassini.employeeportal.dto.mfa.MfaSetupResponse> setupMfa(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(authService.setupMfa(username));
    }

    @PostMapping("/mfa/activate")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Activar MFA", description = "Verifica el codigo y activa MFA para la cuenta")
    public ResponseEntity<java.util.Map<String, String>> activateMfa(
            @Valid @RequestBody com.rassini.employeeportal.dto.mfa.MfaActivateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        authService.activateMfa(username, request);
        return ResponseEntity.ok(java.util.Map.of("message", "MFA habilitado correctamente"));
    }

    @PostMapping("/mfa/verify")
    @Operation(summary = "Verificar MFA", description = "Verifica el codigo de MFA durante el login y retorna los tokens definitivos")
    public ResponseEntity<LoginResponse> verifyMfa(
            @Valid @RequestBody com.rassini.employeeportal.dto.mfa.MfaVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyMfa(request));
    }

    @PostMapping("/mfa/disable")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Deshabilitar MFA", description = "Deshabilita MFA proporcionando password y codigo actual")
    public ResponseEntity<java.util.Map<String, String>> disableMfa(
            @Valid @RequestBody com.rassini.employeeportal.dto.mfa.MfaDisableRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        authService.disableMfa(username, request);
        return ResponseEntity.ok(java.util.Map.of("message", "MFA deshabilitado correctamente"));
    }
}
