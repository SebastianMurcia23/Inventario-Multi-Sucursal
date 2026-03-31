package com.inventario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear un nuevo producto.
 * Incluye relación con categoría mediante ID.
 */
public record ProductoCreateRequest(
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    String nombre,

    @NotBlank(message = "El SKU es requerido")
    @Size(min = 2, max = 60, message = "El SKU debe tener entre 2 y 60 caracteres")
    String sku,

    @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    String descripcion,

    @NotNull(message = "El ID de categoría es requerido")
    @Positive(message = "El ID de categoría debe ser un número positivo")
    Integer categoriaId,

    @NotNull(message = "El stock mínimo es requerido")
    @PositiveOrZero(message = "El stock mínimo debe ser cero o positivo")
    Integer stockMinimo,

    @PositiveOrZero(message = "El stock máximo debe ser cero o positivo")
    Integer stockMaximo
) {
    /*
     * JUSTIFICACIÓN DE VALIDACIONES:
     * - @NotBlank: Valida que nombre y sku no sean vacíos
     * - @Size: Limita longitud según restricciones de BD
     * - @NotNull + @Positive para categoriaId: Asegura referencia válida a entidad Categoria
     * - @PositiveOrZero para stockMinimo/stockMaximo: Niveles de stock no pueden ser negativos
     */
}
