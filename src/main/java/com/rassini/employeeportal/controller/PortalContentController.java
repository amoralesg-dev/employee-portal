package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.dto.request.PortalContentRequest;
import com.rassini.employeeportal.dto.request.PortalContentStatusRequest;
import com.rassini.employeeportal.dto.response.AdminEventAttendanceSummaryResponse;
import com.rassini.employeeportal.dto.response.ApiResponse;
import com.rassini.employeeportal.dto.response.AudienceOptionsResponse;
import com.rassini.employeeportal.dto.response.PortalContentResponse;
import com.rassini.employeeportal.entity.PortalContentCategory;
import com.rassini.employeeportal.entity.PortalContentStatus;
import com.rassini.employeeportal.entity.PortalContentType;
import com.rassini.employeeportal.security.CustomUserDetails;
import com.rassini.employeeportal.service.PortalContentService;
import com.rassini.employeeportal.service.PortalEventAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portal-content")
@RequiredArgsConstructor
@Validated
@Tag(name = "Contenido del Portal", description = "Administración de contenido dinámico del homepage para Comunicación Interna")
public class PortalContentController {

    private final PortalContentService contentService;
    private final PortalEventAttendanceService attendanceService;

    @GetMapping
    @PreAuthorize("hasAuthority('PORTAL_CONTENT_READ')")
    @Operation(summary = "Listar contenido del portal (paginado)")
    public ResponseEntity<ApiResponse<Page<PortalContentResponse>>> list(
            @RequestParam(required = false) PortalContentType type,
            @RequestParam(required = false) PortalContentStatus status,
            @RequestParam(required = false) PortalContentCategory category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Order.asc("priority"), Sort.Order.desc("id")));
        Page<PortalContentResponse> result = contentService.searchAdmin(type, status, category, q, pageable);
        return ResponseEntity.ok(ApiResponse.success("Contenido obtenido exitosamente", result));
    }

    @GetMapping("/audience-options")
    @PreAuthorize("hasAuthority('PORTAL_CONTENT_READ')")
    @Operation(summary = "Obtener catálogos de Business Units y Roles para audiencia")
    public ResponseEntity<ApiResponse<AudienceOptionsResponse>> getAudienceOptions() {
        return ResponseEntity.ok(ApiResponse.success("Opciones de audiencia obtenidas", contentService.getAudienceOptions()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PORTAL_CONTENT_READ')")
    @Operation(summary = "Obtener contenido por ID")
    public ResponseEntity<ApiResponse<PortalContentResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Contenido obtenido exitosamente", contentService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PORTAL_CONTENT_CREATE')")
    @Operation(summary = "Crear contenido en borrador")
    public ResponseEntity<ApiResponse<PortalContentResponse>> create(
            @Valid @RequestBody PortalContentRequest request,
            Authentication authentication) {
        PortalContentResponse created = contentService.create(request, extractUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Contenido creado exitosamente", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PORTAL_CONTENT_UPDATE')")
    @Operation(summary = "Actualizar contenido existente")
    public ResponseEntity<ApiResponse<PortalContentResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody PortalContentRequest request,
            Authentication authentication) {
        PortalContentResponse updated = contentService.update(id, request, extractUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success("Contenido actualizado exitosamente", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PORTAL_CONTENT_PUBLISH')")
    @Operation(summary = "Cambiar estado de publicación")
    public ResponseEntity<ApiResponse<PortalContentResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody PortalContentStatusRequest request,
            Authentication authentication) {
        PortalContentResponse updated = contentService.updateStatus(id, request.getStatus(), extractUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success("Estado actualizado exitosamente", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PORTAL_CONTENT_DELETE')")
    @Operation(summary = "Eliminar contenido (solo DRAFT o INACTIVE)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        contentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Contenido eliminado exitosamente"));
    }

    @GetMapping("/{id}/attendance")
    @PreAuthorize("hasAuthority('PORTAL_EVENT_ATTENDANCE_READ')")
    @Operation(summary = "Consultar confirmaciones de asistencia (RSVP) de un evento")
    public ResponseEntity<ApiResponse<AdminEventAttendanceSummaryResponse>> getEventAttendance(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.success("Confirmaciones obtenidas exitosamente",
                attendanceService.getAdminAttendance(id, pageable)));
    }

    private Long extractUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getUserEntity().getId();
        }
        return null;
    }
}
