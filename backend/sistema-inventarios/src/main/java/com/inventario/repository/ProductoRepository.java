package com.inventario.repository;

import com.inventario.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    Optional<Producto> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Producto> findByActivoTrue();

    List<Producto> findByCategoriaIdCategoriaAndActivoTrue(Integer idCategoria);

    List<Producto> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    @Query("""
            SELECT p FROM Producto p
            WHERE p.activo = true
            AND p.idProducto NOT IN (
                SELECT i.producto.idProducto FROM Inventario i
                WHERE i.sucursal.idSucursal = :idSucursal
            )
            """)
    List<Producto> findProductosSinInventarioEnSucursal(@Param("idSucursal") Integer idSucursal);
}
