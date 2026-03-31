package com.inventario.service;

import com.inventario.dto.request.LoginRequest;
import com.inventario.dto.response.AuthResponse;
import com.inventario.model.Usuario;
import com.inventario.repository.UsuarioRepository;
import com.inventario.config.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio de autenticación.
 * Maneja login, refresh de token y validación de credenciales.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Autentica un usuario por email y contraseña.
     * Retorna un token JWT si las credenciales son válidas.
     */
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Intento de login para email: {}", loginRequest.email());

        // Buscar usuario por email
        Usuario usuario = usuarioRepository.findByCorreo(loginRequest.email())
            .orElseThrow(() -> {
                log.warn("Usuario no encontrado: {}", loginRequest.email());
                return new RuntimeException("Credenciales inválidas");
            });

        // Verificar que la cuenta esté activa
        if (!usuario.getActivo()) {
            log.warn("Intento de login con cuenta inactiva: {}", loginRequest.email());
            throw new RuntimeException("La cuenta está inactiva");
        }

        // Validar contraseña
        if (!passwordEncoder.matches(loginRequest.password(), usuario.getContrasenaHash())) {
            log.warn("Contraseña inválida para usuario: {}", loginRequest.email());
            throw new RuntimeException("Credenciales inválidas");
        }

        // Generar tokens
        String accessToken = jwtService.generateToken(
            usuario.getIdUsuario(),
            usuario.getCorreo(),
            usuario.getRol().toString(),
            usuario.getSucursal() != null ? usuario.getSucursal().getIdSucursal() : null
        );

        String refreshToken = jwtService.generateRefreshToken(usuario.getCorreo());

        log.info("Login exitoso para usuario: {}", usuario.getCorreo());
        return new AuthResponse(
            accessToken,
            refreshToken,
            usuario.getIdUsuario(),
            usuario.getCorreo(),
            usuario.getNombre(),
            usuario.getRol(),
            usuario.getSucursal() != null ? usuario.getSucursal().getIdSucursal() : null
        );
    }

    /**
     * Refresca el token de acceso usando el refresh token.
     */
    public AuthResponse refreshToken(String refreshToken) {
        try {
            String email = jwtService.extractUsername(refreshToken);
            Usuario usuario = usuarioRepository.findByCorreo(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            String newAccessToken = jwtService.generateToken(
                usuario.getIdUsuario(),
                usuario.getCorreo(),
                usuario.getRol().toString(),
                usuario.getSucursal() != null ? usuario.getSucursal().getIdSucursal() : null
            );

            return new AuthResponse(
                newAccessToken,
                refreshToken,
                usuario.getIdUsuario(),
                usuario.getCorreo(),
                usuario.getNombre(),
                usuario.getRol(),
                usuario.getSucursal() != null ? usuario.getSucursal().getIdSucursal() : null
            );
        } catch (Exception e) {
            throw new RuntimeException("Token inválido o expirado");
        }
    }
}
