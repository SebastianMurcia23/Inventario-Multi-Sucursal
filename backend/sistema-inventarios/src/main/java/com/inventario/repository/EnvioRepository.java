package com.inventario.repository;

import com.inventario.model.Envio;
import com.inventario.model.enums.EstadoEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, Integer> {

    Optional<Envio> findByTransferenciaIdTransferencia(Integer idTransferencia);

    List<Envio> findByEstado(EstadoEnvio estado);

    // Reporte de cumplimiento logístico: envíos donde tiempo_real > tiempo_estimado
    @Query("""
            SELECT e FROM Envio e
            JOIN FETCH e.transferencia t
            JOIN FETCH t.sucursalOrigen so
            JOIN FETCH t.sucursalDestino sd
            WHERE e.tiempoReal IS NOT NULL
            AND e.tiempoReal > e.tiempoEstimado
            ORDER BY e.createdAt DESC
            """)
    List<Envio> findEnviosConRetraso();

    // Promedio de tiempo real vs estimado por ruta (para módulo de logística)
    @Query("""
            SELECT t.ruta.idRuta,
                   AVG(e.tiempoEstimado),
                   AVG(e.tiempoReal)
            FROM Envio e
            JOIN e.transferencia t
            WHERE t.ruta IS NOT NULL
            AND e.tiempoReal IS NOT NULL
            GROUP BY t.ruta.idRuta
            """)
    List<Object[]> findPromediosTiemposPorRuta();
}
