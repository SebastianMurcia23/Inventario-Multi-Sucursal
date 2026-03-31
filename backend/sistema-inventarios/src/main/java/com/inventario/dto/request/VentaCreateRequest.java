package com.inventario.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO para crear una nueva venta.
 * Contiene la sucursal, lista de items y notas de descuento.
 */
public record VentaCreateRequest(
    @NotNull(message = "El ID de sucursal es requerido")
    @Positive(message = "El ID de sucursal debe ser un número positivo")
    Integer sucursalId,

    @NotEmpty(message = "La venta debe contener al menos un item")
    @Valid
    List<ProductoVentaItem> ventaItems,

    @Size(min = 0, max = 500, message = "Las notas de descuento no pueden exceder 500 caracteres")
    String notasDescuento
) {
}
