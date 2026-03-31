package com.inventario.dto.response;

import com.inventario.model.enums.RolUsuario;

/**
 * DTO con respuesta de autenticación.
 * Contiene el token JWT, el usuario y su información de autorización.
 */
public record AuthResponse(
    String accessToken,
    String refreshToken,
    Integer userId,
    String email,
    String nombre,
    RolUsuario rol,
    Integer branchId
) {}
