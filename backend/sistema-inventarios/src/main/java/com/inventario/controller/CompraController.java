package com.inventario.controller;

import com.inventario.dto.request.CompraCreateRequest;
import com.inventario.dto.response.CompraResponse;
import com.inventario.service.CompraService;
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
 * Controlador REST para la gestión de compras.
 * Proporciona endpoints para obtener, crear, actualizar estado y eliminar compras.
 * Requiere autenticación y autorización según el endpoint.
 */
@RestController
@RequestMapping("/compras")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Compras", description = "Endpoints para gestionar compras del sistema")
public class CompraController {

    private final CompraService compraService;

    /**
     * Obtiene todas las compras.
     * Requiere rol ADMIN.
     *
     * @return Lista de compras con estado 200 OK
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener todas las compras",
            description = "Retorna la lista de todas las compras del sistema. Requiere rol ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de compras obtenida exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<List<CompraResponse>> getAllCompras() {
        log.info("GET /compras - Obteniendo todas las compras");
        List<CompraResponse> compras = compraService.getAllPurchases()
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(compras);
    }

    /**
     * Obtiene una compra específica por su ID.
     *
     * @param id ID de la compra a obtener
     * @return Compra con estado 200 OK o 404 Not Found si no existe
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener compra por ID",
            description = "Retorna los detalles de una compra específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compra obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Compra no encontrada"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<CompraResponse> getCompraById(
            @Parameter(description = "ID de la compra", required = true)
            @PathVariable Integer id) {
        log.info("GET /compras/{} - Obteniendo compra por ID", id);
        CompraResponse compra = mapToResponse(compraService.getPurchaseById(id));
        return ResponseEntity.ok(compra);
    }

    /**
     * Crea una nueva compra.
     * Requiere rol ADMIN.
     *
     * @param request DTO con los datos de la nueva compra
     * @return Compra creada con estado 201 Created
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear nueva compra",
            description = "Crea una nueva compra en el sistema. Requiere rol ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Compra creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<CompraResponse> createCompra(
            @Valid @RequestBody CompraCreateRequest request) {
        log.info("POST /compras - Creando nueva compra para sucursal: {}", request.sucursalId());
        // TODO: Implementar mapeo de CompraCreateRequest a entidad Compra
        // Se deben mapear fechaEsperada y condicionesPago según la lógica de negocio
        throw new UnsupportedOperationException("Crear compra aún no implementado");
    }

    /**
     * Cambia el estado de una compra existente.
     * Requiere rol ADMIN.
     *
     * @param id ID de la compra
     * @param nuevoEstado Nuevo estado para la compra
     * @return Compra actualizada con estado 200 OK
     */
    @PatchMapping("/{id}/cambiar-estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar estado de compra",
            description = "Cambia el estado de una compra existente. Requiere rol ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado cambiado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Compra no encontrada"),
            @ApiResponse(responseCode = "400", description = "Estado inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<CompraResponse> cambiarEstado(
            @Parameter(description = "ID de la compra", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Nuevo estado de la compra", required = true)
            @RequestParam String nuevoEstado) {
        log.info("PATCH /compras/{}/cambiar-estado - Cambiando a estado: {}", id, nuevoEstado);
        try {
            com.inventario.model.enums.EstadoCompra estado =
                com.inventario.model.enums.EstadoCompra.valueOf(nuevoEstado);
            CompraResponse compra = mapToResponse(compraService.updatePurchaseStatus(id, estado));
            return ResponseEntity.ok(compra);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado inválido: " + nuevoEstado);
        }
    }

    /**
     * Marca una compra como recibida.
     * Requiere rol INVENTORY_OPERATOR.
     *
     * @param id ID de la compra a recibir
     * @return Compra actualizada con estado 200 OK
     */
    @PostMapping("/{id}/recibir")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    @Operation(summary = "Recibir compra",
            description = "Marca una compra como recibida y actualiza el inventario. Requiere rol INVENTORY_OPERATOR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compra recibida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Compra no encontrada"),
            @ApiResponse(responseCode = "400", description = "La compra no puede ser recibida en su estado actual"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol INVENTORY_OPERATOR"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<CompraResponse> recibirCompra(
            @Parameter(description = "ID de la compra", required = true)
            @PathVariable Integer id) {
        log.info("POST /compras/{}/recibir - Marcando compra como recibida", id);
        CompraResponse compra = mapToResponse(compraService.receivePurchase(id, new java.util.HashMap<>()));
        return ResponseEntity.ok(compra);
    }

    /**
     * Elimina una compra.
     * Requiere rol ADMIN.
     *
     * @param id ID de la compra a eliminar
     * @return Estado 204 No Content si se elimina exitosamente
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar compra",
            description = "Elimina una compra del sistema. Requiere rol ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Compra eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Compra no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<Void> deleteCompra(
            @Parameter(description = "ID de la compra", required = true)
            @PathVariable Integer id) {
        log.info("DELETE /compras/{} - Eliminando compra", id);
        compraService.cancelPurchase(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mapea una entidad Compra a su DTO de respuesta.
     */
    private CompraResponse mapToResponse(com.inventario.model.Compra compra) {
        return new CompraResponse(
                compra.getIdCompra(),
                compra.getSucursal().getNombre(),
                compra.getProveedor().getNombre(),
                compra.getEstado().name(),
                compra.getTotal(),
                compra.getFecha(),
                compra.getDiasPago(),
                compra.getNotas()
        );
    }
}
