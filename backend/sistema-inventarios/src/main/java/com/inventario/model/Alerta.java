package com.inventario.model;

import com.inventario.model.enums.TipoAlerta;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Integer idAlerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto")
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_alerta", nullable = false, length = 25)
    private TipoAlerta tipoAlerta;

    @Column(name = "mensaje", nullable = false, length = 500)
    private String mensaje;

    @Column(name = "resuelta", nullable = false)
    private Boolean resuelta = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resuelto_en")
    private LocalDateTime resueltoEn;

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
