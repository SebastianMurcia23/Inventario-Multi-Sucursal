package com.inventario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * DTO para actualizar un producto existente.
 * Solo permite actualizar campos que no son clave o que pueden cambiar después de creación.
 */
public record ProductoUpdateRequest(
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    String nombre,

    @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    String descripcion,

    @NotNull(message = "El stock mínimo es requerido")
    @PositiveOrZero(message = "El stock mínimo debe ser cero o positivo")
    Integer stockMinimo,

    @PositiveOrZero(message = "El stock máximo debe ser cero o positivo")
    Integer stockMaximo,

    @NotNull(message = "El estado activo es requerido")
    Boolean activo
) {
    /*
     * JUSTIFICACIÓN DE VALIDACIONES:
     * - Excluye: idProducto (generado), sku (inmutable, es clave), categoriaId (no debe cambiar)
     * - Incluye: nombre, descripcion, stockMinimo, stockMaximo (pueden cambiar en ciclo de vida)
     * - Incluye: activo (necesario para dar de baja productos)
     * - @Size y @NotBlank: Validan formato de entrada
     */
}
