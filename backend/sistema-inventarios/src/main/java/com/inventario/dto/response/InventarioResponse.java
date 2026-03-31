package com.inventario.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para items de inventario.
 * Contiene información desnormalizada del inventario con datos de producto y sucursal para facilitar consultas.
 */
public record InventarioResponse(
    Integer id,
    Integer productoId,
    String productoNombre,
    Integer sucursalId,
    String sucursalNombre,
    Integer stockActual,
    BigDecimal costoPromedio,
    LocalDateTime fechaUltimoMovimiento
) {
    /*
     * JUSTIFICACIÓN:
     * - No usa validaciones (es response, solo lectura)
     * - Desnormaliza datos de producto y sucursal para evitar N+1 queries en cliente
     * - Utiliza BigDecimal para costoPromedio (precisión financiera requerida)
     * - LocalDateTime para timestamp de auditoría
     * - Permite al cliente tener vista completa de estado del inventario
     * - Todos los valores vienen del servidor ya procesados
     */
}
