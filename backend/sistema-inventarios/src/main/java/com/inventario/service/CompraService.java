package com.inventario.service;

import com.inventario.exception.InsufficientStockException;
import com.inventario.exception.ResourceNotFoundException;
import com.inventario.model.Compra;
import com.inventario.model.DetalleCompra;
import com.inventario.model.MovimientoInventario;
import com.inventario.model.enums.EstadoCompra;
import com.inventario.model.enums.MotivoMovimiento;
import com.inventario.model.enums.TipoMovimiento;
import com.inventario.repository.CompraRepository;
import com.inventario.repository.DetalleCompraRepository;
import com.inventario.repository.ProveedorRepository;
import com.inventario.repository.SucursalRepository;
import com.inventario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para gestionar compras en el sistema de inventario.
 * Maneja el ciclo completo de compras desde su creación hasta la recepción del stock.
 *
 * Flujo de estados:
 * BORRADOR → ENVIADA → CONFIRMADA → RECIBIDA_COMPLETA/RECIBIDA_PARCIAL → CANCELADA
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompraService {

    private final CompraRepository compraRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final InventarioService inventarioService;
    private final ProveedorRepository proveedorRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Obtiene todas las compras del sistema.
     *
     * @return Lista de todas las compras
     */
    @Transactional(readOnly = true)
    public List<Compra> getAllPurchases() {
        log.debug("Obteniendo todas las compras");
        return compraRepository.findAll();
    }

    /**
     * Obtiene una compra específica por su ID.
     *
     * @param id ID de la compra
     * @return Objeto Compra con los detalles
     * @throws ResourceNotFoundException si la compra no existe
     */
    @Transactional(readOnly = true)
    public Compra getPurchaseById(Integer id) {
        log.debug("Obteniendo compra con ID: {}", id);
        return compraRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Compra no encontrada con ID: {}", id);
                    return new ResourceNotFoundException("Compra no encontrada con ID: " + id);
                });
    }

    /**
     * Crea una nueva compra en estado BORRADOR.
     * Valida que el proveedor y sucursal existan.
     *
     * @param compra Objeto Compra con los datos iniciales
     * @return Compra creada
     * @throws ResourceNotFoundException si proveedor o sucursal no existen
     */
    @Transactional
    public Compra createPurchase(Compra compra) {
        log.info("Iniciando creación de nueva compra - Proveedor ID: {}, Sucursal ID: {}",
                 compra.getProveedor().getIdProveedor(),
                 compra.getSucursal().getIdSucursal());

        // Validar que el proveedor existe
        if (!proveedorRepository.existsById(compra.getProveedor().getIdProveedor())) {
            log.error("Proveedor no encontrado con ID: {}", compra.getProveedor().getIdProveedor());
            throw new ResourceNotFoundException("Proveedor no encontrado");
        }

        // Validar que la sucursal existe
        if (!sucursalRepository.existsById(compra.getSucursal().getIdSucursal())) {
            log.error("Sucursal no encontrada con ID: {}", compra.getSucursal().getIdSucursal());
            throw new ResourceNotFoundException("Sucursal no encontrada");
        }

        // Establecer estado inicial
        compra.setEstado(EstadoCompra.BORRADOR);
        compra.setTotal(BigDecimal.ZERO);

        Compra compraGuardada = compraRepository.save(compra);
        log.info("Compra creada exitosamente - ID: {}, Proveedor: {}, Estado: {}",
                 compraGuardada.getIdCompra(),
                 compra.getProveedor().getIdProveedor(),
                 compraGuardada.getEstado());

        return compraGuardada;
    }

    /**
     * Actualiza el estado de una compra.
     * Valida las transiciones de estado permitidas.
     *
     * @param id ID de la compra
     * @param newStatus Nuevo estado
     * @return Compra actualizada
     * @throws ResourceNotFoundException si la compra no existe
     * @throws IllegalArgumentException si la transición no es válida
     */
    @Transactional
    public Compra updatePurchaseStatus(Integer id, EstadoCompra newStatus) {
        log.info("Actualizando estado de compra ID: {} a estado: {}", id, newStatus);

        Compra compra = getPurchaseById(id);
        EstadoCompra estadoActual = compra.getEstado();

        // Validar transiciones permitidas
        if (!isValidTransition(estadoActual, newStatus)) {
            log.warn("Transición de estado inválida - De: {}, A: {}", estadoActual, newStatus);
            throw new IllegalArgumentException(
                    "Transición no permitida de " + estadoActual + " a " + newStatus);
        }

        compra.setEstado(newStatus);
        Compra compraActualizada = compraRepository.save(compra);

        log.info("Estado de compra actualizado exitosamente - ID: {}, Nuevo estado: {}",
                 id, newStatus);

        return compraActualizada;
    }

    /**
     * Recibe una compra registrando las cantidades recibidas e ingresando el stock al inventario.
     * Valida que las cantidades recibidas sean menores o iguales a las comprometidas.
     * Registra movimientos de inventario con cálculo de costo promedio ponderado.
     *
     * @param id ID de la compra
     * @param receivedQuantities Map de idProducto -> cantidadRecibida
     * @return Compra con estado actualizado a RECIBIDA_COMPLETA o RECIBIDA_PARCIAL
     * @throws ResourceNotFoundException si la compra no existe
     * @throws InsufficientStockException si hay validaciones de cantidad fallidas
     */
    @Transactional
    public Compra receivePurchase(Integer id, java.util.Map<Integer, Integer> receivedQuantities) {
        log.info("Iniciando recepción de compra ID: {}", id);

        Compra compra = getPurchaseById(id);

        if (compra.getEstado() == EstadoCompra.CANCELADA) {
            log.warn("No se puede recibir una compra cancelada - ID: {}", id);
            throw new IllegalArgumentException("No se puede recibir una compra cancelada");
        }

        List<DetalleCompra> detalles = detalleCompraRepository.findByCompraIdCompra(compra.getIdCompra());

        if (detalles.isEmpty()) {
            log.warn("La compra no tiene detalles asociados - ID: {}", id);
            throw new IllegalArgumentException("La compra debe tener al menos un detalle");
        }

        boolean hayFaltantes = false;
        BigDecimal totalRecibido = BigDecimal.ZERO;

        // Procesar cada detalle de compra
        for (DetalleCompra detalle : detalles) {
            Integer productoId = detalle.getProducto().getIdProducto();
            Integer cantidadRecibida = receivedQuantities.getOrDefault(productoId, 0);

            // Validar que no se reciba más de lo comprometido
            if (cantidadRecibida > detalle.getCantidad()) {
                log.warn("Cantidad recibida excede cantidad comprometida - Producto ID: {}, " +
                        "Comprometida: {}, Recibida: {}",
                        productoId, detalle.getCantidad(), cantidadRecibida);
                throw new InsufficientStockException(
                        "Cantidad recibida (" + cantidadRecibida +
                        ") excede cantidad comprometida (" + detalle.getCantidad() + ")");
            }

            if (cantidadRecibida < detalle.getCantidad()) {
                hayFaltantes = true;
            }

            // Registrar movimiento de inventario si hay cantidad recibida
            if (cantidadRecibida > 0) {
                registrarMovimientoCompra(compra, detalle, cantidadRecibida);
                totalRecibido = totalRecibido.add(
                        detalle.getPrecioUnitario()
                                .multiply(BigDecimal.valueOf(cantidadRecibida))
                );
            }

            // Actualizar cantidad recibida en detalle
            detalle.setCantidadRecibida(cantidadRecibida);
            detalleCompraRepository.save(detalle);

            log.debug("Detalle de compra procesado - Producto ID: {}, Cantidad recibida: {}",
                     productoId, cantidadRecibida);
        }

        // Determinar estado final
        EstadoCompra estadoFinal = hayFaltantes ? EstadoCompra.RECIBIDA_PARCIAL : EstadoCompra.RECIBIDA_COMPLETA;
        compra.setEstado(estadoFinal);
        compra.setFechaRecepcion(LocalDateTime.now());

        Compra compraActualizada = compraRepository.save(compra);

        log.info("Compra recibida exitosamente - ID: {}, Estado: {}, Total recibido: {}",
                 compraActualizada.getIdCompra(),
                 estadoFinal,
                 totalRecibido);

        return compraActualizada;
    }

    /**
     * Registra un movimiento de inventario para una compra.
     * Crea un movimiento de tipo INGRESO con motivo COMPRA.
     *
     * @param compra Compra que genera el movimiento
     * @param detalle Detalle de compra
     * @param cantidadRecibida Cantidad que se está recibiendo
     */
    private void registrarMovimientoCompra(Compra compra, DetalleCompra detalle, Integer cantidadRecibida) {
        log.debug("Registrando movimiento de inventario para compra - Producto: {}, Cantidad: {}",
                 detalle.getProducto().getNombre(), cantidadRecibida);

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .producto(detalle.getProducto())
                .sucursal(compra.getSucursal())
                .usuario(compra.getUsuario())
                .tipo(TipoMovimiento.INGRESO)
                .motivo(MotivoMovimiento.COMPRA)
                .cantidad(cantidadRecibida)
                .costoUnitario(detalle.getPrecioUnitario())
                .referenciaId(compra.getIdCompra())
                .referenciaTipo("COMPRA")
                .build();

        inventarioService.registrarMovimiento(movimiento);
    }

    /**
     * Cancela una compra.
     * Solo se puede cancelar si no ha sido completamente recibida.
     *
     * @param id ID de la compra a cancelar
     * @return Compra cancelada
     * @throws ResourceNotFoundException si la compra no existe
     * @throws IllegalArgumentException si la compra ya fue completamente recibida
     */
    @Transactional
    public Compra cancelPurchase(Integer id) {
        log.info("Cancelando compra ID: {}", id);

        Compra compra = getPurchaseById(id);

        if (compra.getEstado() == EstadoCompra.RECIBIDA_COMPLETA) {
            log.warn("No se puede cancelar una compra completamente recibida - ID: {}", id);
            throw new IllegalArgumentException("No se puede cancelar una compra completamente recibida");
        }

        compra.setEstado(EstadoCompra.CANCELADA);
        Compra compraActualizada = compraRepository.save(compra);

        log.info("Compra cancelada exitosamente - ID: {}", id);

        return compraActualizada;
    }

    /**
     * Valida si una transición de estado es permitida.
     *
     * @param estadoActual Estado actual
     * @param estadoNuevo Estado destino
     * @return true si la transición es válida, false en caso contrario
     */
    private boolean isValidTransition(EstadoCompra estadoActual, EstadoCompra estadoNuevo) {
        return switch (estadoActual) {
            case BORRADOR -> estadoNuevo == EstadoCompra.ENVIADA || estadoNuevo == EstadoCompra.CANCELADA;
            case ENVIADA -> estadoNuevo == EstadoCompra.CONFIRMADA || estadoNuevo == EstadoCompra.CANCELADA;
            case CONFIRMADA -> estadoNuevo == EstadoCompra.RECIBIDA_COMPLETA ||
                              estadoNuevo == EstadoCompra.RECIBIDA_PARCIAL ||
                              estadoNuevo == EstadoCompra.CANCELADA;
            case RECIBIDA_COMPLETA, RECIBIDA_PARCIAL, CANCELADA -> false;
        };
    }
}
