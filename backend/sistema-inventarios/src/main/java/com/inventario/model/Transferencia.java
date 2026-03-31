package com.inventario.model;

import com.inventario.model.enums.EstadoTransferencia;
import com.inventario.model.enums.PrioridadTransferencia;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transferencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transferencia")
    private Integer idTransferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_origen", nullable = false)
    private Sucursal sucursalOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_destino", nullable = false)
    private Sucursal sucursalDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_solicita", nullable = false)
    private Usuario usuarioSolicita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_aprueba")
    private Usuario usuarioAprueba;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ruta")
    private Ruta ruta;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoTransferencia estado = EstadoTransferencia.SOLICITADO;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 10)
    private PrioridadTransferencia prioridad = PrioridadTransferencia.NORMAL;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "fecha_solicitud", nullable = false, updatable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_estimada")
    private LocalDateTime fechaEstimada;

    @Column(name = "fecha_recepcion")
    private LocalDateTime fechaRecepcion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        fechaSolicitud = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
