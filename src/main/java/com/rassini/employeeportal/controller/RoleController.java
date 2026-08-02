package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.dto.request.AssignPermissionsRequest;
import com.rassini.employeeportal.dto.request.RoleRequest;
import com.rassini.employeeportal.dto.response.ApiResponse;
import com.rassini.employeeportal.dto.response.PermissionResponse;
import com.rassini.employeeportal.dto.response.RoleResponse;
import com.rassini.employeeportal.service.RoleService;
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
 * Controller REST para gestión de roles.
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Validated
@Tag(name = "Roles", description = "Operaciones CRUD y gestión de permisos para roles")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "Listar roles", description = "Retorna todos los roles registrados")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de roles obtenida exitosamente")
    })
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles() {
        List<RoleResponse> roles = roleService.getRoles();
        return ResponseEntity.ok(ApiResponse.success("Roles obtenidos exitosamente", roles));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener rol por ID", description = "Retorna un rol específico por su ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rol encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable Long id) {
        RoleResponse role = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.success("Rol obtenido exitosamente", role));
    }

    @PostMapping
    @Operation(summary = "Crear rol", description = "Crea un nuevo rol en el sistema")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Rol creado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o código duplicado")
    })
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody RoleRequest request) {
        RoleResponse role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Rol creado exitosamente", role));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar rol", description = "Actualiza un rol existente")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rol actualizado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rol no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o código duplicado")
    })
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(@PathVariable Long id,
                                                                 @Valid @RequestBody RoleRequest request) {
        RoleResponse role = roleService.updateRole(id, request);
        return ResponseEntity.ok(ApiResponse.success("Rol actualizado exitosamente", role));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar rol", description = "Elimina un rol del sistema")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rol eliminado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("Rol eliminado exitosamente"));
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "Obtener permisos del rol", description = "Retorna los permisos asignados a un rol")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permisos obtenidos exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getRolePermissions(@PathVariable Long id) {
        List<PermissionResponse> permissions = roleService.getRolePermissions(id);
        return ResponseEntity.ok(ApiResponse.success("Permisos del rol obtenidos exitosamente", permissions));
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Reemplazar permisos del rol", description = "Reemplaza todos los permisos asignados al rol")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permisos reemplazados exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rol no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "IDs de permiso inválidos")
    })
    public ResponseEntity<ApiResponse<RoleResponse>> replaceRolePermissions(@PathVariable Long id,
                                                                             @Valid @RequestBody AssignPermissionsRequest request) {
        RoleResponse role = roleService.replaceRolePermissions(id, request.getPermissionIds());
        return ResponseEntity.ok(ApiResponse.success("Permisos del rol actualizados exitosamente", role));
    }
}
