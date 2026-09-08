package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.dto.request.BusinessUnitRequest;
import com.rassini.employeeportal.dto.request.UpdateStatusRequest;
import com.rassini.employeeportal.dto.response.ApiResponse;
import com.rassini.employeeportal.dto.response.BusinessUnitResponse;
import com.rassini.employeeportal.dto.response.UserResponse;
import com.rassini.employeeportal.service.BusinessUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/business-units")
@RequiredArgsConstructor
@Tag(name = "Business Units", description = "Operaciones CRUD y gestión de usuarios para Business Units")
@SecurityRequirement(name = "bearerAuth")
public class BusinessUnitController {

    private final BusinessUnitService businessUnitService;

    @GetMapping
    @PreAuthorize("hasAuthority('BU_VIEW')")
    @Operation(summary = "Obtener todas las Business Units", description = "Retorna la lista completa superficial de Business Units")
    public ResponseEntity<ApiResponse<List<BusinessUnitResponse>>> getAllBusinessUnits() {
        List<BusinessUnitResponse> bus = businessUnitService.getAllBusinessUnits();
        return ResponseEntity.ok(ApiResponse.success("Business Units obtenidas exitosamente", bus));
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('BU_VIEW')")
    @Operation(summary = "Obtener árbol de Business Units", description = "Retorna el árbol jerárquico de Business Units")
    public ResponseEntity<ApiResponse<List<BusinessUnitResponse>>> getBusinessUnitTree() {
        List<BusinessUnitResponse> tree = businessUnitService.getBusinessUnitTree();
        return ResponseEntity.ok(ApiResponse.success("Árbol de Business Units obtenido exitosamente", tree));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BU_VIEW')")
    @Operation(summary = "Obtener Business Unit por ID", description = "Retorna una Business Unit específica por su identificador")
    public ResponseEntity<ApiResponse<BusinessUnitResponse>> getBusinessUnitById(@PathVariable Long id) {
        BusinessUnitResponse bu = businessUnitService.getBusinessUnitById(id);
        return ResponseEntity.ok(ApiResponse.success("Business Unit obtenida exitosamente", bu));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BU_CREATE')")
    @Operation(summary = "Crear Business Unit", description = "Crea una nueva Business Unit en el sistema")
    public ResponseEntity<ApiResponse<BusinessUnitResponse>> createBusinessUnit(@Valid @RequestBody BusinessUnitRequest request) {
        BusinessUnitResponse bu = businessUnitService.createBusinessUnit(request);
        return new ResponseEntity<>(ApiResponse.success(HttpStatus.CREATED.value(), "Business Unit creada exitosamente", bu), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BU_EDIT')")
    @Operation(summary = "Actualizar Business Unit", description = "Modifica los datos de una Business Unit existente")
    public ResponseEntity<ApiResponse<BusinessUnitResponse>> updateBusinessUnit(@PathVariable Long id, @Valid @RequestBody BusinessUnitRequest request) {
        BusinessUnitResponse bu = businessUnitService.updateBusinessUnit(id, request);
        return ResponseEntity.ok(ApiResponse.success("Business Unit actualizada exitosamente", bu));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('BU_EDIT')")
    @Operation(summary = "Actualizar estado de Business Unit", description = "Activa o desactiva de forma lógica una Business Unit")
    public ResponseEntity<ApiResponse<BusinessUnitResponse>> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        BusinessUnitResponse bu = businessUnitService.updateStatus(id, request.getEnabled());
        return ResponseEntity.ok(ApiResponse.success("Estado de Business Unit actualizado exitosamente", bu));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BU_DELETE')")
    @Operation(summary = "Eliminar Business Unit", description = "Elimina físicamente una Business Unit si no tiene usuarios ni hijos")
    public ResponseEntity<ApiResponse<Void>> deleteBusinessUnit(@PathVariable Long id) {
        businessUnitService.deleteBusinessUnit(id);
        return ResponseEntity.ok(ApiResponse.success("Business Unit eliminada exitosamente"));
    }

    @GetMapping("/{id}/users")
    @PreAuthorize("hasAuthority('BU_VIEW')")
    @Operation(summary = "Obtener usuarios asignados a la Business Unit", description = "Retorna los usuarios directamente asociados a la Business Unit")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getBusinessUnitUsers(@PathVariable Long id) {
        List<UserResponse> users = businessUnitService.getBusinessUnitUsers(id);
        return ResponseEntity.ok(ApiResponse.success("Usuarios de la Business Unit obtenidos exitosamente", users));
    }

    @PutMapping("/{id}/users")
    @PreAuthorize("hasAuthority('BU_ASSIGN_USERS')")
    @Operation(summary = "Reemplazar usuarios asignados a la Business Unit", description = "Reemplaza el conjunto completo de usuarios asignados a la Business Unit")
    public ResponseEntity<ApiResponse<Void>> replaceBusinessUnitUsers(@PathVariable Long id, @RequestBody Set<Long> userIds) {
        businessUnitService.replaceBusinessUnitUsers(id, userIds);
        return ResponseEntity.ok(ApiResponse.success("Asignación de usuarios actualizada exitosamente"));
    }
}
