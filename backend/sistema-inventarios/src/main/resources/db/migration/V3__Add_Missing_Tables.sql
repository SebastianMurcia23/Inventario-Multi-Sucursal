-- OptiPlant Inventory Management System - Missing Tables
-- Version 3: Add missing tables for Envio, ProductoUnidad, ProductoProveedor

-- ============================================================================
-- Table: producto_unidad (Product Unit of Measurement)
-- ============================================================================
CREATE TABLE producto_unidad (
    id_producto BIGINT NOT NULL REFERENCES producto(id_producto) ON DELETE CASCADE,
    id_unidad BIGINT NOT NULL REFERENCES unidad_medida(id_unidad) ON DELETE CASCADE,
    factor_conversion NUMERIC(10, 2) NOT NULL DEFAULT 1.00,
    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    PRIMARY KEY (id_producto, id_unidad)
);

CREATE INDEX idx_producto_unidad_id_producto ON producto_unidad(id_producto);
CREATE INDEX idx_producto_unidad_id_unidad ON producto_unidad(id_unidad);

COMMENT ON COLUMN producto_unidad.factor_conversion IS 'Conversion factor from base unit';
COMMENT ON COLUMN producto_unidad.es_principal IS 'Whether this is the primary unit for the product';

-- ============================================================================
-- Table: producto_proveedor (Product Supplier)
-- ============================================================================
CREATE TABLE producto_proveedor (
    id_producto_proveedor BIGSERIAL PRIMARY KEY,
    id_producto BIGINT NOT NULL REFERENCES producto(id_producto) ON DELETE CASCADE,
    id_proveedor BIGINT NOT NULL REFERENCES proveedor(id_proveedor) ON DELETE CASCADE,
    precio_unitario NUMERIC(14, 4) NOT NULL,
    dias_entrega INTEGER NOT NULL DEFAULT 1,
    preferido BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    UNIQUE (id_producto, id_proveedor)
);

CREATE INDEX idx_producto_proveedor_id_producto ON producto_proveedor(id_producto);
CREATE INDEX idx_producto_proveedor_id_proveedor ON producto_proveedor(id_proveedor);

COMMENT ON COLUMN producto_proveedor.dias_entrega IS 'Average delivery days from this supplier';
COMMENT ON COLUMN producto_proveedor.preferido IS 'Whether this is the preferred supplier for the product';

-- ============================================================================
-- Table: envio (Shipment)
-- ============================================================================
CREATE TABLE envio (
    id_envio BIGSERIAL PRIMARY KEY,
    id_transferencia BIGINT NOT NULL UNIQUE REFERENCES transferencia(id_transferencia) ON DELETE CASCADE,
    transportista VARCHAR(100),
    tiempo_estimado INTEGER,
    tiempo_real INTEGER,
    estado VARCHAR(20) NOT NULL DEFAULT 'EN_PREPARACION',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_envio_id_transferencia ON envio(id_transferencia);
CREATE INDEX idx_envio_estado ON envio(estado);

COMMENT ON COLUMN envio.tiempo_estimado IS 'Estimated transit time in days';
COMMENT ON COLUMN envio.tiempo_real IS 'Actual transit time in days';
COMMENT ON COLUMN envio.estado IS 'Shipment status: EN_PREPARACION, EN_TRANSITO, ENTREGADO';

-- Apply update_timestamp trigger to new tables
CREATE TRIGGER trigger_update_producto_unidad BEFORE UPDATE ON producto_unidad
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_producto_proveedor BEFORE UPDATE ON producto_proveedor
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_envio BEFORE UPDATE ON envio
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
