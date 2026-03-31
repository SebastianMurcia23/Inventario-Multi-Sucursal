package com.inventario.controller;

import com.inventario.dto.request.LoginRequest;
import com.inventario.dto.response.AuthResponse;
import com.inventario.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para autenticación y autorización.
 * Endpoints públicos: login, refresh token.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Endpoint de login.
     * POST /api/v1/auth/login
     * Requiere: email y contraseña.
     * Retorna: token JWT, refresh token e información del usuario.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para refrescar el token.
     * POST /api/v1/auth/refresh
     * Requiere: refresh token en el body.
     * Retorna: nuevo access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String refreshToken = authHeader.substring(7);
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
}
