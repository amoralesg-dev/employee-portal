package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.dto.response.ApiResponse;
import com.rassini.employeeportal.dto.response.PlaceholderResponse;
import com.rassini.employeeportal.service.MenuUrlResolverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/placeholders")
@RequiredArgsConstructor
@Tag(name = "Placeholders", description = "Catálogo de placeholders de contexto soportados para URLs de menús")
public class PlaceholderController {

    private final MenuUrlResolverService menuUrlResolverService;

    @GetMapping
    @Operation(summary = "Listar placeholders soportados", description = "Retorna el catálogo oficial de placeholders disponibles para parámetros de menú")
    public ResponseEntity<ApiResponse<List<PlaceholderResponse>>> getPlaceholders() {
        List<PlaceholderResponse> placeholders = menuUrlResolverService.getAvailablePlaceholders();
        return ResponseEntity.ok(ApiResponse.success("Placeholders obtenidos exitosamente", placeholders));
    }
}
