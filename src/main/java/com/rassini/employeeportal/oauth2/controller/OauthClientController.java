package com.rassini.employeeportal.oauth2.controller;

import com.rassini.employeeportal.oauth2.dto.OauthClientRequest;
import com.rassini.employeeportal.oauth2.dto.OauthClientResponse;
import com.rassini.employeeportal.oauth2.service.OauthClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/oauth-clients")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "OAuth2 Clients", description = "Administracion de clientes OAuth2 / OpenID Connect")
public class OauthClientController {

    private final OauthClientService oauthClientService;
    private final com.rassini.employeeportal.oauth2.repository.OauthClientRepository oauthClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('OAUTH2_CLIENT_VIEW', 'OAUTH2_CLIENT_ADMIN')")
    @Operation(summary = "Listar clientes OAuth2 registrados")
    public ResponseEntity<List<OauthClientResponse>> getAll() {
        return ResponseEntity.ok(oauthClientService.findAll());
    }

    @GetMapping("/scopes")
    @PreAuthorize("hasAnyAuthority('OAUTH2_CLIENT_VIEW', 'OAUTH2_CLIENT_ADMIN')")
    @Operation(summary = "Listar scopes OAuth2 activos desde catálogo dinámico")
    public ResponseEntity<List<Map<String, Object>>> getAllowedScopes() {
        return ResponseEntity.ok(jdbcTemplate.queryForList(
                "SELECT id, code, description, enabled FROM oauth_scopes WHERE enabled = 1 ORDER BY code"));
    }

    @PostMapping("/scopes")
    @PreAuthorize("hasAuthority('OAUTH2_CLIENT_ADMIN')")
    @Operation(summary = "Registrar nuevo scope en catálogo dinámico")
    public ResponseEntity<Map<String, Object>> createScope(@RequestBody Map<String, Object> body) {
        String code = body != null && body.get("code") != null ? body.get("code").toString().trim() : "";
        String desc = body != null && body.get("description") != null ? body.get("description").toString().trim() : "";
        if (code.isBlank()) {
            throw new IllegalArgumentException("El código del scope es obligatorio");
        }
        jdbcTemplate.update("INSERT INTO oauth_scopes (code, description, enabled) VALUES (?, ?, 1)", code, desc);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("code", code, "description", desc, "enabled", true));
    }

    @PostMapping("/{id}/rotate-secret")
    @PreAuthorize("hasAuthority('OAUTH2_CLIENT_ADMIN')")
    @Operation(summary = "Generar o rotar Client Secret (retorna el secreto una sola vez)")
    public ResponseEntity<Map<String, String>> rotateSecret(@PathVariable Long id, Authentication auth) {
        var entity = oauthClientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente OAuth2 no encontrado: " + id));
        if (entity.getAuthorizationGrantTypes() == null || !entity.getAuthorizationGrantTypes().contains("client_credentials")) {
            throw new IllegalArgumentException("Los clientes SPA/OIDC son de solo lectura en este módulo");
        }
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        String rawSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        entity.setClientSecret(passwordEncoder.encode(rawSecret));
        entity.setUpdatedAt(LocalDateTime.now());
        oauthClientRepository.save(entity);
        audit(entity.getId(), entity.getClientId(), "ROTATE_SECRET", "Secreto rotado", auth);
        return ResponseEntity.ok(Map.of("clientSecret", rawSecret));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('OAUTH2_CLIENT_ADMIN')")
    @Operation(summary = "Activar o desactivar cliente OAuth2 (sin eliminación física)")
    public ResponseEntity<OauthClientResponse> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body, Authentication auth) {
        var entity = oauthClientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente OAuth2 no encontrado: " + id));
        if (entity.getAuthorizationGrantTypes() == null || !entity.getAuthorizationGrantTypes().contains("client_credentials")) {
            throw new IllegalArgumentException("Los clientes SPA/OIDC son de solo lectura en este módulo");
        }
        boolean enabled = body != null && Boolean.TRUE.equals(body.get("enabled"));
        entity.setEnabled(enabled);
        entity.setUpdatedAt(LocalDateTime.now());
        oauthClientRepository.save(entity);
        audit(entity.getId(), entity.getClientId(), enabled ? "ENABLE" : "DISABLE", "Estado=" + enabled, auth);
        return ResponseEntity.ok(oauthClientService.findById(id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyAuthority('OAUTH2_CLIENT_VIEW', 'OAUTH2_CLIENT_ADMIN')")
    @Operation(summary = "Consultar historial de auditoría del cliente OAuth2")
    public ResponseEntity<List<Map<String, Object>>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(jdbcTemplate.queryForList(
                "SELECT id, oauth_client_id, client_id, action, detail, actor_username, created_at FROM oauth_client_audit WHERE oauth_client_id = ? ORDER BY created_at DESC",
                id));
    }

    private void audit(Long oauthClientId, String clientId, String action, String detail, Authentication auth) {
        String actor = auth != null ? auth.getName() : "SYSTEM";
        jdbcTemplate.update(
                "INSERT INTO oauth_client_audit (oauth_client_id, client_id, action, detail, actor_username, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                oauthClientId, clientId, action, detail, actor, LocalDateTime.now());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cliente OAuth2 por ID")
    public ResponseEntity<OauthClientResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(oauthClientService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Registrar nuevo cliente OAuth2")
    public ResponseEntity<OauthClientResponse> create(@Valid @RequestBody OauthClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(oauthClientService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cliente OAuth2 existente")
    public ResponseEntity<OauthClientResponse> update(@PathVariable Long id, @Valid @RequestBody OauthClientRequest request) {
        return ResponseEntity.ok(oauthClientService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cliente OAuth2")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        oauthClientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
