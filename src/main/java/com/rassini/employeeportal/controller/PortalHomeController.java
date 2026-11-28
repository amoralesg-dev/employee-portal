package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.dto.request.EventAttendanceRequest;
import com.rassini.employeeportal.dto.response.ApiResponse;
import com.rassini.employeeportal.dto.response.EventAttendanceResponse;
import com.rassini.employeeportal.dto.response.HomeItemResponse;
import com.rassini.employeeportal.dto.response.HomeResponse;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.security.CustomUserDetails;
import com.rassini.employeeportal.service.PortalEventAttendanceService;
import com.rassini.employeeportal.service.PortalEventCalendarService;
import com.rassini.employeeportal.service.PortalHomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
@Tag(name = "Homepage", description = "Contenido dinámico del homepage filtrado por vigencia, audiencia e idioma")
public class PortalHomeController {

    private final PortalHomeService homeService;
    private final PortalEventCalendarService calendarService;
    private final PortalEventAttendanceService attendanceService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener contenido dinámico del homepage para el usuario autenticado")
    public ResponseEntity<ApiResponse<HomeResponse>> getHome(
            @RequestParam(required = false, defaultValue = "es-MX") String lang,
            Authentication authentication) {
        Long userId = requireUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success("Homepage obtenido exitosamente", homeService.getHome(userId, lang)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener detalle de una pieza visible para el usuario autenticado")
    public ResponseEntity<ApiResponse<HomeItemResponse>> getHomeItemDetail(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "es-MX") String lang,
            Authentication authentication) {
        Long userId = requireUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success("Detalle obtenido exitosamente", homeService.getHomeItemDetail(userId, id, lang)));
    }

    @GetMapping("/{id}/calendar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Descargar archivo .ics para agregar un evento al calendario")
    public ResponseEntity<byte[]> downloadCalendar(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "es-MX") String lang,
            Authentication authentication) {
        Long userId = requireUserId(authentication);
        PortalEventCalendarService.CalendarDownload download = calendarService.generateIcs(userId, id, lang);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "calendar", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(download.filename(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(download.content().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/{id}/attendance")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar la confirmación de asistencia del usuario autenticado para un evento")
    public ResponseEntity<ApiResponse<EventAttendanceResponse>> getMyAttendance(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = requireUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success("Confirmación obtenida exitosamente", attendanceService.getMyAttendance(userId, id)));
    }

    @PutMapping("/{id}/attendance")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar o actualizar la confirmación de asistencia (RSVP) del usuario autenticado")
    public ResponseEntity<ApiResponse<EventAttendanceResponse>> upsertMyAttendance(
            @PathVariable Long id,
            @Valid @RequestBody EventAttendanceRequest request,
            Authentication authentication) {
        Long userId = requireUserId(authentication);
        EventAttendanceResponse response = attendanceService.upsertMyAttendance(userId, id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Respuesta guardada exitosamente", response));
    }

    @DeleteMapping("/{id}/attendance")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Eliminar la confirmación de asistencia del usuario autenticado")
    public ResponseEntity<ApiResponse<Void>> deleteMyAttendance(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = requireUserId(authentication);
        attendanceService.deleteMyAttendance(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Respuesta eliminada exitosamente"));
    }

    private Long requireUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            return cud.getUserEntity().getId();
        }
        throw new BusinessException("No se pudo identificar al usuario autenticado");
    }
}
