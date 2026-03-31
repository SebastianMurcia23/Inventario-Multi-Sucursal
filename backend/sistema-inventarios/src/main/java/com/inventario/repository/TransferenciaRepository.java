package com.inventario.repository;

import com.inventario.model.Transferencia;
import com.inventario.model.enums.EstadoTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransferenciaRepository extends JpaRepository<Transferencia, Integer> {

    List<Transferencia> findBySucursalOrigenIdSucursalOrderByFechaSolicitudDesc(Integer idOrigen);

    List<Transferencia> findBySucursalDestinoIdSucursalOrderByFechaSolicitudDesc(Integer idDestino);

    List<Transferencia> findByEstado(EstadoTransferencia estado);

    // Transferencias activas de una sucursal (como origen o destino)
    @Query("""
            SELECT t FROM Transferencia t
            JOIN FETCH t.sucursalOrigen so
            JOIN FETCH t.sucursalDestino sd
            WHERE (t.sucursalOrigen.idSucursal = :idSucursal
                OR t.sucursalDestino.idSucursal = :idSucursal)
            AND t.estado NOT IN ('RECIBIDO', 'CANCELADO')
            ORDER BY t.fechaSolicitud DESC
            """)
    List<Transferencia> findTransferenciasActivasPorSucursal(@Param("idSucursal") Integer idSucursal);

    // Todas las transferencias en curso en la red (para admin)
    @Query("""
            SELECT t FROM Transferencia t
            JOIN FETCH t.sucursalOrigen so
            JOIN FETCH t.sucursalDestino sd
            JOIN FETCH t.usuarioSolicita u
            WHERE t.estado NOT IN ('RECIBIDO', 'CANCELADO')
            ORDER BY t.prioridad DESC, t.fechaSolicitud DESC
            """)
    List<Transferencia> findTodasTransferenciasActivas();

    // Transferencias por sucursal en rango de fechas (para reportes de logística)
    @Query("""
            SELECT t FROM Transferencia t
            JOIN FETCH t.sucursalOrigen so
            JOIN FETCH t.sucursalDestino sd
            WHERE t.sucursalOrigen.idSucursal = :idSucursal
            AND t.fechaSolicitud BETWEEN :desde AND :hasta
            ORDER BY t.fechaSolicitud DESC
            """)
    List<Transferencia> findByOrigenYRangoFecha(
            @Param("idSucursal") Integer idSucursal,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
