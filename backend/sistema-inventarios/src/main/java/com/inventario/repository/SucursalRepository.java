package com.inventario.repository;

import com.inventario.model.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SucursalRepository extends JpaRepository<Sucursal, Integer> {

    List<Sucursal> findByEstadoTrue();

    boolean existsByNombre(String nombre);

    boolean existsByCodigo(String codigo);

    Optional<Sucursal> findByCodigo(String codigo);
}
