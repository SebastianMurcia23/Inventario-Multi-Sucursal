package com.inventario.repository;

import com.inventario.model.Venta;
import com.inventario.model.enums.EstadoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Integer> {

    List<Venta> findBySucursalIdSucursalOrderByFechaDesc(Integer idSucursal);

    List<Venta> findBySucursalIdSucursalAndEstadoOrderByFechaDesc(
            Integer idSucursal, EstadoVenta estado);

    List<Venta> findByUsuarioIdUsuarioOrderByFechaDesc(Integer idUsuario);

    // Ventas por sucursal en un rango de fechas
    @Query("""
            SELECT v FROM Venta v
            JOIN FETCH v.usuario u
            WHERE v.sucursal.idSucursal = :idSucursal
            AND v.estado = 'CONFIRMADA'
            AND v.fecha BETWEEN :desde AND :hasta
            ORDER BY v.fecha DESC
            """)
    List<Venta> findVentasConfirmadasPorSucursalYRango(
            @Param("idSucursal") Integer idSucursal,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // Total de ventas confirmadas por sucursal en un mes (para dashboard)
    @Query("""
            SELECT COALESCE(SUM(v.total), 0)
            FROM Venta v
            WHERE v.sucursal.idSucursal = :idSucursal
            AND v.estado = 'CONFIRMADA'
            AND v.fecha BETWEEN :desde AND :hasta
            """)
    BigDecimal sumTotalVentasConfirmadasEnPeriodo(
            @Param("idSucursal") Integer idSucursal,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // Conteo de ventas por sucursal en un periodo (para dashboard)
    @Query("""
            SELECT COUNT(v)
            FROM Venta v
            WHERE v.sucursal.idSucursal = :idSucursal
            AND v.estado = 'CONFIRMADA'
            AND v.fecha BETWEEN :desde AND :hasta
            """)
    Long countVentasConfirmadasEnPeriodo(
            @Param("idSucursal") Integer idSucursal,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
