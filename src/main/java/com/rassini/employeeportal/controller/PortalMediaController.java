package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.dto.response.ApiResponse;
import com.rassini.employeeportal.dto.response.PortalMediaResponse;
import com.rassini.employeeportal.security.CustomUserDetails;
import com.rassini.employeeportal.service.PortalMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/portal-media")
@RequiredArgsConstructor
@Tag(name = "Medios del Portal", description = "Carga y descarga autenticada de imágenes del portal")
public class PortalMediaController {

    private final PortalMediaService mediaService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PORTAL_MEDIA_UPLOAD')")
    @Operation(summary = "Subir imagen (JPG, PNG, WebP, máx. 5 MB)")
    public ResponseEntity<ApiResponse<PortalMediaResponse>> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        Long userId = (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails cud)
                ? cud.getUserEntity().getId() : null;
        PortalMediaResponse saved = mediaService.upload(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Imagen subida exitosamente", saved));
    }

    @GetMapping("/{id}/content")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Descargar imagen autenticada")
    public ResponseEntity<Resource> getContent(@PathVariable Long id) {
        PortalMediaService.MediaResource loaded = mediaService.load(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(loaded.metadata().getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
                .body(loaded.resource());
    }
}
