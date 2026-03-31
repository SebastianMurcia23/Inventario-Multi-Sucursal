package com.inventario.service;

import com.inventario.exception.InsufficientStockException;
import com.inventario.exception.ResourceNotFoundException;
import com.inventario.model.DetalleVenta;
import com.inventario.model.Inventario;
import com.inventario.model.MovimientoInventario;
import com.inventario.model.Venta;
import com.inventario.model.enums.EstadoVenta;
import com.inventario.model.enums.MotivoMovimiento;
import com.inventario.model.enums.TipoMovimiento;
import com.inventario.repository.DetalleVentaRepository;
import com.inventario.repository.InventarioRepository;
import com.inventario.repository.SucursalRepository;
import com.inventario.repository.UsuarioRepository;
import com.inventario.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestionar ventas en el sistema de inventario.
 * Maneja la creación de ventas, validación de stock y registro de movimientos de inventario.
 *
 * Flujo de estados:
 * PENDIENTE → CONFIRMADA | ANULADA
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VentaService {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final InventarioService inventarioService;
    private final InventarioRepository inventarioRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Obtiene todas las ventas del sistema.
     *
     * @return Lista de todas las ventas
     */
    @Transactional(readOnly = true)
    public List<Venta> getAllSales() {
        log.debug("Obteniendo todas las ventas");
        return ventaRepository.findAll();
    }

    /**
     * Obtiene una venta específica por su ID.
     *
     * @param id ID de la venta
     * @return Objeto Venta con los detalles
     * @throws ResourceNotFoundException si la venta no existe
     */
    @Transactional(readOnly = true)
    public Venta getSaleById(Integer id) {
        log.debug("Obteniendo venta con ID: {}", id);
        return ventaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Venta no encontrada con ID: {}", id);
                    return new ResourceNotFoundException("Venta no encontrada con ID: " + id);
                });
    }

    /**
     * Crea una nueva venta con sus detalles.
     * Valida que haya stock disponible para todos los items.
     * Registra movimientos de inventario y descuenta el stock.
     * Usa @Transactional para garantizar consistencia.
     *
     * @param venta Objeto Venta con detalles incluidos
     * @param detalles Lista de DetalleVenta con los items de la venta
     * @return Venta creada y confirmada
     * @throws ResourceNotFoundException si sucursal o usuario no existen
     * @throws InsufficientStockException si no hay stock para algún item
     * @throws IllegalArgumentException si la venta no tiene detalles
     */
    @Transactional
    public Venta createSale(Venta venta, List<DetalleVenta> detalles) {
        log.info("Iniciando creación de nueva venta - Sucursal ID: {}, Usuario ID: {}, Items: {}",
                 venta.getSucursal().getIdSucursal(),
                 venta.getUsuario().getIdUsuario(),
                 detalles.size());

        // Validaciones iniciales
        if (detalles == null || detalles.isEmpty()) {
            log.warn("Intento de crear venta sin detalles");
            throw new IllegalArgumentException("La venta debe contener al menos un item");
        }

        if (!sucursalRepository.existsById(venta.getSucursal().getIdSucursal())) {
            log.error("Sucursal no encontrada con ID: {}", venta.getSucursal().getIdSucursal());
            throw new ResourceNotFoundException("Sucursal no encontrada");
        }

        if (!usuarioRepository.existsById(venta.getUsuario().getIdUsuario())) {
            log.error("Usuario no encontrado con ID: {}", venta.getUsuario().getIdUsuario());
            throw new ResourceNotFoundException("Usuario no encontrado");
        }

        // Validar stock disponible para todos los items
        log.debug("Validando disponibilidad de stock para {} items", detalles.size());
        validarStockDisponible(venta.getSucursal().getIdSucursal(), detalles);

        // Guardar venta
        venta.setEstado(EstadoVenta.PENDIENTE);
        Venta ventaGuardada = ventaRepository.save(venta);

        log.debug("Venta guardada - ID: {}, Estado: {}", ventaGuardada.getIdVenta(), ventaGuardada.getEstado());

        // Procesar detalles y descontar stock
        BigDecimal subtotal = BigDecimal.ZERO;
        List<DetalleVenta> detallesGuardados = new ArrayList<>();

        for (DetalleVenta detalle : detalles) {
            detalle.setVenta(ventaGuardada);

            // Calcular total de línea
            BigDecimal totalLinea = detalle.getPrecio()
                    .multiply(BigDecimal.valueOf(detalle.getCantidad()));

            if (detalle.getDescuento() != null && detalle.getDescuento().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal descuentoValor = totalLinea
                        .multiply(detalle.getDescuento())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                totalLinea = totalLinea.subtract(descuentoValor);
            }

            detalle.setTotalLinea(totalLinea);
            DetalleVenta detalleGuardado = detalleVentaRepository.save(detalle);
            detallesGuardados.add(detalleGuardado);

            // Registrar movimiento de inventario (SALIDA)
            registrarMovimientoVenta(ventaGuardada, detalle);

            subtotal = subtotal.add(totalLinea);

            log.debug("Detalle de venta procesado - Producto ID: {}, Cantidad: {}, Total línea: {}",
                     detalle.getProducto().getIdProducto(),
                     detalle.getCantidad(),
                     totalLinea);
        }

        // Actualizar totales de la venta
        BigDecimal total = subtotal;
        if (venta.getDescuentoPct() != null && venta.getDescuentoPct().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal descuentoTotal = subtotal
                    .multiply(venta.getDescuentoPct())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            total = subtotal.subtract(descuentoTotal);
        }

        ventaGuardada.setSubtotal(subtotal);
        ventaGuardada.setTotal(total);
        ventaGuardada.setEstado(EstadoVenta.CONFIRMADA);

        Venta ventaFinal = ventaRepository.save(ventaGuardada);

        log.info("Venta creada exitosamente - ID: {}, Estado: {}, Total: {}, Items procesados: {}",
                 ventaFinal.getIdVenta(),
                 ventaFinal.getEstado(),
                 ventaFinal.getTotal(),
                 detallesGuardados.size());

        return ventaFinal;
    }

    /**
     * Cancela una venta anulada.
     * Si la venta fue confirmada, revierte los movimientos de inventario.
     *
     * @param id ID de la venta a cancelar
     * @return Venta cancelada
     * @throws ResourceNotFoundException si la venta no existe
     * @throws IllegalArgumentException si la venta ya fue cancelada
     */
    @Transactional
    public Venta cancelSale(Integer id) {
        log.info("Cancelando venta ID: {}", id);

        Venta venta = getSaleById(id);

        if (venta.getEstado() == EstadoVenta.ANULADA) {
            log.warn("La venta ya fue cancelada - ID: {}", id);
            throw new IllegalArgumentException("La venta ya ha sido cancelada");
        }

        // Si la venta fue confirmada, revertir movimientos de inventario
        if (venta.getEstado() == EstadoVenta.CONFIRMADA) {
            log.debug("Revirtiendo movimientos de inventario para venta ID: {}", id);
            revertirMovimientosVenta(id);
        }

        venta.setEstado(EstadoVenta.ANULADA);
        Venta ventaActualizada = ventaRepository.save(venta);

        log.info("Venta cancelada exitosamente - ID: {}", id);

        return ventaActualizada;
    }

    /**
     * Valida que haya stock disponible en la sucursal para todos los items de la venta.
     *
     * @param sucursalId ID de la sucursal
     * @param detalles Lista de detalles de venta
     * @throws InsufficientStockException si no hay stock para algún item
     */
    private void validarStockDisponible(Integer sucursalId, List<DetalleVenta> detalles) {
        log.debug("Validando stock disponible en sucursal ID: {}", sucursalId);

        for (DetalleVenta detalle : detalles) {
            Integer productoId = detalle.getProducto().getIdProducto();
            Integer cantidadSolicitada = detalle.getCantidad();

            Inventario inventario = inventarioRepository
                    .findBySucursalIdSucursalAndProductoIdProducto(sucursalId, productoId)
                    .orElse(null);

            int stockDisponible = inventario != null ? inventario.getStockActual() : 0;

            if (stockDisponible < cantidadSolicitada) {
                log.warn("Stock insuficiente - Producto ID: {}, Disponible: {}, Solicitado: {}",
                         productoId, stockDisponible, cantidadSolicitada);
                throw new InsufficientStockException(
                        "Stock insuficiente para producto ID " + productoId +
                        ". Disponible: " + stockDisponible + ", Solicitado: " + cantidadSolicitada);
            }

            log.debug("Stock validado - Producto ID: {}, Disponible: {}", productoId, stockDisponible);
        }
    }

    /**
     * Registra un movimiento de inventario para una venta.
     * Crea un movimiento de tipo SALIDA con motivo VENTA.
     *
     * @param venta Venta que genera el movimiento
     * @param detalle Detalle de venta
     */
    private void registrarMovimientoVenta(Venta venta, DetalleVenta detalle) {
        log.debug("Registrando movimiento de inventario para venta - Producto: {}, Cantidad: {}",
                 detalle.getProducto().getNombre(), detalle.getCantidad());

        // Obtener el costo promedio del inventario para la salida
        Inventario inventario = inventarioRepository
                .findBySucursalIdSucursalAndProductoIdProducto(
                        venta.getSucursal().getIdSucursal(),
                        detalle.getProducto().getIdProducto())
                .orElse(null);

        BigDecimal costoUnitario = inventario != null ?
                inventario.getCostoPromedio() : detalle.getPrecio();

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .producto(detalle.getProducto())
                .sucursal(venta.getSucursal())
                .usuario(venta.getUsuario())
                .tipo(TipoMovimiento.SALIDA)
                .motivo(MotivoMovimiento.VENTA)
                .cantidad(detalle.getCantidad())
                .costoUnitario(costoUnitario)
                .referenciaId(venta.getIdVenta())
                .referenciaTipo("VENTA")
                .build();

        inventarioService.registrarMovimiento(movimiento);
    }

    /**
     * Revierte los movimientos de inventario de una venta cancelada.
     * Registra ingresos por el mismo monto que se retiraron.
     *
     * @param ventaId ID de la venta a revertir
     */
    private void revertirMovimientosVenta(Integer ventaId) {
        log.debug("Revirtiendo movimientos de venta ID: {}", ventaId);

        Venta venta = getSaleById(ventaId);
        List<DetalleVenta> detalles = detalleVentaRepository.findByVentaIdVenta(venta.getIdVenta());

        for (DetalleVenta detalle : detalles) {
            // Obtener el costo promedio para registrar el ingreso
            Inventario inventario = inventarioRepository
                    .findBySucursalIdSucursalAndProductoIdProducto(
                            venta.getSucursal().getIdSucursal(),
                            detalle.getProducto().getIdProducto())
                    .orElse(null);

            BigDecimal costoUnitario = inventario != null ?
                    inventario.getCostoPromedio() : detalle.getPrecio();

            MovimientoInventario movimientoReversa = MovimientoInventario.builder()
                    .producto(detalle.getProducto())
                    .sucursal(venta.getSucursal())
                    .usuario(venta.getUsuario())
                    .tipo(TipoMovimiento.INGRESO)
                    .motivo(MotivoMovimiento.DEVOLUCION)
                    .cantidad(detalle.getCantidad())
                    .costoUnitario(costoUnitario)
                    .referenciaId(ventaId)
                    .referenciaTipo("VENTA_CANCELADA")
                    .build();

            inventarioService.registrarMovimiento(movimientoReversa);

            log.debug("Movimiento revertido - Producto ID: {}, Cantidad: {}",
                     detalle.getProducto().getIdProducto(),
                     detalle.getCantidad());
        }

        log.info("Movimientos de venta revirtidos - Venta ID: {}", ventaId);
    }
}
