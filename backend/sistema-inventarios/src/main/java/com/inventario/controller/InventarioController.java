package com.inventario.controller;

import com.inventario.dto.request.MovimientoInventarioRequest;
import com.inventario.dto.response.InventarioResponse;
import com.inventario.model.MovimientoInventario;
import com.inventario.model.Producto;
import com.inventario.model.Sucursal;
import com.inventario.model.Usuario;
import com.inventario.repository.ProductoRepository;
import com.inventario.repository.SucursalRepository;
import com.inventario.repository.UsuarioRepository;
import com.inventario.service.InventarioService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión del inventario.
 * Proporciona endpoints para consultar inventario y registrar movimientos.
 * Requiere autenticación y autorización según el endpoint.
 */
@RestController
@RequestMapping("/inventario")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventario", description = "Endpoints para consultar y gestionar el inventario")
public class InventarioController {

    private final InventarioService inventarioService;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;

    /**
     * Obtiene el inventario completo de una sucursal.
     *
     * @param sucursalId ID de la sucursal
     * @return Lista de items del inventario con estado 200 OK
     */
    @GetMapping
    @Operation(summary = "Obtener inventario de sucursal",
            description = "Retorna todos los items del inventario de una sucursal específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inventario obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Sucursal no encontrada"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<List<InventarioResponse>> getInventario(
            @Parameter(description = "ID de la sucursal", required = true)
            @RequestParam Integer sucursalId) {
        log.info("GET /inventario?sucursalId={} - Obteniendo inventario de sucursal", sucursalId);
        List<InventarioResponse> inventario = inventarioService.getInventory(sucursalId);
        return ResponseEntity.ok(inventario);
    }

    /**
     * Obtiene un item específico del inventario.
     *
     * @param productoId ID del producto
     * @param sucursalId ID de la sucursal
     * @return Item del inventario con estado 200 OK o 404 Not Found
     */
    @GetMapping("/{productoId}")
    @Operation(summary = "Obtener item de inventario",
            description = "Retorna los detalles de un item específico del inventario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Item de inventario no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<InventarioResponse> getInventarioItem(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable Integer productoId,
            @Parameter(description = "ID de la sucursal", required = true)
            @RequestParam Integer sucursalId) {
        log.info("GET /inventario/{}?sucursalId={} - Obteniendo item de inventario", productoId, sucursalId);
        InventarioResponse inventario = inventarioService.getInventoryItem(sucursalId, productoId);
        return ResponseEntity.ok(inventario);
    }

    /**
     * Registra un movimiento de inventario (ingreso o salida).
     * Requiere rol INVENTORY_OPERATOR.
     *
     * @param request DTO con los datos del movimiento
     * @return Item actualizado con nuevo stock
     */
    @PostMapping("/movimiento")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    @Operation(summary = "Registrar movimiento de inventario",
            description = "Registra un movimiento de ingreso o salida en el inventario. " +
                    "Requiere rol INVENTORY_OPERATOR. Valida stock disponible para salidas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Movimiento registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, producto/sucursal no existen, o stock insuficiente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol INVENTORY_OPERATOR"),
            @ApiResponse(responseCode = "404", description = "Producto, sucursal o usuario no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<InventarioResponse> registrarMovimiento(
            @Valid @RequestBody MovimientoInventarioRequest request) {
        log.info("POST /inventario/movimiento - Registrando movimiento de inventario. " +
                "Tipo: {}, Producto: {}, Sucursal: {}, Cantidad: {}",
                request.tipo(), request.productoId(), request.sucursalId(), request.cantidad());

        // Obtener usuario actual del contexto de seguridad
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByCorreo(userEmail)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado con email: {}", userEmail);
                    return new RuntimeException("Usuario autenticado no encontrado");
                });

        // Obtener producto y sucursal
        Producto producto = productoRepository.findById(request.productoId())
                .orElseThrow(() -> {
                    log.error("Producto no encontrado con ID: {}", request.productoId());
                    return new RuntimeException("Producto no encontrado con ID: " + request.productoId());
                });

        Sucursal sucursal = sucursalRepository.findById(request.sucursalId())
                .orElseThrow(() -> {
                    log.error("Sucursal no encontrada con ID: {}", request.sucursalId());
                    return new RuntimeException("Sucursal no encontrada con ID: " + request.sucursalId());
                });

        // Construir entidad MovimientoInventario desde el request
        MovimientoInventario movimiento = MovimientoInventario.builder()
                .producto(producto)
                .sucursal(sucursal)
                .usuario(usuario)
                .tipo(request.tipo())
                .motivo(request.motivo())
                .cantidad(request.cantidad())
                .costoUnitario(request.costoPorUnidad())
                .build();

        // Registrar movimiento
        inventarioService.registrarMovimiento(movimiento);

        log.info("Movimiento registrado exitosamente - Tipo: {}, Producto: {}, " +
                "Cantidad: {}, Usuario: {}", request.tipo(), producto.getNombre(),
                request.cantidad(), usuario.getCorreo());

        // Obtener y retornar item actualizado
        InventarioResponse inventario = inventarioService.getInventoryItem(
                request.sucursalId(), request.productoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(inventario);
    }
}
