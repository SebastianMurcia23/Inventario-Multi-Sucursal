package com.inventario.repository;

import com.inventario.model.MovimientoInventario;
import com.inventario.model.enums.MotivoMovimiento;
import com.inventario.model.enums.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Integer> {

    List<MovimientoInventario> findBySucursalIdSucursalOrderByFechaDesc(Integer idSucursal);

    List<MovimientoInventario> findByProductoIdProductoOrderByFechaDesc(Integer idProducto);

    List<MovimientoInventario> findBySucursalIdSucursalAndProductoIdProductoOrderByFechaDesc(
            Integer idSucursal, Integer idProducto);

    List<MovimientoInventario> findByMotivoOrderByFechaDesc(MotivoMovimiento motivo);

    List<MovimientoInventario> findByTipoOrderByFechaDesc(TipoMovimiento tipo);

    // Historial por sucursal en un rango de fechas
    @Query("""
            SELECT m FROM MovimientoInventario m
            JOIN FETCH m.producto p
            JOIN FETCH m.usuario u
            WHERE m.sucursal.idSucursal = :idSucursal
            AND m.fecha BETWEEN :desde AND :hasta
            ORDER BY m.fecha DESC
            """)
    List<MovimientoInventario> findBySucursalYRangoFecha(
            @Param("idSucursal") Integer idSucursal,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // Movimientos de un producto en todas las sucursales
    @Query("""
            SELECT m FROM MovimientoInventario m
            JOIN FETCH m.sucursal s
            JOIN FETCH m.usuario u
            WHERE m.producto.idProducto = :idProducto
            ORDER BY m.fecha DESC
            """)
    List<MovimientoInventario> findByProductoTodasSucursales(@Param("idProducto") Integer idProducto);

    // Movimientos por referencia (trazabilidad desde una venta, compra o transferencia)
    List<MovimientoInventario> findByReferenciaIdAndReferenciaTipo(
            Integer referenciaId, String referenciaTipo);
}
