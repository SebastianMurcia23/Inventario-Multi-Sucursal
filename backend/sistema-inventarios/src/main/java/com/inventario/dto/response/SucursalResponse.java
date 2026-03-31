package com.inventario.dto.response;

/**
 * DTO de respuesta para sucursales.
 * Contiene todos los campos de una sucursal para devoluciones en endpoints GET, POST, PUT.
 */
public record SucursalResponse(
    Integer idSucursal,
    String nombre,
    String codigo,
    String direccion,
    String ciudad,
    String telefono,
    Boolean activo
) {
    /*
     * JUSTIFICACIÓN:
     * - No usa validaciones porque es un response (solo lectura)
     * - Todos los campos vienen del servidor, ya validados en la BD
     * - Estructura simple para serialización JSON
     */
}
