package com.inventario.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para una compra.
 * Contiene toda la información de la compra incluyendo datos denormalizados para facilidad de acceso.
 */
public record CompraResponse(
    Integer idCompra,
    String sucursalNombre,
    String proveedorNombre,
    String estado,
    BigDecimal total,
    LocalDateTime fecha,
    Integer diasPago,
    String notas
) {
}
