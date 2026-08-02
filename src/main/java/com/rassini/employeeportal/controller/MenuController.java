package com.rassini.employeeportal.controller;

import com.rassini.employeeportal.dto.request.MenuRequest;
import com.rassini.employeeportal.dto.response.ApiResponse;
import com.rassini.employeeportal.dto.response.MenuResponse;
import com.rassini.employeeportal.service.MenuService;
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
 * Controller REST para gestión de menús.
 */
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@Validated
@Tag(name = "Menús", description = "Operaciones CRUD y consulta de árbol jerárquico de menús")
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    @Operation(summary = "Listar menús", description = "Retorna todos los menús registrados (lista plana)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de menús obtenida exitosamente")
    })
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getMenus() {
        List<MenuResponse> menus = menuService.getMenus();
        return ResponseEntity.ok(ApiResponse.success("Menús obtenidos exitosamente", menus));
    }

    @GetMapping("/tree")
    @Operation(summary = "Obtener árbol de menús",
               description = "Retorna el árbol completo de menús con jerarquía padre-hijo, ordenado por order_index")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Árbol de menús obtenido exitosamente")
    })
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getMenuTree() {
        List<MenuResponse> tree = menuService.getMenuTree();
        return ResponseEntity.ok(ApiResponse.success("Árbol de menús obtenido exitosamente", tree));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener menú por ID", description = "Retorna un menú específico por su ID, incluyendo hijos")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Menú encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Menú no encontrado")
    })
    public ResponseEntity<ApiResponse<MenuResponse>> getMenuById(@PathVariable Long id) {
        MenuResponse menu = menuService.getMenuById(id);
        return ResponseEntity.ok(ApiResponse.success("Menú obtenido exitosamente", menu));
    }

    @PostMapping
    @Operation(summary = "Crear menú", description = "Crea un nuevo menú. Usar parentId para asignarlo como hijo de otro menú")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Menú creado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o código duplicado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Menú padre no encontrado")
    })
    public ResponseEntity<ApiResponse<MenuResponse>> createMenu(@Valid @RequestBody MenuRequest request) {
        MenuResponse menu = menuService.createMenu(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Menú creado exitosamente", menu));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar menú", description = "Actualiza un menú existente")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Menú actualizado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Menú no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos, código duplicado o ciclo self-reference")
    })
    public ResponseEntity<ApiResponse<MenuResponse>> updateMenu(@PathVariable Long id,
                                                                 @Valid @RequestBody MenuRequest request) {
        MenuResponse menu = menuService.updateMenu(id, request);
        return ResponseEntity.ok(ApiResponse.success("Menú actualizado exitosamente", menu));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar menú", description = "Elimina un menú del sistema")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Menú eliminado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Menú no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return ResponseEntity.ok(ApiResponse.success("Menú eliminado exitosamente"));
    }
}
