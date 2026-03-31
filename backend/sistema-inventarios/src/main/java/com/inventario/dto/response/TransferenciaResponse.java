package com.inventario.dto.response;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para una transferencia.
 * Contiene todos los datos de la transferencia incluyendo información de sucursales.
 */
public record TransferenciaResponse(
    Integer idTransferencia,
    String sucursalOrigen,
    String sucursalDestino,
    String estado,
    String prioridad,
    LocalDateTime fechaSolicitud,
    LocalDateTime fechaEnvio,
    LocalDateTime fechaRecepcion,
    String notas
) {
}
