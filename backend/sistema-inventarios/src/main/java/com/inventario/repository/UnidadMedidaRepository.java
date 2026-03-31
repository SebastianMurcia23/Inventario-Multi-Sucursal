package com.inventario.repository;

import com.inventario.model.UnidadMedida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Integer> {

    Optional<UnidadMedida> findByNombre(String nombre);

    Optional<UnidadMedida> findByAbreviatura(String abreviatura);

    boolean existsByNombre(String nombre);

    boolean existsByAbreviatura(String abreviatura);
}
