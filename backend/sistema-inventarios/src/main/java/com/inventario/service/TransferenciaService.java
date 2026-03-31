package com.inventario.service;

import com.inventario.exception.InsufficientStockException;
import com.inventario.exception.ResourceNotFoundException;
import com.inventario.model.DetalleTransferencia;
import com.inventario.model.Inventario;
import com.inventario.model.MovimientoInventario;
import com.inventario.model.Transferencia;
import com.inventario.model.enums.EstadoTransferencia;
import com.inventario.model.enums.MotivoMovimiento;
import com.inventario.model.enums.TipoMovimiento;
import com.inventario.repository.DetalleTransferenciaRepository;
import com.inventario.repository.InventarioRepository;
import com.inventario.repository.SucursalRepository;
import com.inventario.repository.TransferenciaRepository;
import com.inventario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Servicio para gestionar transferencias de inventario entre sucursales.
 * Maneja el ciclo completo de transferencias desde la solicitud hasta la recepción.
 *
 * Flujo de estados:
 * SOLICITADO → APROBADO → ENVIADO → RECIBIDO | PARCIAL | CANCELADO
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferenciaService {

    private final TransferenciaRepository transferenciaRepository;
    private final DetalleTransferenciaRepository detalleTransferenciaRepository;
    private final InventarioService inventarioService;
    private final InventarioRepository inventarioRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Obtiene todas las transferencias del sistema, opcionalmente filtradas por sucursal destino.
     *
     * @param branchIdDestino ID de sucursal destino (opcional)
     * @return Lista de transferencias
     */
    @Transactional(readOnly = true)
    public List<Transferencia> getAllTransfers(Integer branchIdDestino) {
        log.debug("Obteniendo transferencias - Sucursal destino: {}", branchIdDestino);

        if (branchIdDestino != null) {
            return transferenciaRepository.findBySucursalDestinoIdSucursalOrderByFechaSolicitudDesc(branchIdDestino);
        }

        return transferenciaRepository.findAll();
    }

    /**
     * Obtiene una transferencia específica por su ID.
     *
     * @param id ID de la transferencia
     * @return Objeto Transferencia con los detalles
     * @throws ResourceNotFoundException si la transferencia no existe
     */
    @Transactional(readOnly = true)
    public Transferencia getTransferById(Integer id) {
        log.debug("Obteniendo transferencia con ID: {}", id);
        return transferenciaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Transferencia no encontrada con ID: {}", id);
                    return new ResourceNotFoundException("Transferencia no encontrada con ID: " + id);
                });
    }

    /**
     * Crea una nueva solicitud de transferencia en estado SOLICITADO.
     * Valida que las sucursales origen y destino existan y sean diferentes.
     *
     * @param transferencia Objeto Transferencia con los datos iniciales
     * @param detalles Lista de DetalleTransferencia con los items
     * @return Transferencia creada
     * @throws ResourceNotFoundException si sucursales no existen
     * @throws IllegalArgumentException si sucursal origen y destino son iguales o faltan detalles
     */
    @Transactional
    public Transferencia createTransfer(Transferencia transferencia, List<DetalleTransferencia> detalles) {
        log.info("Iniciando creación de nueva transferencia - Origen: {}, Destino: {}, Items: {}",
                 transferencia.getSucursalOrigen().getIdSucursal(),
                 transferencia.getSucursalDestino().getIdSucursal(),
                 detalles.size());

        // Validaciones iniciales
        if (detalles == null || detalles.isEmpty()) {
            log.warn("Intento de crear transferencia sin detalles");
            throw new IllegalArgumentException("La transferencia debe contener al menos un item");
        }

        Integer idOrigen = transferencia.getSucursalOrigen().getIdSucursal();
        Integer idDestino = transferencia.getSucursalDestino().getIdSucursal();

        if (idOrigen.equals(idDestino)) {
            log.warn("Sucursal origen y destino son iguales");
            throw new IllegalArgumentException("La sucursal origen y destino deben ser diferentes");
        }

        if (!sucursalRepository.existsById(idOrigen)) {
            log.error("Sucursal origen no encontrada con ID: {}", idOrigen);
            throw new ResourceNotFoundException("Sucursal origen no encontrada");
        }

        if (!sucursalRepository.existsById(idDestino)) {
            log.error("Sucursal destino no encontrada con ID: {}", idDestino);
            throw new ResourceNotFoundException("Sucursal destino no encontrada");
        }

        // Guardar transferencia
        transferencia.setEstado(EstadoTransferencia.SOLICITADO);
        Transferencia transferenciaGuardada = transferenciaRepository.save(transferencia);

        log.debug("Transferencia guardada - ID: {}, Estado: {}", transferenciaGuardada.getIdTransferencia(),
                 transferenciaGuardada.getEstado());

        // Guardar detalles
        for (DetalleTransferencia detalle : detalles) {
            detalle.setTransferencia(transferenciaGuardada);
            detalleTransferenciaRepository.save(detalle);

            log.debug("Detalle de transferencia creado - Producto ID: {}, Cantidad solicitada: {}",
                     detalle.getProducto().getIdProducto(),
                     detalle.getCantidadSolicitada());
        }

        log.info("Transferencia creada exitosamente - ID: {}, Estado: {}",
                 transferenciaGuardada.getIdTransferencia(),
                 transferenciaGuardada.getEstado());

        return transferenciaGuardada;
    }

    /**
     * Aprueba una transferencia y reserva el stock en la sucursal origen.
     * Valida que haya stock disponible para todos los items.
     * Registra movimientos de salida de inventario en sucursal origen.
     *
     * @param id ID de la transferencia
     * @return Transferencia aprobada con estado APROBADO
     * @throws ResourceNotFoundException si la transferencia no existe
     * @throws IllegalArgumentException si la transferencia no está en estado SOLICITADO
     * @throws InsufficientStockException si no hay stock en sucursal origen
     */
    @Transactional
    public Transferencia approveTransfer(Integer id) {
        log.info("Aprobando transferencia ID: {}", id);

        Transferencia transferencia = getTransferById(id);

        if (transferencia.getEstado() != EstadoTransferencia.SOLICITADO) {
            log.warn("Transferencia no está en estado SOLICITADO - ID: {}, Estado: {}", id,
                    transferencia.getEstado());
            throw new IllegalArgumentException("La transferencia debe estar en estado SOLICITADO");
        }

        Integer idOrigen = transferencia.getSucursalOrigen().getIdSucursal();
        List<DetalleTransferencia> detalles = detalleTransferenciaRepository
                .findByTransferenciaIdTransferencia(transferencia.getIdTransferencia());

        // Validar stock disponible
        log.debug("Validando stock disponible en sucursal origen");
        validarStockDisponible(idOrigen, detalles);

        // Registrar movimientos de reserva (SALIDA con TRANSFERENCIA_SALIDA)
        for (DetalleTransferencia detalle : detalles) {
            registrarMovimientoTransferenciaOrigen(transferencia, detalle);
            detalle.setCantidadEnviada(detalle.getCantidadSolicitada());
            detalleTransferenciaRepository.save(detalle);

            log.debug("Stock reservado - Producto ID: {}, Cantidad: {}",
                     detalle.getProducto().getIdProducto(),
                     detalle.getCantidadSolicitada());
        }

        transferencia.setEstado(EstadoTransferencia.APROBADO);
        Transferencia transferenciaActualizada = transferenciaRepository.save(transferencia);

        log.info("Transferencia aprobada exitosamente - ID: {}, Estado: {}",
                 transferenciaActualizada.getIdTransferencia(),
                 EstadoTransferencia.APROBADO);

        return transferenciaActualizada;
    }

    /**
     * Registra el envío de una transferencia.
     * Actualiza información de envío y cambia estado a ENVIADO.
     *
     * @param id ID de la transferencia
     * @param nombreTransportista Nombre del transportista
     * @param fechaEstimada Fecha estimada de llegada
     * @return Transferencia con estado ENVIADO
     * @throws ResourceNotFoundException si la transferencia no existe
     * @throws IllegalArgumentException si no está en estado APROBADO
     */
    @Transactional
    public Transferencia shipTransfer(Integer id, String nombreTransportista, LocalDateTime fechaEstimada) {
        log.info("Registrando envío de transferencia ID: {} - Transportista: {}", id, nombreTransportista);

        Transferencia transferencia = getTransferById(id);

        if (transferencia.getEstado() != EstadoTransferencia.APROBADO) {
            log.warn("Transferencia no está en estado APROBADO - ID: {}, Estado: {}", id,
                    transferencia.getEstado());
            throw new IllegalArgumentException("La transferencia debe estar en estado APROBADO");
        }

        transferencia.setEstado(EstadoTransferencia.ENVIADO);
        transferencia.setFechaEnvio(LocalDateTime.now());
        transferencia.setFechaEstimada(fechaEstimada);
        transferencia.setNotas((transferencia.getNotas() != null ? transferencia.getNotas() + " | " : "") +
                "Enviado por: " + nombreTransportista);

        Transferencia transferenciaActualizada = transferenciaRepository.save(transferencia);

        log.info("Envío registrado exitosamente - ID: {}, Transportista: {}, Fecha estimada: {}",
                 id, nombreTransportista, fechaEstimada);

        return transferenciaActualizada;
    }

    /**
     * Recibe una transferencia registrando las cantidades recibidas.
     * Valida que las cantidades recibidas sean menores o iguales a las enviadas.
     * Registra movimientos de ingreso en sucursal destino.
     * Cambia estado a RECIBIDO (completo) o PARCIAL (si hay faltantes).
     *
     * @param id ID de la transferencia
     * @param receivedQuantities Mapa de idProducto -> cantidadRecibida
     * @return Transferencia con estado RECIBIDO o PARCIAL
     * @throws ResourceNotFoundException si la transferencia no existe
     * @throws IllegalArgumentException si no está en estado ENVIADO
     * @throws InsufficientStockException si cantidad recibida > cantidad enviada
     */
    @Transactional
    public Transferencia receiveTransfer(Integer id, Map<Integer, Integer> receivedQuantities) {
        log.info("Iniciando recepción de transferencia ID: {}", id);

        Transferencia transferencia = getTransferById(id);

        if (transferencia.getEstado() != EstadoTransferencia.ENVIADO) {
            log.warn("Transferencia no está en estado ENVIADO - ID: {}, Estado: {}", id,
                    transferencia.getEstado());
            throw new IllegalArgumentException("La transferencia debe estar en estado ENVIADO");
        }

        List<DetalleTransferencia> detalles = detalleTransferenciaRepository
                .findByTransferenciaIdTransferencia(transferencia.getIdTransferencia());

        boolean hayFaltantes = false;
        List<DetalleTransferencia> faltantes = new ArrayList<>();

        // Procesar cada detalle
        for (DetalleTransferencia detalle : detalles) {
            Integer productoId = detalle.getProducto().getIdProducto();
            Integer cantidadRecibida = receivedQuantities.getOrDefault(productoId, 0);

            // Validar que no se reciba más de lo enviado
            if (cantidadRecibida > detalle.getCantidadEnviada()) {
                log.warn("Cantidad recibida excede cantidad enviada - Producto ID: {}, " +
                        "Enviada: {}, Recibida: {}",
                        productoId, detalle.getCantidadEnviada(), cantidadRecibida);
                throw new InsufficientStockException(
                        "Cantidad recibida (" + cantidadRecibida +
                        ") excede cantidad enviada (" + detalle.getCantidadEnviada() + ")");
            }

            if (cantidadRecibida < detalle.getCantidadEnviada()) {
                hayFaltantes = true;
                faltantes.add(detalle);
                detalle.setAccionFaltante("PENDIENTE");
            }

            // Registrar movimiento de ingreso en sucursal destino
            if (cantidadRecibida > 0) {
                registrarMovimientoTransferenciaDestino(transferencia, detalle, cantidadRecibida);
            }

            detalle.setCantidadRecibida(cantidadRecibida);
            detalleTransferenciaRepository.save(detalle);

            log.debug("Detalle de transferencia procesado - Producto ID: {}, Cantidad recibida: {}",
                     productoId, cantidadRecibida);
        }

        // Determinar estado final
        EstadoTransferencia estadoFinal = hayFaltantes ? EstadoTransferencia.PARCIAL : EstadoTransferencia.RECIBIDO;
        transferencia.setEstado(estadoFinal);
        transferencia.setFechaRecepcion(LocalDateTime.now());

        Transferencia transferenciaActualizada = transferenciaRepository.save(transferencia);

        // Publicar alerta si hay faltantes
        if (hayFaltantes) {
            publicarAlertaFaltantes(transferenciaActualizada, faltantes);
        }

        log.info("Transferencia recibida exitosamente - ID: {}, Estado: {}, Faltantes: {}",
                 transferenciaActualizada.getIdTransferencia(),
                 estadoFinal,
                 faltantes.size());

        return transferenciaActualizada;
    }

    /**
     * Cancela una transferencia.
     * Solo se puede cancelar si está en estado SOLICITADO o APROBADO.
     * Si está aprobada, revierte los movimientos de reserva.
     *
     * @param id ID de la transferencia
     * @return Transferencia cancelada
     * @throws ResourceNotFoundException si la transferencia no existe
     * @throws IllegalArgumentException si no se puede cancelar desde su estado actual
     */
    @Transactional
    public Transferencia cancelTransfer(Integer id) {
        log.info("Cancelando transferencia ID: {}", id);

        Transferencia transferencia = getTransferById(id);

        if (transferencia.getEstado() == EstadoTransferencia.ENVIADO ||
            transferencia.getEstado() == EstadoTransferencia.RECIBIDO ||
            transferencia.getEstado() == EstadoTransferencia.PARCIAL) {
            log.warn("No se puede cancelar transferencia en estado {} - ID: {}",
                    transferencia.getEstado(), id);
            throw new IllegalArgumentException(
                    "No se puede cancelar una transferencia en estado " + transferencia.getEstado());
        }

        // Si estaba aprobada, revertir movimientos
        if (transferencia.getEstado() == EstadoTransferencia.APROBADO) {
            log.debug("Revirtiendo movimientos de reserva para transferencia ID: {}", id);
            revertirMovimientosTransferencia(id);
        }

        transferencia.setEstado(EstadoTransferencia.CANCELADO);
        Transferencia transferenciaActualizada = transferenciaRepository.save(transferencia);

        log.info("Transferencia cancelada exitosamente - ID: {}", id);

        return transferenciaActualizada;
    }

    /**
     * Valida que haya stock disponible en la sucursal origen para todos los items.
     *
     * @param sucursalId ID de la sucursal
     * @param detalles Lista de detalles de transferencia
     * @throws InsufficientStockException si no hay stock para algún item
     */
    private void validarStockDisponible(Integer sucursalId, List<DetalleTransferencia> detalles) {
        log.debug("Validando stock disponible en sucursal ID: {}", sucursalId);

        for (DetalleTransferencia detalle : detalles) {
            Integer productoId = detalle.getProducto().getIdProducto();
            Integer cantidadSolicitada = detalle.getCantidadSolicitada();

            Inventario inventario = inventarioRepository
                    .findBySucursalIdSucursalAndProductoIdProducto(sucursalId, productoId)
                    .orElse(null);

            int stockDisponible = inventario != null ? inventario.getStockActual() : 0;

            if (stockDisponible < cantidadSolicitada) {
                log.warn("Stock insuficiente - Producto ID: {}, Disponible: {}, Solicitado: {}",
                         productoId, stockDisponible, cantidadSolicitada);
                throw new InsufficientStockException(
                        "Stock insuficiente en sucursal origen para producto ID " + productoId +
                        ". Disponible: " + stockDisponible + ", Solicitado: " + cantidadSolicitada);
            }

            log.debug("Stock validado - Producto ID: {}, Disponible: {}", productoId, stockDisponible);
        }
    }

    /**
     * Registra un movimiento de salida en la sucursal origen para una transferencia aprobada.
     * Crea movimiento SALIDA con motivo TRANSFERENCIA_SALIDA.
     *
     * @param transferencia Transferencia que genera el movimiento
     * @param detalle Detalle de transferencia
     */
    private void registrarMovimientoTransferenciaOrigen(Transferencia transferencia, DetalleTransferencia detalle) {
        log.debug("Registrando movimiento de salida - Origen: {}, Producto: {}, Cantidad: {}",
                 transferencia.getSucursalOrigen().getNombre(),
                 detalle.getProducto().getNombre(),
                 detalle.getCantidadSolicitada());

        // Obtener costo promedio
        Inventario inventario = inventarioRepository
                .findBySucursalIdSucursalAndProductoIdProducto(
                        transferencia.getSucursalOrigen().getIdSucursal(),
                        detalle.getProducto().getIdProducto())
                .orElse(null);

        BigDecimal costoUnitario = inventario != null ?
                inventario.getCostoPromedio() : BigDecimal.ZERO;

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .producto(detalle.getProducto())
                .sucursal(transferencia.getSucursalOrigen())
                .usuario(transferencia.getUsuarioSolicita())
                .tipo(TipoMovimiento.SALIDA)
                .motivo(MotivoMovimiento.TRANSFERENCIA_SALIDA)
                .cantidad(detalle.getCantidadSolicitada())
                .costoUnitario(costoUnitario)
                .referenciaId(transferencia.getIdTransferencia())
                .referenciaTipo("TRANSFERENCIA")
                .build();

        inventarioService.registrarMovimiento(movimiento);
    }

    /**
     * Registra un movimiento de ingreso en la sucursal destino para una transferencia recibida.
     * Crea movimiento INGRESO con motivo TRANSFERENCIA_ENTRADA.
     *
     * @param transferencia Transferencia que genera el movimiento
     * @param detalle Detalle de transferencia
     * @param cantidadRecibida Cantidad que se está recibiendo
     */
    private void registrarMovimientoTransferenciaDestino(Transferencia transferencia,
                                                        DetalleTransferencia detalle,
                                                        Integer cantidadRecibida) {
        log.debug("Registrando movimiento de ingreso - Destino: {}, Producto: {}, Cantidad: {}",
                 transferencia.getSucursalDestino().getNombre(),
                 detalle.getProducto().getNombre(),
                 cantidadRecibida);

        // Obtener costo promedio de la sucursal origen
        Inventario inventarioOrigen = inventarioRepository
                .findBySucursalIdSucursalAndProductoIdProducto(
                        transferencia.getSucursalOrigen().getIdSucursal(),
                        detalle.getProducto().getIdProducto())
                .orElse(null);

        BigDecimal costoUnitario = inventarioOrigen != null ?
                inventarioOrigen.getCostoPromedio() : BigDecimal.ZERO;

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .producto(detalle.getProducto())
                .sucursal(transferencia.getSucursalDestino())
                .usuario(transferencia.getUsuarioSolicita())
                .tipo(TipoMovimiento.INGRESO)
                .motivo(MotivoMovimiento.TRANSFERENCIA_ENTRADA)
                .cantidad(cantidadRecibida)
                .costoUnitario(costoUnitario)
                .referenciaId(transferencia.getIdTransferencia())
                .referenciaTipo("TRANSFERENCIA")
                .build();

        inventarioService.registrarMovimiento(movimiento);
    }

    /**
     * Revierte los movimientos de una transferencia cancelada.
     *
     * @param transferenciaId ID de la transferencia
     */
    private void revertirMovimientosTransferencia(Integer transferenciaId) {
        log.debug("Revirtiendo movimientos de transferencia ID: {}", transferenciaId);

        Transferencia transferencia = getTransferById(transferenciaId);
        List<DetalleTransferencia> detalles = detalleTransferenciaRepository
                .findByTransferenciaIdTransferencia(transferenciaId);

        for (DetalleTransferencia detalle : detalles) {
            // Registrar ingreso en sucursal origen (reversa de la salida)
            Inventario inventario = inventarioRepository
                    .findBySucursalIdSucursalAndProductoIdProducto(
                            transferencia.getSucursalOrigen().getIdSucursal(),
                            detalle.getProducto().getIdProducto())
                    .orElse(null);

            BigDecimal costoUnitario = inventario != null ?
                    inventario.getCostoPromedio() : BigDecimal.ZERO;

            MovimientoInventario movimientoReversa = MovimientoInventario.builder()
                    .producto(detalle.getProducto())
                    .sucursal(transferencia.getSucursalOrigen())
                    .usuario(transferencia.getUsuarioSolicita())
                    .tipo(TipoMovimiento.INGRESO)
                    .motivo(MotivoMovimiento.DEVOLUCION)
                    .cantidad(detalle.getCantidadEnviada())
                    .costoUnitario(costoUnitario)
                    .referenciaId(transferenciaId)
                    .referenciaTipo("TRANSFERENCIA_CANCELADA")
                    .build();

            inventarioService.registrarMovimiento(movimientoReversa);

            log.debug("Movimiento revertido - Producto ID: {}, Cantidad: {}",
                     detalle.getProducto().getIdProducto(),
                     detalle.getCantidadEnviada());
        }

        log.info("Movimientos de transferencia revirtidos - Transferencia ID: {}", transferenciaId);
    }

    /**
     * Publica una alerta de faltantes en una transferencia.
     * Este método puede extenderse para integrar con un servicio de alertas.
     *
     * @param transferencia Transferencia con faltantes
     * @param detallesFaltantes Lista de detalles con faltantes
     */
    private void publicarAlertaFaltantes(Transferencia transferencia, List<DetalleTransferencia> detallesFaltantes) {
        log.warn("Publicando alerta de faltantes - Transferencia ID: {}, Items con faltantes: {}",
                 transferencia.getIdTransferencia(),
                 detallesFaltantes.size());

        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Transferencia ").append(transferencia.getIdTransferencia())
                .append(" recibida parcialmente. Faltantes: ");

        for (DetalleTransferencia detalle : detallesFaltantes) {
            int cantidadFaltante = detalle.getCantidadEnviada() - detalle.getCantidadRecibida();
            mensaje.append(detalle.getProducto().getNombre())
                    .append(" (").append(cantidadFaltante).append("), ");
        }

        log.warn("Alerta: {}", mensaje);

        // TODO: Integrar con servicio de alertas (AlertaService)
        // alertaService.crearAlerta(TipoAlerta.FALTANTE_TRANSFERENCIA, mensaje.toString(), transferencia.getSucursalDestino());
    }
}
