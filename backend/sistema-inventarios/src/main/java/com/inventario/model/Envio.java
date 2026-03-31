package com.inventario.model;
import com.inventario.model.enums.EstadoEnvio;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "envio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_envio")
    private Integer idEnvio;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transferencia", nullable = false, unique = true)
    private Transferencia transferencia;

    @Column(name = "transportista", length = 100)
    private String transportista;

    @Column(name = "tiempo_estimado")
    private Integer tiempoEstimado;

    @Column(name = "tiempo_real")
    private Integer tiempoReal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEnvio estado = EstadoEnvio.EN_PREPARACION;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
        actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}
