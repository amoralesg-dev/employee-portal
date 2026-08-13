package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.dto.request.AssignRolesRequest;
import com.rassini.employeeportal.dto.request.UpdateStatusRequest;
import com.rassini.employeeportal.dto.request.UserRequest;
import com.rassini.employeeportal.dto.request.UserUpdateRequest;
import com.rassini.employeeportal.dto.response.ApiResponse;
import com.rassini.employeeportal.dto.response.BusinessUnitResponse;
import com.rassini.employeeportal.dto.request.AssignBusinessUnitsRequest;
import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.dto.response.RoleResponse;
import com.rassini.employeeportal.dto.response.UserAccessContextResponse;
import com.rassini.employeeportal.dto.response.UserResponse;
import com.rassini.employeeportal.service.AccessContextService;
import com.rassini.employeeportal.service.UserService;
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
 * Controller REST para gestión de usuarios.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Usuarios", description = "Operaciones CRUD y gestión de roles para usuarios")
public class UserController {

    private final UserService userService;
    private final AccessContextService accessContextService;

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Retorna todos los usuarios registrados")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente")
    })
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers() {
        List<UserResponse> users = userService.getUsers();
        return ResponseEntity.ok(ApiResponse.success("Usuarios obtenidos exitosamente", users));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID", description = "Retorna un usuario específico por su ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("Usuario obtenido exitosamente", user));
    }

    @PostMapping
    @Operation(summary = "Crear usuario", description = "Crea un nuevo usuario en el sistema")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o usuario/email duplicado")
    })
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Usuario creado exitosamente", user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario", description = "Actualiza username y/o email de un usuario existente")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o username/email duplicado")
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long id,
                                                                 @Valid @RequestBody UserUpdateRequest request) {
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("Usuario actualizado exitosamente", user));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Actualizar estado de usuario", description = "Activa o desactiva un usuario")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(@PathVariable Long id,
                                                                   @Valid @RequestBody UpdateStatusRequest request) {
        UserResponse user = userService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Estado del usuario actualizado exitosamente", user));
    }

    @GetMapping("/{id}/roles")
    @Operation(summary = "Obtener roles del usuario", description = "Retorna los roles asignados a un usuario")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Roles obtenidos exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getUserRoles(@PathVariable Long id) {
        List<RoleResponse> roles = userService.getUserRoles(id);
        return ResponseEntity.ok(ApiResponse.success("Roles del usuario obtenidos exitosamente", roles));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Reemplazar roles del usuario", description = "Reemplaza todos los roles asignados al usuario")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Roles reemplazados exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "IDs de rol inválidos")
    })
    public ResponseEntity<ApiResponse<UserResponse>> replaceUserRoles(@PathVariable Long id,
                                                                       @Valid @RequestBody AssignRolesRequest request) {
        UserResponse user = userService.replaceUserRoles(id, request.getRoleIds());
        return ResponseEntity.ok(ApiResponse.success("Roles del usuario actualizados exitosamente", user));
    }

    @GetMapping("/{id}/business-units")
    public ResponseEntity<ApiResponse<List<BusinessUnitResponse>>> getUserBusinessUnits(@PathVariable Long id) {
        List<BusinessUnitResponse> businessUnits = userService.getUserBusinessUnits(id);
        return ResponseEntity.ok(ApiResponse.success("Unidades de negocio obtenidas exitosamente", businessUnits));
    }

    @PutMapping("/{id}/business-units")
    public ResponseEntity<ApiResponse<UserResponse>> replaceUserBusinessUnits(@PathVariable Long id,
                                                                       @Valid @RequestBody AssignBusinessUnitsRequest request) {
        UserResponse user = userService.replaceUserBusinessUnits(id, request.getBusinessUnitIds());
        return ResponseEntity.ok(ApiResponse.success("Unidades de negocio actualizadas exitosamente", user));
    }

    @GetMapping("/{id}/menus")
    @Operation(summary = "Obtener menús autorizados del usuario",
               description = "Retorna el árbol de menús a los que el usuario tiene acceso a través de sus roles y permisos")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Menús obtenidos exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getUserMenus(@PathVariable Long id) {
        UserAccessContextResponse context = accessContextService.getAccessContext(id);
        return ResponseEntity.ok(ApiResponse.success("Menús del usuario obtenidos exitosamente", context.getMenus()));
    }

    @GetMapping("/{id}/access-context")
    @Operation(summary = "Obtener contexto de acceso del usuario",
               description = "Retorna roles, permisos y menús consolidados del usuario en una sola llamada")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Contexto de acceso obtenido exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<ApiResponse<UserAccessContextResponse>> getAccessContext(@PathVariable Long id) {
        UserAccessContextResponse context = accessContextService.getAccessContext(id);
        return ResponseEntity.ok(ApiResponse.success("Contexto de acceso obtenido exitosamente", context));
    }

    @PostMapping("/{id}/change-password")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cambiar contraseña del usuario",
               description = "Permite cambiar la contraseña del usuario indicando la contraseña actual y la nueva")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Contraseña actualizada exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos (contraseñas no coinciden, incorrecta, etc.)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<com.rassini.employeeportal.dto.response.SimpleMessageResponse> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody com.rassini.employeeportal.dto.request.ChangePasswordRequest request) {
        com.rassini.employeeportal.dto.response.SimpleMessageResponse response = userService.changePassword(id, request);
        return ResponseEntity.ok(response);
    }
}
