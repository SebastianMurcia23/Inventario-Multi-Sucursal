package com.inventario.controller;

import com.inventario.dto.request.SucursalCreateRequest;
import com.inventario.dto.request.SucursalUpdateRequest;
import com.inventario.dto.response.SucursalResponse;
import com.inventario.service.SucursalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de sucursales.
 * Proporciona endpoints para obtener, crear, actualizar y eliminar sucursales.
 * Requiere autenticación y autorización según el endpoint.
 */
@RestController
@RequestMapping("/sucursales")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sucursales", description = "Endpoints para gestionar sucursales del sistema")
public class SucursalController {

    private final SucursalService sucursalService;

    /**
     * Obtiene todas las sucursales activas.
     *
     * @return Lista de sucursales con estado 200 OK
     */
    @GetMapping
    @Operation(summary = "Obtener todas las sucursales",
            description = "Retorna la lista de todas las sucursales activas del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de sucursales obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<List<SucursalResponse>> getAllSucursales() {
        log.info("GET /sucursales - Obteniendo todas las sucursales");
        List<SucursalResponse> sucursales = sucursalService.getAllBranches();
        return ResponseEntity.ok(sucursales);
    }

    /**
     * Obtiene una sucursal específica por su ID.
     *
     * @param id ID de la sucursal a obtener
     * @return Sucursal con estado 200 OK o 404 Not Found si no existe
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener sucursal por ID",
            description = "Retorna los detalles de una sucursal específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sucursal obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Sucursal no encontrada"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<SucursalResponse> getSucursalById(
            @Parameter(description = "ID de la sucursal", required = true)
            @PathVariable Integer id) {
        log.info("GET /sucursales/{} - Obteniendo sucursal por ID", id);
        SucursalResponse sucursal = sucursalService.getBranchById(id);
        return ResponseEntity.ok(sucursal);
    }

    /**
     * Crea una nueva sucursal.
     * Requiere rol ADMIN.
     *
     * @param request DTO con los datos de la nueva sucursal
     * @return Sucursal creada con estado 201 Created
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear nueva sucursal",
            description = "Crea una nueva sucursal en el sistema. Requiere rol ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sucursal creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o sucursal duplicada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<SucursalResponse> createSucursal(
            @Valid @RequestBody SucursalCreateRequest request) {
        log.info("POST /sucursales - Creando nueva sucursal: {}", request.nombre());
        SucursalResponse sucursal = sucursalService.createBranch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sucursal);
    }

    /**
     * Actualiza una sucursal existente.
     * Requiere rol ADMIN.
     *
     * @param id ID de la sucursal a actualizar
     * @param request DTO con los datos actualizados
     * @return Sucursal actualizada con estado 200 OK
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar sucursal",
            description = "Actualiza los datos de una sucursal existente. Requiere rol ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sucursal actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Sucursal no encontrada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<SucursalResponse> updateSucursal(
            @Parameter(description = "ID de la sucursal", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody SucursalUpdateRequest request) {
        log.info("PUT /sucursales/{} - Actualizando sucursal", id);
        SucursalResponse sucursal = sucursalService.updateBranch(id, request);
        return ResponseEntity.ok(sucursal);
    }

    /**
     * Elimina (desactiva) una sucursal.
     * Requiere rol ADMIN.
     *
     * @param id ID de la sucursal a eliminar
     * @return Estado 204 No Content si se elimina exitosamente
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar sucursal",
            description = "Desactiva una sucursal del sistema. Requiere rol ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Sucursal eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Sucursal no encontrada"),
            @ApiResponse(responseCode = "400", description = "No se puede eliminar - contiene usuarios activos"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<Void> deleteSucursal(
            @Parameter(description = "ID de la sucursal", required = true)
            @PathVariable Integer id) {
        log.info("DELETE /sucursales/{} - Eliminando sucursal", id);
        sucursalService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }
}
