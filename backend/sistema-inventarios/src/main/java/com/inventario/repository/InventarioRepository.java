package com.inventario.repository;

import com.inventario.model.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Integer> {

    Optional<Inventario> findBySucursalIdSucursalAndProductoIdProducto(
            Integer idSucursal, Integer idProducto);

    List<Inventario> findBySucursalIdSucursal(Integer idSucursal);

    List<Inventario> findByProductoIdProducto(Integer idProducto);

    // Productos con stock bajo el mínimo en una sucursal
    @Query("""
            SELECT i FROM Inventario i
            JOIN FETCH i.producto p
            JOIN FETCH i.sucursal s
            WHERE i.sucursal.idSucursal = :idSucursal
            AND i.stockActual <= COALESCE(i.stockMinimo, p.stockMinimo)
            """)
    List<Inventario> findStockBajoMinimoEnSucursal(@Param("idSucursal") Integer idSucursal);

    // Productos agotados en una sucursal
    @Query("""
            SELECT i FROM Inventario i
            WHERE i.sucursal.idSucursal = :idSucursal
            AND i.stockActual = 0
            """)
    List<Inventario> findStockAgotadoEnSucursal(@Param("idSucursal") Integer idSucursal);

    // Stock de un producto en todas las sucursales (visibilidad de red)
    @Query("""
            SELECT i FROM Inventario i
            JOIN FETCH i.sucursal s
            WHERE i.producto.idProducto = :idProducto
            AND s.estado = true
            """)
    List<Inventario> findStockPorProductoEnTodasSucursales(@Param("idProducto") Integer idProducto);

    // Verificar si hay stock suficiente
    @Query("""
            SELECT CASE WHEN i.stockActual >= :cantidad THEN true ELSE false END
            FROM Inventario i
            WHERE i.sucursal.idSucursal = :idSucursal
            AND i.producto.idProducto = :idProducto
            """)
    Boolean verificarStockDisponible(
            @Param("idSucursal") Integer idSucursal,
            @Param("idProducto") Integer idProducto,
            @Param("cantidad") Integer cantidad);
}
