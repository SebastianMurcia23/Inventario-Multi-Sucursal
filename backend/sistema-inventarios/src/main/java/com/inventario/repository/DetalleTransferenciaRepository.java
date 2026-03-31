package com.inventario.repository;

import com.inventario.model.DetalleTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleTransferenciaRepository extends JpaRepository<DetalleTransferencia, Integer> {

    List<DetalleTransferencia> findByTransferenciaIdTransferencia(Integer idTransferencia);

    List<DetalleTransferencia> findByProductoIdProducto(Integer idProducto);

    // Detalles con faltantes (para generar alertas de recepción parcial)
    @Query("""
            SELECT dt FROM DetalleTransferencia dt
            JOIN FETCH dt.producto p
            WHERE dt.transferencia.idTransferencia = :idTransferencia
            AND dt.cantidadRecibida < dt.cantidadEnviada
            """)
    List<DetalleTransferencia> findFaltantesPorTransferencia(
            @Param("idTransferencia") Integer idTransferencia);
}
