package com.inventario.dto.request;

import com.inventario.model.enums.MotivoMovimiento;
import com.inventario.model.enums.TipoMovimiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * DTO para registrar un movimiento de inventario (ingreso o salida).
 * Incluye validaciones para cantidad y costo unitario.
 */
public record MovimientoInventarioRequest(
    @NotNull(message = "El ID de producto es requerido")
    @Positive(message = "El ID de producto debe ser un número positivo")
    Integer productoId,

    @NotNull(message = "El ID de sucursal es requerido")
    @Positive(message = "El ID de sucursal debe ser un número positivo")
    Integer sucursalId,

    @NotNull(message = "El tipo de movimiento es requerido")
    TipoMovimiento tipo,

    @NotNull(message = "La cantidad es requerida")
    @Positive(message = "La cantidad debe ser un número positivo")
    Integer cantidad,

    @NotNull(message = "El motivo del movimiento es requerido")
    MotivoMovimiento motivo,

    @NotNull(message = "El costo por unidad es requerido")
    @DecimalMin(value = "0.0", inclusive = false, message = "El costo por unidad debe ser mayor a 0")
    BigDecimal costoPorUnidad
) {
    /*
     * JUSTIFICACIÓN DE VALIDACIONES:
     * - @NotNull + @Positive para IDs: Asegura referencias válidas a producto y sucursal
     * - @NotNull para tipo y motivo: Campos enumerados obligatorios
     * - @NotNull + @Positive para cantidad: Debe ser un valor positivo
     * - @NotNull + @DecimalMin para costoPorUnidad: Precisión financiera, no puede ser cero o negativo
     */
}
