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
    @Operation(summary = "Login de usuario", description = "Autentica al usuario y retorna un JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
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

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar token", description = "Genera un nuevo JWT de acceso utilizando un refresh token válido")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody com.rassini.employeeportal.dto.request.RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
