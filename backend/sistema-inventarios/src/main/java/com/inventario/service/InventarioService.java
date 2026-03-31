package com.inventario.service;

import com.inventario.dto.response.InventarioResponse;
import com.inventario.exception.InsufficientStockException;
import com.inventario.model.Inventario;
import com.inventario.model.MovimientoInventario;
import com.inventario.model.Sucursal;
import com.inventario.model.Producto;
import com.inventario.model.enums.TipoMovimiento;
import com.inventario.repository.InventarioRepository;
import com.inventario.repository.MovimientoInventarioRepository;
import com.inventario.repository.SucursalRepository;
import com.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar el inventario del sistema.
 * Maneja movimientos de inventario, cálculo de costos promedio ponderado y validaciones de stock.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final SucursalRepository sucursalRepository;
    private final ProductoRepository productoRepository;

    /**
     * Obtiene todo el inventario de una sucursal específica.
     *
     * @param branchId ID de la sucursal
     * @return Lista de InventarioResponse con todos los items del inventario
     * @throws RuntimeException si la sucursal no existe
     */
    @Transactional(readOnly = true)
    public List<InventarioResponse> getInventory(Integer branchId) {
        log.debug("Obteniendo inventario completo de sucursal ID: {}", branchId);

        // Validar que la sucursal existe
        sucursalRepository.findById(branchId)
                .orElseThrow(() -> {
                    log.warn("Sucursal no encontrada con ID: {}", branchId);
                    return new RuntimeException("Sucursal no encontrada con ID: " + branchId);
                });

        List<Inventario> items = inventarioRepository.findBySucursalIdSucursal(branchId);

        log.info("Se obtuvieron {} items de inventario para sucursal ID: {}",
                 items.size(), branchId);

        return items.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un item específico del inventario.
     *
     * @param branchId ID de la sucursal
     * @param productId ID del producto
     * @return InventarioResponse con los datos del item
     * @throws RuntimeException si el item no existe
     */
    @Transactional(readOnly = true)
    public InventarioResponse getInventoryItem(Integer branchId, Integer productId) {
        log.debug("Obteniendo item de inventario - Sucursal ID: {}, Producto ID: {}",
                  branchId, productId);

        Inventario inventario = inventarioRepository
                .findBySucursalIdSucursalAndProductoIdProducto(branchId, productId)
                .orElseThrow(() -> {
                    log.warn("Item de inventario no encontrado - Sucursal ID: {}, Producto ID: {}",
                             branchId, productId);
                    return new RuntimeException("Item de inventario no encontrado");
                });

        log.info("Item de inventario obtenido - Sucursal ID: {}, Producto: {}",
                 branchId, inventario.getProducto().getNombre());

        return toResponse(inventario);
    }

    /**
     * Registra un movimiento de inventario (ingreso o salida).
     * Valida stock disponible para salidas, calcula costo promedio ponderado e
     * actualiza el inventario de forma transaccional.
     *
     * Fórmula de costo promedio ponderado:
     * costo_nuevo = (stock_actual * costo_anterior + cantidad * costo_unitario) / (stock_actual + cantidad)
     *
     * @param movimiento MovimientoInventario con los datos del movimiento
     * @throws InsufficientStockException si no hay stock disponible para retirar
     * @throws RuntimeException si sucursal, producto o usuario no existen
     */
    @Transactional
    public void registrarMovimiento(MovimientoInventario movimiento) {
        log.info("Iniciando registro de movimiento de inventario - Tipo: {}, Cantidad: {}, Motivo: {}",
                 movimiento.getTipo(), movimiento.getCantidad(), movimiento.getMotivo());

        // Validaciones básicas
        Sucursal sucursal = sucursalRepository.findById(movimiento.getSucursal().getIdSucursal())
                .orElseThrow(() -> {
                    log.error("Sucursal no encontrada con ID: {}",
                              movimiento.getSucursal().getIdSucursal());
                    return new RuntimeException("Sucursal no encontrada");
                });

        Producto producto = productoRepository.findById(movimiento.getProducto().getIdProducto())
                .orElseThrow(() -> {
                    log.error("Producto no encontrado con ID: {}",
                              movimiento.getProducto().getIdProducto());
                    return new RuntimeException("Producto no encontrado");
                });

        log.debug("Validaciones básicas completadas para movimiento: {} - {}",
                  producto.getNombre(), movimiento.getCantidad());

        // Obtener o crear item de inventario
        Inventario inventario = inventarioRepository
                .findBySucursalIdSucursalAndProductoIdProducto(
                        sucursal.getIdSucursal(),
                        producto.getIdProducto())
                .orElseGet(() -> {
                    log.debug("Creando nuevo item de inventario para Producto: {}, Sucursal: {}",
                              producto.getNombre(), sucursal.getNombre());
                    return Inventario.builder()
                            .producto(producto)
                            .sucursal(sucursal)
                            .stockActual(0)
                            .costoPromedio(BigDecimal.ZERO)
                            .build();
                });

        // Validar stock para retiros
        if (TipoMovimiento.SALIDA == movimiento.getTipo()) {
            if (inventario.getStockActual() < movimiento.getCantidad()) {
                log.warn("Stock insuficiente para retiro - Disponible: {}, Solicitado: {}",
                         inventario.getStockActual(), movimiento.getCantidad());
                throw new InsufficientStockException(
                        "Stock insuficiente. Disponible: " + inventario.getStockActual() +
                        ", Solicitado: " + movimiento.getCantidad());
            }
        }

        // Calcular nuevo costo promedio ponderado
        BigDecimal nuevoCosoPromedio = calcularCostoPromedioPonderado(
                inventario.getStockActual(),
                inventario.getCostoPromedio(),
                movimiento.getCantidad(),
                movimiento.getCostoUnitario(),
                movimiento.getTipo());

        log.debug("Costo promedio calculado - Anterior: {}, Nuevo: {}",
                  inventario.getCostoPromedio(), nuevoCosoPromedio);

        // Actualizar stock e inventario
        Integer stockAnterior = inventario.getStockActual();

        if (TipoMovimiento.INGRESO == movimiento.getTipo()) {
            inventario.setStockActual(inventario.getStockActual() + movimiento.getCantidad());
        } else {
            inventario.setStockActual(inventario.getStockActual() - movimiento.getCantidad());
        }

        inventario.setCostoPromedio(nuevoCosoPromedio);
        Inventario inventarioActualizado = inventarioRepository.save(inventario);

        log.debug("Inventario actualizado - Stock anterior: {}, Stock nuevo: {}, Costo: {}",
                  stockAnterior, inventarioActualizado.getStockActual(), nuevoCosoPromedio);

        // Vincular movimiento al inventario
        movimiento.setInventario(inventarioActualizado);
        MovimientoInventario movimientoGuardado = movimientoInventarioRepository.save(movimiento);

        log.info("Movimiento registrado exitosamente - ID: {}, Producto: {}, Tipo: {}, Cantidad: {}, Costo: {}",
                 movimientoGuardado.getIdMovimiento(),
                 producto.getNombre(),
                 movimiento.getTipo(),
                 movimiento.getCantidad(),
                 movimiento.getCostoUnitario());
    }

    /**
     * Calcula el costo promedio ponderado para un movimiento de inventario.
     *
     * Para ingresos:
     *   nuevo_costo = (stock_actual * costo_anterior + cantidad * costo_nuevo) / (stock_anterior + cantidad)
     *
     * Para salidas:
     *   nuevo_costo = costo_anterior (se mantiene el costo promedio)
     *
     * @param stockActual Stock actual en el inventario
     * @param costoAnterior Costo promedio anterior
     * @param cantidad Cantidad del movimiento
     * @param costoUnitario Costo unitario del movimiento
     * @param tipo Tipo de movimiento (INGRESO o SALIDA)
     * @return BigDecimal con el nuevo costo promedio ponderado
     */
    private BigDecimal calcularCostoPromedioPonderado(
            Integer stockActual,
            BigDecimal costoAnterior,
            Integer cantidad,
            BigDecimal costoUnitario,
            TipoMovimiento tipo) {

        log.debug("Calculando costo promedio ponderado - Stock: {}, Costo anterior: {}, " +
                  "Cantidad: {}, Costo unitario: {}, Tipo: {}",
                  stockActual, costoAnterior, cantidad, costoUnitario, tipo);

        // Para salidas, mantener el costo promedio anterior
        if (TipoMovimiento.SALIDA == tipo) {
            log.debug("Salida detectada, se mantiene costo anterior: {}", costoAnterior);
            return costoAnterior;
        }

        // Para ingresos, calcular el nuevo costo promedio ponderado
        if (stockActual == 0) {
            // Si no hay stock, el costo promedio es el del movimiento
            log.debug("Stock inicial, costo = costo unitario del movimiento: {}", costoUnitario);
            return costoUnitario;
        }

        BigDecimal totalAnterior = costoAnterior.multiply(BigDecimal.valueOf(stockActual));
        BigDecimal totalNuevo = costoUnitario.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal nuevoStock = BigDecimal.valueOf(stockActual + cantidad);

        BigDecimal nuevoCosoPromedio = (totalAnterior.add(totalNuevo))
                .divide(nuevoStock, 4, java.math.RoundingMode.HALF_UP);

        log.debug("Costo promedio ponderado calculado: {}", nuevoCosoPromedio);

        return nuevoCosoPromedio;
    }

    /**
     * Convierte una entidad Inventario a InventarioResponse.
     *
     * @param inventario Entidad Inventario
     * @return InventarioResponse con los datos del inventario
     */
    private InventarioResponse toResponse(Inventario inventario) {
        String productoNombre = inventario.getProducto() != null ?
                inventario.getProducto().getNombre() : "Sin producto";
        String sucursalNombre = inventario.getSucursal() != null ?
                inventario.getSucursal().getNombre() : "Sin sucursal";

        return new InventarioResponse(
                inventario.getIdInventario(),
                inventario.getProducto() != null ? inventario.getProducto().getIdProducto() : null,
                productoNombre,
                inventario.getSucursal() != null ? inventario.getSucursal().getIdSucursal() : null,
                sucursalNombre,
                inventario.getStockActual(),
                inventario.getCostoPromedio(),
                inventario.getUpdatedAt()
        );
    }
}
