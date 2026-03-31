package com.inventario.repository;

import com.inventario.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    Optional<Proveedor> findByNit(String nit);

    List<Proveedor> findByActivoTrue();

    boolean existsByNit(String nit);

    List<Proveedor> findByNombreContainingIgnoreCase(String nombre);
}
