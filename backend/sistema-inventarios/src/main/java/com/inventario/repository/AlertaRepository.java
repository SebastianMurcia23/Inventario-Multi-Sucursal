package com.inventario.repository;

import com.inventario.model.Alerta;
import com.inventario.model.enums.TipoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Integer> {

    List<Alerta> findBySucursalIdSucursalAndResueltaFalseOrderByCreatedAtDesc(Integer idSucursal);

    List<Alerta> findBySucursalIdSucursalOrderByCreatedAtDesc(Integer idSucursal);

    List<Alerta> findByTipoAlertaAndResueltaFalse(TipoAlerta tipoAlerta);

    List<Alerta> findBySucursalIdSucursalAndTipoAlertaAndResueltaFalse(
            Integer idSucursal, TipoAlerta tipoAlerta);

    // Verificar si ya existe una alerta activa del mismo tipo para evitar duplicados
    boolean existsBySucursalIdSucursalAndProductoIdProductoAndTipoAlertaAndResueltaFalse(
            Integer idSucursal, Integer idProducto, TipoAlerta tipoAlerta);

    // Conteo de alertas no resueltas por sucursal (para badge en dashboard)
    @Query("""
            SELECT COUNT(a)
            FROM Alerta a
            WHERE a.sucursal.idSucursal = :idSucursal
            AND a.resuelta = false
            """)
    Long countAlertasActivasPorSucursal(@Param("idSucursal") Integer idSucursal);

    // Alertas activas de toda la red (para administrador)
    @Query("""
            SELECT a FROM Alerta a
            JOIN FETCH a.sucursal s
            LEFT JOIN FETCH a.producto p
            WHERE a.resuelta = false
            ORDER BY a.createdAt DESC
            """)
    List<Alerta> findTodasAlertasActivas();
}
