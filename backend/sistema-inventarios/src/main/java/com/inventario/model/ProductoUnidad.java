package com.inventario.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "producto_unidad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoUnidad {

    @EmbeddedId
    private ProductoUnidadId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idProducto")
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idUnidad")
    @JoinColumn(name = "id_unidad", nullable = false)
    private UnidadMedida unidadMedida;

    @Column(name = "factor_conversion", nullable = false, precision = 10, scale = 2)
    private BigDecimal factorConversion = BigDecimal.ONE;

    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal = false;
}
