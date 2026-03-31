package com.inventario.repository;

import com.inventario.model.ProductoUnidad;
import com.inventario.model.ProductoUnidadId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoUnidadRepository extends JpaRepository<ProductoUnidad, ProductoUnidadId> {

    List<ProductoUnidad> findByProductoIdProducto(Integer idProducto);

    Optional<ProductoUnidad> findByProductoIdProductoAndEsPrincipalTrue(Integer idProducto);
}
