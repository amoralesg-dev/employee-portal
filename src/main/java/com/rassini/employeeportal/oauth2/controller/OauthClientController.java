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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/oauth-clients")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "OAuth2 Clients", description = "Administracion de clientes OAuth2 / OpenID Connect")
public class OauthClientController {

    private final OauthClientService oauthClientService;

    @GetMapping
    @Operation(summary = "Listar clientes OAuth2 registrados")
    public ResponseEntity<List<OauthClientResponse>> getAll() {
        return ResponseEntity.ok(oauthClientService.findAll());
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
