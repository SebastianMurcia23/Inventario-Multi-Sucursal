package com.inventario.repository;

import com.inventario.model.ProductoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoProveedorRepository extends JpaRepository<ProductoProveedor, Integer> {

    List<ProductoProveedor> findByProductoIdProducto(Integer idProducto);

    List<ProductoProveedor> findByProveedorIdProveedor(Integer idProveedor);

    Optional<ProductoProveedor> findByProductoIdProductoAndPreferidoTrue(Integer idProducto);

    boolean existsByProductoIdProductoAndProveedorIdProveedor(Integer idProducto, Integer idProveedor);
}
