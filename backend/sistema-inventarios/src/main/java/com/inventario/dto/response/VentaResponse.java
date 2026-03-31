package com.inventario.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para una venta.
 * Contiene los datos principales de la venta con información denormalizada.
 */
public record VentaResponse(
    Integer idVenta,
    String sucursalNombre,
    String estado,
    BigDecimal total,
    LocalDateTime fecha,
    String notas
) {
}
