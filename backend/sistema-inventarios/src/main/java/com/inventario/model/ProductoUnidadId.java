package com.inventario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProductoUnidadId implements Serializable {

    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "id_unidad")
    private Integer idUnidad;
}
