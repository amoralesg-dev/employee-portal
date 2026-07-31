package com.rassini.employeeportal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador de índice para verificar que la API está activa.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Index", description = "Estado de la API")
public class IndexController {

    @GetMapping("/index")
    @Operation(summary = "Health check de la API", description = "Retorna un mensaje indicando que Employee Portal API está activa")
    public ResponseEntity<Map<String, String>> index() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "Employee Portal API está activa",
                "version", "v1"
        ));
    }
}
