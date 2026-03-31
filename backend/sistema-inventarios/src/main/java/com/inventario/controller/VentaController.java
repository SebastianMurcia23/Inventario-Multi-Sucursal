package com.inventario.controller;

import com.inventario.dto.request.VentaCreateRequest;
import com.inventario.dto.response.VentaResponse;
import com.inventario.service.VentaService;
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
 * Controlador REST para la gestión de ventas.
 * Proporciona endpoints para obtener, crear y eliminar ventas.
 * Requiere autenticación y autorización según el endpoint.
 */
@RestController
@RequestMapping("/ventas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ventas", description = "Endpoints para gestionar ventas del sistema")
public class VentaController {

    private final VentaService ventaService;

    /**
     * Obtiene todas las ventas.
     * Accesible para todos los usuarios autenticados.
     *
     * @return Lista de ventas con estado 200 OK
     */
    @GetMapping
    @Operation(summary = "Obtener todas las ventas",
            description = "Retorna la lista de todas las ventas del sistema. Accesible para todos los usuarios")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de ventas obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<List<VentaResponse>> getAllVentas() {
        log.info("GET /ventas - Obteniendo todas las ventas");
        List<VentaResponse> ventas = ventaService.getAllSales()
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(ventas);
    }

    /**
     * Obtiene una venta específica por su ID.
     *
     * @param id ID de la venta a obtener
     * @return Venta con estado 200 OK o 404 Not Found si no existe
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener venta por ID",
            description = "Retorna los detalles de una venta específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Venta obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Venta no encontrada"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<VentaResponse> getVentaById(
            @Parameter(description = "ID de la venta", required = true)
            @PathVariable Integer id) {
        log.info("GET /ventas/{} - Obteniendo venta por ID", id);
        VentaResponse venta = mapToResponse(ventaService.getSaleById(id));
        return ResponseEntity.ok(venta);
    }

    /**
     * Crea una nueva venta.
     * Requiere rol INVENTORY_OPERATOR.
     *
     * @param request DTO con los datos de la nueva venta
     * @return Venta creada con estado 201 Created
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    @Operation(summary = "Crear nueva venta",
            description = "Crea una nueva venta en el sistema. Requiere rol INVENTORY_OPERATOR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Venta creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o stock insuficiente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol INVENTORY_OPERATOR"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<VentaResponse> createVenta(
            @Valid @RequestBody VentaCreateRequest request) {
        log.info("POST /ventas - Creando nueva venta para sucursal: {}", request.sucursalId());
        // TODO: Implementar mapeo de VentaCreateRequest a entidad Venta
        // Se deben mapear los ventaItems a detalles y procesar el inventario
        throw new UnsupportedOperationException("Crear venta aún no implementado");
    }

    /**
     * Elimina una venta.
     * Requiere rol BRANCH_MANAGER.
     *
     * @param id ID de la venta a eliminar
     * @return Estado 204 No Content si se elimina exitosamente
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Eliminar venta",
            description = "Elimina una venta del sistema. Requiere rol BRANCH_MANAGER")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Venta eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Venta no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol BRANCH_MANAGER"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<Void> deleteVenta(
            @Parameter(description = "ID de la venta", required = true)
            @PathVariable Integer id) {
        log.info("DELETE /ventas/{} - Eliminando venta", id);
        ventaService.cancelSale(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mapea una entidad Venta a su DTO de respuesta.
     */
    private VentaResponse mapToResponse(com.inventario.model.Venta venta) {
        return new VentaResponse(
                venta.getIdVenta(),
                venta.getSucursal().getNombre(),
                venta.getEstado().name(),
                venta.getTotal(),
                venta.getFecha(),
                venta.getNotas()
        );
    }
}
