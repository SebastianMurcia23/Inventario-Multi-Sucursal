package com.inventario.dto.request;

import com.inventario.model.enums.PrioridadTransferencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO para crear una nueva transferencia entre sucursales.
 * Contiene las sucursales origen y destino, prioridad, notas y los items a transferir.
 */
public record TransferenciaCreateRequest(
    @NotNull(message = "El ID de sucursal origen es requerido")
    @Positive(message = "El ID de sucursal origen debe ser un número positivo")
    Integer sucursalOrigenId,

    @NotNull(message = "El ID de sucursal destino es requerido")
    @Positive(message = "El ID de sucursal destino debe ser un número positivo")
    Integer sucursalDestinoId,

    @NotNull(message = "El nivel de prioridad es requerido")
    PrioridadTransferencia prioridad,

    @Size(max = 1000, message = "Las notas no pueden exceder 1000 caracteres")
    String notas,

    @NotEmpty(message = "La transferencia debe contener al menos un item")
    @Valid
    List<TransferenciaItemRequest> items
) {
}
