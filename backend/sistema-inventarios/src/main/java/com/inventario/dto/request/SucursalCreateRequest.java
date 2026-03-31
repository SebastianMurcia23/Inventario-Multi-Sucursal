package com.inventario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear una nueva sucursal.
 * Incluye todos los campos necesarios para la creación, con el ID generado automáticamente.
 */
public record SucursalCreateRequest(
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    String nombre,

    @NotBlank(message = "El código es requerido")
    @Size(min = 2, max = 20, message = "El código debe tener entre 2 y 20 caracteres")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "El código debe contener solo letras mayúsculas, números y guiones")
    String codigo,

    @NotBlank(message = "La dirección es requerida")
    @Size(min = 5, max = 255, message = "La dirección debe tener entre 5 y 255 caracteres")
    String direccion,

    @NotBlank(message = "La ciudad es requerida")
    @Size(min = 2, max = 50, message = "La ciudad debe tener entre 2 y 50 caracteres")
    String ciudad,

    @NotBlank(message = "El teléfono es requerido")
    @Pattern(regexp = "^[0-9+\\-() ]+$", message = "El teléfono contiene caracteres inválidos")
    @Size(min = 7, max = 20, message = "El teléfono debe tener entre 7 y 20 caracteres")
    String telefono,

    @NotNull(message = "El estado activo es requerido")
    Boolean activo
) {
    /*
     * JUSTIFICACIÓN DE VALIDACIONES:
     * - @NotBlank: Garantiza que campos de texto no sean vacíos o solo espacios
     * - @Size: Valida longitud según restricciones de BD (máximos definidos en modelo)
     * - @Pattern para codigo: Asegura formato estándar para códigos (mayúsculas, números, guiones)
     * - @Pattern para telefono: Permite formatos comunes de teléfono internacional
     * - @NotNull para activo: Campo booleano no puede ser null
     */
}
