package com.inventario.controller;

import com.inventario.dto.request.TransferenciaCreateRequest;
import com.inventario.dto.response.TransferenciaResponse;
import com.inventario.service.TransferenciaService;
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador REST para la gestión de transferencias entre sucursales.
 * Proporciona endpoints para obtener, crear, aprobar, enviar, recibir y eliminar transferencias.
 * Requiere autenticación y autorización según el endpoint.
 */
@RestController
@RequestMapping("/transferencias")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transferencias", description = "Endpoints para gestionar transferencias entre sucursales")
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    /**
     * Obtiene transferencias filtradas por sucursal del usuario.
     *
     * @param branchId ID de la sucursal para filtrar (opcional)
     * @return Lista de transferencias con estado 200 OK
     */
    @GetMapping
    @Operation(summary = "Obtener transferencias",
            description = "Retorna la lista de transferencias, opcionalmente filtrada por sucursal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de transferencias obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<List<TransferenciaResponse>> getTransferencias(
            @Parameter(description = "ID de la sucursal para filtrar (opcional)")
            @RequestParam(required = false) Integer branchId) {
        log.info("GET /transferencias - Obteniendo transferencias, branchId: {}", branchId);
        List<TransferenciaResponse> transferencias = transferenciaService.getAllTransfers(branchId)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(transferencias);
    }

    /**
     * Obtiene una transferencia específica por su ID.
     *
     * @param id ID de la transferencia a obtener
     * @return Transferencia con estado 200 OK o 404 Not Found si no existe
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener transferencia por ID",
            description = "Retorna los detalles de una transferencia específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transferencia obtenida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Transferencia no encontrada"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<TransferenciaResponse> getTransferenciaById(
            @Parameter(description = "ID de la transferencia", required = true)
            @PathVariable Integer id) {
        log.info("GET /transferencias/{} - Obteniendo transferencia por ID", id);
        TransferenciaResponse transferencia = mapToResponse(transferenciaService.getTransferById(id));
        return ResponseEntity.ok(transferencia);
    }

    /**
     * Crea una nueva transferencia entre sucursales.
     * Requiere rol BRANCH_MANAGER.
     *
     * @param request DTO con los datos de la nueva transferencia
     * @return Transferencia creada con estado 201 Created
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Crear nueva transferencia",
            description = "Crea una nueva transferencia entre sucursales. Requiere rol BRANCH_MANAGER")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transferencia creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o stock insuficiente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol BRANCH_MANAGER"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<TransferenciaResponse> createTransferencia(
            @Valid @RequestBody TransferenciaCreateRequest request) {
        log.info("POST /transferencias - Creando nueva transferencia de sucursal {} a {}",
                request.sucursalOrigenId(), request.sucursalDestinoId());
        // TODO: Implementar mapeo de TransferenciaCreateRequest a entidad Transferencia
        // Se deben mapear los items a detalles de transferencia
        throw new UnsupportedOperationException("Crear transferencia aún no implementado");
    }

    /**
     * Aprueba una transferencia pendiente.
     * Requiere rol BRANCH_MANAGER.
     *
     * @param id ID de la transferencia a aprobar
     * @return Transferencia actualizada con estado 200 OK
     */
    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Aprobar transferencia",
            description = "Aprueba una transferencia pendiente. Requiere rol BRANCH_MANAGER")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transferencia aprobada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Transferencia no encontrada"),
            @ApiResponse(responseCode = "400", description = "La transferencia no puede ser aprobada en su estado actual"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol BRANCH_MANAGER"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<TransferenciaResponse> aprobarTransferencia(
            @Parameter(description = "ID de la transferencia", required = true)
            @PathVariable Integer id) {
        log.info("PATCH /transferencias/{}/aprobar - Aprobando transferencia", id);
        TransferenciaResponse transferencia = mapToResponse(transferenciaService.approveTransfer(id));
        return ResponseEntity.ok(transferencia);
    }

    /**
     * Marca una transferencia como enviada con datos del transportista.
     * Requiere rol BRANCH_MANAGER.
     *
     * @param id ID de la transferencia
     * @param transportista Nombre del transportista
     * @param fechaEstimada Fecha estimada de entrega
     * @return Transferencia actualizada con estado 200 OK
     */
    @PatchMapping("/{id}/enviar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Enviar transferencia",
            description = "Marca una transferencia como enviada y registra datos del transportista. Requiere rol BRANCH_MANAGER")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transferencia enviada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Transferencia no encontrada"),
            @ApiResponse(responseCode = "400", description = "La transferencia no puede ser enviada en su estado actual"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol BRANCH_MANAGER"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<TransferenciaResponse> enviarTransferencia(
            @Parameter(description = "ID de la transferencia", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Nombre del transportista", required = true)
            @RequestParam String transportista,
            @Parameter(description = "Fecha estimada de entrega", required = true)
            @RequestParam LocalDateTime fechaEstimada) {
        log.info("PATCH /transferencias/{}/enviar - Enviando transferencia con transportista: {}", id, transportista);
        TransferenciaResponse transferencia = mapToResponse(transferenciaService.shipTransfer(id, transportista, fechaEstimada));
        return ResponseEntity.ok(transferencia);
    }

    /**
     * Marca una transferencia como recibida en la sucursal destino.
     * Requiere rol INVENTORY_OPERATOR.
     *
     * @param id ID de la transferencia
     * @return Transferencia actualizada con estado 200 OK
     */
    @PatchMapping("/{id}/recibir")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    @Operation(summary = "Recibir transferencia",
            description = "Marca una transferencia como recibida y actualiza el inventario. Requiere rol INVENTORY_OPERATOR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transferencia recibida exitosamente"),
            @ApiResponse(responseCode = "404", description = "Transferencia no encontrada"),
            @ApiResponse(responseCode = "400", description = "La transferencia no puede ser recibida en su estado actual"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol INVENTORY_OPERATOR"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<TransferenciaResponse> recibirTransferencia(
            @Parameter(description = "ID de la transferencia", required = true)
            @PathVariable Integer id) {
        log.info("PATCH /transferencias/{}/recibir - Marcando transferencia como recibida", id);
        TransferenciaResponse transferencia = mapToResponse(transferenciaService.receiveTransfer(id, new java.util.HashMap<>()));
        return ResponseEntity.ok(transferencia);
    }

    /**
     * Elimina una transferencia.
     * Requiere rol BRANCH_MANAGER.
     *
     * @param id ID de la transferencia a eliminar
     * @return Estado 204 No Content si se elimina exitosamente
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Eliminar transferencia",
            description = "Elimina una transferencia del sistema. Requiere rol BRANCH_MANAGER")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transferencia eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Transferencia no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol BRANCH_MANAGER"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<Void> deleteTransferencia(
            @Parameter(description = "ID de la transferencia", required = true)
            @PathVariable Integer id) {
        log.info("DELETE /transferencias/{} - Eliminando transferencia", id);
        transferenciaService.cancelTransfer(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mapea una entidad Transferencia a su DTO de respuesta.
     */
    private TransferenciaResponse mapToResponse(com.inventario.model.Transferencia transferencia) {
        return new TransferenciaResponse(
                transferencia.getIdTransferencia(),
                transferencia.getSucursalOrigen().getNombre(),
                transferencia.getSucursalDestino().getNombre(),
                transferencia.getEstado().name(),
                transferencia.getPrioridad().name(),
                transferencia.getFechaSolicitud(),
                transferencia.getFechaEnvio(),
                transferencia.getFechaRecepcion(),
                transferencia.getNotas()
        );
    }
}
