package com.inventario.repository;

import com.inventario.model.DetalleCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Integer> {

    List<DetalleCompra> findByCompraIdCompra(Integer idCompra);

    List<DetalleCompra> findByProductoIdProducto(Integer idProducto);

    // Historial de compras de un producto específico con datos de la orden
    @Query("""
            SELECT dc FROM DetalleCompra dc
            JOIN FETCH dc.compra c
            JOIN FETCH c.sucursal s
            JOIN FETCH c.proveedor p
            WHERE dc.producto.idProducto = :idProducto
            ORDER BY c.fecha DESC
            """)
    List<DetalleCompra> findHistorialComprasPorProducto(@Param("idProducto") Integer idProducto);
}
