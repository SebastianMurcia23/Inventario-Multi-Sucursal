package com.inventario.repository;

import com.inventario.model.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Integer> {

    Optional<Ruta> findBySucursalOrigenIdSucursalAndSucursalDestinoIdSucursal(
            Integer idOrigen, Integer idDestino);

    List<Ruta> findBySucursalOrigenIdSucursalAndActivaTrue(Integer idOrigen);

    List<Ruta> findBySucursalDestinoIdSucursalAndActivaTrue(Integer idDestino);

    boolean existsBySucursalOrigenIdSucursalAndSucursalDestinoIdSucursal(
            Integer idOrigen, Integer idDestino);
}
