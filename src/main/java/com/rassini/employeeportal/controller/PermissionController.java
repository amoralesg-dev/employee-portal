package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.dto.request.AssignMenusRequest;
import com.rassini.employeeportal.dto.request.PermissionRequest;
import com.rassini.employeeportal.dto.response.ApiResponse;
import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.dto.response.PermissionResponse;
import com.rassini.employeeportal.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para gestión de permisos.
 */
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Validated
@Tag(name = "Permisos", description = "Operaciones CRUD y gestión de menús para permisos")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @Operation(summary = "Listar permisos", description = "Retorna todos los permisos registrados")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de permisos obtenida exitosamente")
    })
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissions() {
        List<PermissionResponse> permissions = permissionService.getPermissions();
        return ResponseEntity.ok(ApiResponse.success("Permisos obtenidos exitosamente", permissions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener permiso por ID", description = "Retorna un permiso específico por su ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permiso encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Permiso no encontrado")
    })
    public ResponseEntity<ApiResponse<PermissionResponse>> getPermissionById(@PathVariable Long id) {
        PermissionResponse permission = permissionService.getPermissionById(id);
        return ResponseEntity.ok(ApiResponse.success("Permiso obtenido exitosamente", permission));
    }

    @PostMapping
    @Operation(summary = "Crear permiso", description = "Crea un nuevo permiso en el sistema")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Permiso creado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o código duplicado")
    })
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(@Valid @RequestBody PermissionRequest request) {
        PermissionResponse permission = permissionService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Permiso creado exitosamente", permission));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar permiso", description = "Actualiza un permiso existente")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permiso actualizado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Permiso no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o código duplicado")
    })
    public ResponseEntity<ApiResponse<PermissionResponse>> updatePermission(@PathVariable Long id,
                                                                             @Valid @RequestBody PermissionRequest request) {
        PermissionResponse permission = permissionService.updatePermission(id, request);
        return ResponseEntity.ok(ApiResponse.success("Permiso actualizado exitosamente", permission));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar permiso", description = "Elimina un permiso del sistema")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permiso eliminado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Permiso no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.ok(ApiResponse.success("Permiso eliminado exitosamente"));
    }

    @GetMapping("/{id}/menus")
    @Operation(summary = "Obtener menús del permiso", description = "Retorna los menús asignados a un permiso")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Menús obtenidos exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Permiso no encontrado")
    })
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getPermissionMenus(@PathVariable Long id) {
        List<MenuResponse> menus = permissionService.getPermissionMenus(id);
        return ResponseEntity.ok(ApiResponse.success("Menús del permiso obtenidos exitosamente", menus));
    }

    @PutMapping("/{id}/menus")
    @Operation(summary = "Reemplazar menús del permiso", description = "Reemplaza todos los menús asignados al permiso")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Menús reemplazados exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Permiso no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "IDs de menú inválidos")
    })
    public ResponseEntity<ApiResponse<PermissionResponse>> replacePermissionMenus(@PathVariable Long id,
                                                                                    @Valid @RequestBody AssignMenusRequest request) {
        PermissionResponse permission = permissionService.replacePermissionMenus(id, request.getMenuIds());
        return ResponseEntity.ok(ApiResponse.success("Menús del permiso actualizados exitosamente", permission));
    }
}
