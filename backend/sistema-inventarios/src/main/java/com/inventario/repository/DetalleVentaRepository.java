package com.inventario.repository;

import com.inventario.model.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Integer> {

    List<DetalleVenta> findByVentaIdVenta(Integer idVenta);

    List<DetalleVenta> findByProductoIdProducto(Integer idProducto);

    // Productos más vendidos en una sucursal en un periodo (para dashboard)
    @Query("""
            SELECT dv.producto.idProducto, dv.producto.nombre, SUM(dv.cantidad) as totalVendido
            FROM DetalleVenta dv
            JOIN dv.venta v
            WHERE v.sucursal.idSucursal = :idSucursal
            AND v.estado = 'CONFIRMADA'
            AND v.fecha BETWEEN :desde AND :hasta
            GROUP BY dv.producto.idProducto, dv.producto.nombre
            ORDER BY totalVendido DESC
            """)
    List<Object[]> findProductosMasVendidosPorSucursal(
            @Param("idSucursal") Integer idSucursal,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // Historial de ventas de un producto en una sucursal
    @Query("""
            SELECT dv FROM DetalleVenta dv
            JOIN FETCH dv.venta v
            WHERE dv.producto.idProducto = :idProducto
            AND v.sucursal.idSucursal = :idSucursal
            AND v.estado = 'CONFIRMADA'
            ORDER BY v.fecha DESC
            """)
    List<DetalleVenta> findHistorialVentasPorProductoYSucursal(
            @Param("idProducto") Integer idProducto,
            @Param("idSucursal") Integer idSucursal);

    // Cantidad total vendida de un producto por sucursal en un periodo
    // (usado por el servicio de predicción de demanda)
    @Query("""
            SELECT COALESCE(SUM(dv.cantidad), 0)
            FROM DetalleVenta dv
            JOIN dv.venta v
            WHERE dv.producto.idProducto = :idProducto
            AND v.sucursal.idSucursal = :idSucursal
            AND v.estado = 'CONFIRMADA'
            AND v.fecha BETWEEN :desde AND :hasta
            """)
    Integer sumCantidadVendidaEnPeriodo(
            @Param("idProducto") Integer idProducto,
            @Param("idSucursal") Integer idSucursal,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
