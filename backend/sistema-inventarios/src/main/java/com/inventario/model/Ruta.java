package com.inventario.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ruta",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sucursal_origen", "sucursal_destino"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta")
    private Integer idRuta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_origen", nullable = false)
    private Sucursal sucursalOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_destino", nullable = false)
    private Sucursal sucursalDestino;

    @Column(name = "dias_promedio", nullable = false)
    private Integer diasPromedio = 1;

    @Column(name = "costo_promedio", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoPromedio = BigDecimal.ZERO;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }
}
