package com.inventario.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO para los items de una transferencia.
 * Contiene el producto y cantidad a transferir.
 */
public record TransferenciaItemRequest(
    @NotNull(message = "El ID del producto es requerido")
    @Positive(message = "El ID del producto debe ser un número positivo")
    Integer productoId,

    @NotNull(message = "La cantidad es requerida")
    @Positive(message = "La cantidad debe ser un número positivo")
    Integer cantidad
) {
}
