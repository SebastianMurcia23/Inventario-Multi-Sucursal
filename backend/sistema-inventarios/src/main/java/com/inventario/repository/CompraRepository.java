package com.inventario.repository;

import com.inventario.model.Compra;
import com.inventario.model.enums.EstadoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Integer> {

    List<Compra> findBySucursalIdSucursalOrderByFechaDesc(Integer idSucursal);

    List<Compra> findByProveedorIdProveedorOrderByFechaDesc(Integer idProveedor);

    List<Compra> findByEstado(EstadoCompra estado);

    List<Compra> findBySucursalIdSucursalAndEstado(Integer idSucursal, EstadoCompra estado);

    // Historial de compras por sucursal en rango de fechas
    @Query("""
            SELECT c FROM Compra c
            JOIN FETCH c.proveedor p
            JOIN FETCH c.usuario u
            WHERE c.sucursal.idSucursal = :idSucursal
            AND c.fecha BETWEEN :desde AND :hasta
            ORDER BY c.fecha DESC
            """)
    List<Compra> findBySucursalYRangoFecha(
            @Param("idSucursal") Integer idSucursal,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // Historial de compras por proveedor en rango de fechas
    @Query("""
            SELECT c FROM Compra c
            JOIN FETCH c.sucursal s
            WHERE c.proveedor.idProveedor = :idProveedor
            AND c.fecha BETWEEN :desde AND :hasta
            ORDER BY c.fecha DESC
            """)
    List<Compra> findByProveedorYRangoFecha(
            @Param("idProveedor") Integer idProveedor,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
