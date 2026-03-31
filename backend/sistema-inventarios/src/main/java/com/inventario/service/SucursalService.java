package com.inventario.service;

import com.inventario.dto.request.SucursalCreateRequest;
import com.inventario.dto.request.SucursalUpdateRequest;
import com.inventario.dto.response.SucursalResponse;
import com.inventario.model.Sucursal;
import com.inventario.repository.SucursalRepository;
import com.inventario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar sucursales del sistema.
 * Maneja operaciones CRUD de sucursales con validaciones y logging completo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SucursalService {

    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Obtiene todas las sucursales activas del sistema.
     *
     * @return Lista de SucursalResponse con todas las sucursales activas
     */
    @Transactional(readOnly = true)
    public List<SucursalResponse> getAllBranches() {
        log.debug("Obteniendo todas las sucursales activas");

        List<Sucursal> sucursales = sucursalRepository.findByEstadoTrue();

        log.info("Se obtuvieron {} sucursales activas", sucursales.size());

        return sucursales.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una sucursal específica por su ID.
     *
     * @param id ID de la sucursal
     * @return SucursalResponse con los datos de la sucursal
     * @throws RuntimeException si la sucursal no existe
     */
    @Transactional(readOnly = true)
    public SucursalResponse getBranchById(Integer id) {
        log.debug("Obteniendo sucursal con ID: {}", id);

        Sucursal sucursal = sucursalRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Sucursal no encontrada con ID: {}", id);
                    return new RuntimeException("Sucursal no encontrada con ID: " + id);
                });

        log.info("Sucursal obtenida exitosamente: {} (ID: {})", sucursal.getNombre(), id);

        return toResponse(sucursal);
    }

    /**
     * Crea una nueva sucursal en el sistema.
     * Valida que no exista ya una sucursal con el mismo nombre o código.
     *
     * @param request Datos de la nueva sucursal (SucursalCreateRequest)
     * @return SucursalResponse con los datos de la sucursal creada
     * @throws RuntimeException si ya existe una sucursal con ese nombre o código
     */
    @Transactional
    public SucursalResponse createBranch(SucursalCreateRequest request) {
        log.info("Iniciando creación de nueva sucursal: {}", request.nombre());

        // Validar que no exista sucursal con el mismo nombre
        if (sucursalRepository.existsByNombre(request.nombre())) {
            log.warn("Intento de crear sucursal con nombre duplicado: {}", request.nombre());
            throw new RuntimeException("Ya existe una sucursal con el nombre: " + request.nombre());
        }

        // Validar que no exista sucursal con el mismo código
        if (sucursalRepository.existsByCodigo(request.codigo())) {
            log.warn("Intento de crear sucursal con código duplicado: {}", request.codigo());
            throw new RuntimeException("Ya existe una sucursal con el código: " + request.codigo());
        }

        // Crear entidad Sucursal
        Sucursal sucursal = Sucursal.builder()
                .nombre(request.nombre())
                .codigo(request.codigo())
                .direccion(request.direccion())
                .ciudad(request.ciudad())
                .telefono(request.telefono())
                .estado(request.activo())
                .build();

        Sucursal sucursalGuardada = sucursalRepository.save(sucursal);

        log.info("Sucursal creada exitosamente con ID: {} - Nombre: {}",
                 sucursalGuardada.getIdSucursal(), sucursalGuardada.getNombre());

        return toResponse(sucursalGuardada);
    }

    /**
     * Actualiza una sucursal existente.
     *
     * @param id ID de la sucursal a actualizar
     * @param request Datos actualizados de la sucursal
     * @return SucursalResponse con los datos actualizados
     * @throws RuntimeException si la sucursal no existe o el nombre está duplicado
     */
    @Transactional
    public SucursalResponse updateBranch(Integer id, SucursalUpdateRequest request) {
        log.info("Iniciando actualización de sucursal ID: {}", id);

        Sucursal sucursal = sucursalRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Sucursal no encontrada para actualizar con ID: {}", id);
                    return new RuntimeException("Sucursal no encontrada con ID: " + id);
                });

        // Validar nombre duplicado (solo si cambió el nombre)
        if (!sucursal.getNombre().equals(request.nombre()) &&
            sucursalRepository.existsByNombre(request.nombre())) {
            log.warn("Intento de actualizar sucursal con nombre duplicado: {}", request.nombre());
            throw new RuntimeException("Ya existe una sucursal con el nombre: " + request.nombre());
        }

        // Actualizar campos
        sucursal.setNombre(request.nombre());
        sucursal.setCodigo(request.codigo());
        sucursal.setDireccion(request.direccion());
        sucursal.setCiudad(request.ciudad());
        sucursal.setTelefono(request.telefono());
        sucursal.setEstado(request.activo());

        Sucursal sucursalActualizada = sucursalRepository.save(sucursal);

        log.info("Sucursal actualizada exitosamente - ID: {} - Nombre: {}",
                 id, sucursalActualizada.getNombre());

        return toResponse(sucursalActualizada);
    }

    /**
     * Elimina (desactiva) una sucursal del sistema.
     * Valida que no existan usuarios activos en la sucursal antes de eliminarla.
     *
     * @param id ID de la sucursal a eliminar
     * @throws RuntimeException si la sucursal no existe o contiene usuarios activos
     */
    @Transactional
    public void deleteBranch(Integer id) {
        log.info("Iniciando eliminación de sucursal ID: {}", id);

        Sucursal sucursal = sucursalRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Sucursal no encontrada para eliminar con ID: {}", id);
                    return new RuntimeException("Sucursal no encontrada con ID: " + id);
                });

        // Validar que no existan usuarios en la sucursal
        long usuariosActivos = usuarioRepository.findBySucursalIdSucursalAndActivoTrue(id).size();

        if (usuariosActivos > 0) {
            log.warn("Intento de eliminar sucursal con {} usuarios activos. ID: {}",
                     usuariosActivos, id);
            throw new RuntimeException("No se puede eliminar una sucursal con usuarios activos. " +
                    "Desactive los usuarios primero.");
        }

        // Desactivar la sucursal
        sucursal.setEstado(false);
        sucursalRepository.save(sucursal);

        log.info("Sucursal desactivada exitosamente - ID: {} - Nombre: {}",
                 id, sucursal.getNombre());
    }

    /**
     * Convierte una entidad Sucursal a SucursalResponse.
     *
     * @param sucursal Entidad Sucursal
     * @return SucursalResponse con los datos de la sucursal
     */
    private SucursalResponse toResponse(Sucursal sucursal) {
        return new SucursalResponse(
                sucursal.getIdSucursal(),
                sucursal.getNombre(),
                sucursal.getCodigo(),
                sucursal.getDireccion(),
                sucursal.getCiudad(),
                sucursal.getTelefono(),
                sucursal.getEstado()
        );
    }
}
