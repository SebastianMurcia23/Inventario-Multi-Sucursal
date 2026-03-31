package com.inventario.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * DTO para crear una nueva compra.
 * Contiene los datos de la compra inicial con referencias a sucursal y proveedor.
 */
public record CompraCreateRequest(
    @NotNull(message = "El ID de sucursal es requerido")
    @Positive(message = "El ID de sucursal debe ser un número positivo")
    Integer sucursalId,

    @NotNull(message = "El ID de proveedor es requerido")
    @Positive(message = "El ID de proveedor debe ser un número positivo")
    Integer proveedorId,

    @NotNull(message = "La fecha esperada es requerida")
    LocalDate fechaEsperada,

    @Size(min = 0, max = 500, message = "Las condiciones de pago no pueden exceder 500 caracteres")
    String condicionesPago
) {
}
