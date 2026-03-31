package com.inventario.dto.response;

/**
 * DTO de respuesta para productos.
 * Contiene información desnormalizada con nombres de relaciones en lugar de IDs.
 */
public record ProductoResponse(
    Integer idProducto,
    String nombre,
    String sku,
    String descripcion,
    String categoriaNombre,
    Integer stockMinimo,
    Integer stockMaximo,
    Boolean activo
) {
    /*
     * JUSTIFICACIÓN:
     * - No usa validaciones (es response, solo lectura)
     * - Desnormaliza nombres de categoría en lugar de IDs para mejor UX
     * - Permite cliente obtener información completa sin segunda consulta
     * - Todos los datos vienen del servidor, ya validados
     */
}
