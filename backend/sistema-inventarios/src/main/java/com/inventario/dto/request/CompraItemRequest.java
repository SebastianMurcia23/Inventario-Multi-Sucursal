package com.inventario.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * DTO para los items de una compra.
 * Cada item representa un producto con cantidad, precio y descuento aplicable.
 */
public record CompraItemRequest(
    @NotNull(message = "El ID del producto es requerido")
    @Positive(message = "El ID del producto debe ser un número positivo")
    Integer productoId,

    @NotNull(message = "La cantidad es requerida")
    @Positive(message = "La cantidad debe ser un número positivo")
    Integer cantidad,

    @NotNull(message = "El precio por unidad es requerido")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    BigDecimal precioPorUnidad,

    @DecimalMin(value = "0", message = "El descuento no puede ser negativo")
    @Positive(message = "Si se proporciona descuento, debe ser mayor a 0")
    BigDecimal descuentoPorcentaje
) {
}
