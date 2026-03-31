-- OptiPlant Inventory Management System - Initial Schema
-- Version 1: Create all base tables

-- ============================================================================
-- Table: sucursal (Branch/Location)
-- ============================================================================
CREATE TABLE sucursal (
    id_sucursal BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    direccion TEXT,
    telefono VARCHAR(20),
    correo VARCHAR(100),
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_sucursal_nombre ON sucursal(nombre);
CREATE INDEX idx_sucursal_estado ON sucursal(estado);

COMMENT ON COLUMN sucursal.nombre IS 'Branch name (e.g., CENTRAL, SUCURSAL_NORTE)';
COMMENT ON COLUMN sucursal.estado IS 'Active status of the branch';

-- ============================================================================
-- Table: usuario (User)
-- ============================================================================
CREATE TABLE usuario (
    id_usuario BIGSERIAL PRIMARY KEY,
    id_sucursal BIGINT NOT NULL REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(100) NOT NULL UNIQUE,
    contrasena_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL, -- ADMIN, GERENTE, OPERADOR
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_usuario_correo ON usuario(correo);
CREATE INDEX idx_usuario_id_sucursal ON usuario(id_sucursal);
CREATE INDEX idx_usuario_rol ON usuario(rol);
CREATE INDEX idx_usuario_activo ON usuario(activo);

COMMENT ON COLUMN usuario.rol IS 'User role: ADMIN, GERENTE, OPERADOR';
COMMENT ON COLUMN usuario.activo IS 'User account status';

-- ============================================================================
-- Table: categoria (Product Category)
-- ============================================================================
CREATE TABLE categoria (
    id_categoria BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_categoria_nombre ON categoria(nombre);

COMMENT ON COLUMN categoria.nombre IS 'Category name (e.g., FRUTAS, VEGETALES)';

-- ============================================================================
-- Table: unidad_medida (Unit of Measurement)
-- ============================================================================
CREATE TABLE unidad_medida (
    id_unidad BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    abreviatura VARCHAR(15) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_unidad_medida_nombre ON unidad_medida(nombre);
CREATE INDEX idx_unidad_medida_abreviatura ON unidad_medida(abreviatura);

COMMENT ON COLUMN unidad_medida.nombre IS 'Unit name (e.g., KILOGRAMOS, LITROS)';
COMMENT ON COLUMN unidad_medida.abreviatura IS 'Unit abbreviation (e.g., KG, L)';

-- ============================================================================
-- Table: producto (Product)
-- ============================================================================
CREATE TABLE producto (
    id_producto BIGSERIAL PRIMARY KEY,
    sku VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    id_categoria BIGINT REFERENCES categoria(id_categoria) ON DELETE SET NULL,
    stock_minimo INTEGER NOT NULL DEFAULT 0,
    stock_maximo INTEGER,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_producto_sku ON producto(sku);
CREATE INDEX idx_producto_nombre ON producto(nombre);
CREATE INDEX idx_producto_id_categoria ON producto(id_categoria);
CREATE INDEX idx_producto_activo ON producto(activo);

COMMENT ON COLUMN producto.sku IS 'Stock Keeping Unit - unique product identifier';
COMMENT ON COLUMN producto.stock_minimo IS 'Minimum stock level for alerts';
COMMENT ON COLUMN producto.stock_maximo IS 'Maximum stock level for purchasing decisions';

-- ============================================================================
-- Table: proveedor (Supplier)
-- ============================================================================
CREATE TABLE proveedor (
    id_proveedor BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    nit VARCHAR(30) UNIQUE,
    contacto VARCHAR(150),
    correo VARCHAR(100),
    telefono VARCHAR(20),
    condicion_pago VARCHAR(100),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_proveedor_nombre ON proveedor(nombre);
CREATE INDEX idx_proveedor_nit ON proveedor(nit);
CREATE INDEX idx_proveedor_activo ON proveedor(activo);

COMMENT ON COLUMN proveedor.nit IS 'Tax Identification Number';
COMMENT ON COLUMN proveedor.condicion_pago IS 'Payment terms';

-- ============================================================================
-- Table: inventario (Inventory)
-- ============================================================================
CREATE TABLE inventario (
    id_inventario BIGSERIAL PRIMARY KEY,
    id_producto BIGINT NOT NULL REFERENCES producto(id_producto) ON DELETE RESTRICT,
    id_sucursal BIGINT NOT NULL REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    stock_actual INTEGER NOT NULL DEFAULT 0,
    stock_minimo INTEGER,
    costo_promedio NUMERIC(14, 4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    UNIQUE (id_producto, id_sucursal)
);

CREATE INDEX idx_inventario_id_producto ON inventario(id_producto);
CREATE INDEX idx_inventario_id_sucursal ON inventario(id_sucursal);
CREATE INDEX idx_inventario_stock_actual ON inventario(stock_actual);

COMMENT ON COLUMN inventario.stock_actual IS 'Current stock quantity for this product at this branch';
COMMENT ON COLUMN inventario.costo_promedio IS 'Weighted average cost per unit';

-- ============================================================================
-- Table: movimiento_inventario (Inventory Movement)
-- ============================================================================
CREATE TABLE movimiento_inventario (
    id_movimiento BIGSERIAL PRIMARY KEY,
    id_inventario BIGINT REFERENCES inventario(id_inventario) ON DELETE SET NULL,
    id_producto BIGINT NOT NULL REFERENCES producto(id_producto) ON DELETE RESTRICT,
    id_sucursal BIGINT NOT NULL REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    id_usuario BIGINT NOT NULL REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    tipo VARCHAR(10) NOT NULL, -- INGRESO, SALIDA
    motivo VARCHAR(30) NOT NULL, -- COMPRA, VENTA, AJUSTE, DEVOLUCION, TRANSFERENCIA_ENTRADA, TRANSFERENCIA_SALIDA, MERMA
    cantidad INTEGER NOT NULL,
    costo_unitario NUMERIC(14, 4) NOT NULL DEFAULT 0.0000,
    referencia_id BIGINT,
    referencia_tipo VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(255)
);

CREATE INDEX idx_movimiento_inventario_id_producto ON movimiento_inventario(id_producto);
CREATE INDEX idx_movimiento_inventario_id_sucursal ON movimiento_inventario(id_sucursal);
CREATE INDEX idx_movimiento_inventario_id_usuario ON movimiento_inventario(id_usuario);
CREATE INDEX idx_movimiento_inventario_tipo ON movimiento_inventario(tipo);
CREATE INDEX idx_movimiento_inventario_motivo ON movimiento_inventario(motivo);
CREATE INDEX idx_movimiento_inventario_fecha ON movimiento_inventario(fecha);

COMMENT ON COLUMN movimiento_inventario.tipo IS 'Movement type: INGRESO (in) or SALIDA (out)';
COMMENT ON COLUMN movimiento_inventario.motivo IS 'Reason for movement: COMPRA, VENTA, AJUSTE, DEVOLUCION, TRANSFERENCIA_ENTRADA, TRANSFERENCIA_SALIDA, MERMA';
COMMENT ON COLUMN movimiento_inventario.referencia_id IS 'Foreign key to reference table (compra, venta, transferencia)';
COMMENT ON COLUMN movimiento_inventario.referencia_tipo IS 'Type of reference (COMPRA, VENTA, TRANSFERENCIA)';

-- ============================================================================
-- Table: compra (Purchase Order)
-- ============================================================================
CREATE TABLE compra (
    id_compra BIGSERIAL PRIMARY KEY,
    id_proveedor BIGINT NOT NULL REFERENCES proveedor(id_proveedor) ON DELETE RESTRICT,
    id_sucursal BIGINT NOT NULL REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    id_usuario BIGINT NOT NULL REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    estado VARCHAR(25) NOT NULL DEFAULT 'BORRADOR', -- BORRADOR, CONFIRMADO, RECIBIDO, CANCELADO
    descuento_pct NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    dias_pago INTEGER NOT NULL DEFAULT 30,
    total NUMERIC(14, 2),
    notas TEXT,
    fecha TIMESTAMPTZ NOT NULL,
    fecha_recepcion TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_compra_id_proveedor ON compra(id_proveedor);
CREATE INDEX idx_compra_id_sucursal ON compra(id_sucursal);
CREATE INDEX idx_compra_estado ON compra(estado);
CREATE INDEX idx_compra_fecha ON compra(fecha);

COMMENT ON COLUMN compra.estado IS 'Purchase order status: BORRADOR, CONFIRMADO, RECIBIDO, CANCELADO';
COMMENT ON COLUMN compra.descuento_pct IS 'Discount percentage applied to purchase';
COMMENT ON COLUMN compra.dias_pago IS 'Payment terms in days';

-- ============================================================================
-- Table: detalle_compra (Purchase Order Detail)
-- ============================================================================
CREATE TABLE detalle_compra (
    id_detalle BIGSERIAL PRIMARY KEY,
    id_compra BIGINT NOT NULL REFERENCES compra(id_compra) ON DELETE CASCADE,
    id_producto BIGINT NOT NULL REFERENCES producto(id_producto) ON DELETE RESTRICT,
    cantidad INTEGER NOT NULL,
    precio_unitario NUMERIC(14, 4) NOT NULL,
    descuento NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    cantidad_recibida INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_detalle_compra_id_compra ON detalle_compra(id_compra);
CREATE INDEX idx_detalle_compra_id_producto ON detalle_compra(id_producto);

COMMENT ON COLUMN detalle_compra.cantidad_recibida IS 'Quantity received so far (for partial receipts)';

-- ============================================================================
-- Table: venta (Sales Order)
-- ============================================================================
CREATE TABLE venta (
    id_venta BIGSERIAL PRIMARY KEY,
    id_sucursal BIGINT NOT NULL REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    id_usuario BIGINT NOT NULL REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE, CONFIRMADO, ENVIADO, ENTREGADO, CANCELADO
    lista_precios VARCHAR(60) NOT NULL DEFAULT 'LISTA_1',
    subtotal NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    descuento_pct NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    total NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    notas TEXT,
    fecha TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_venta_id_sucursal ON venta(id_sucursal);
CREATE INDEX idx_venta_estado ON venta(estado);
CREATE INDEX idx_venta_fecha ON venta(fecha);

COMMENT ON COLUMN venta.estado IS 'Sales order status: PENDIENTE, CONFIRMADO, ENVIADO, ENTREGADO, CANCELADO';
COMMENT ON COLUMN venta.lista_precios IS 'Price list used for this sale';

-- ============================================================================
-- Table: detalle_venta (Sales Order Detail)
-- ============================================================================
CREATE TABLE detalle_venta (
    id_detalle BIGSERIAL PRIMARY KEY,
    id_venta BIGINT NOT NULL REFERENCES venta(id_venta) ON DELETE CASCADE,
    id_producto BIGINT NOT NULL REFERENCES producto(id_producto) ON DELETE RESTRICT,
    cantidad INTEGER NOT NULL,
    precio NUMERIC(14, 4) NOT NULL,
    descuento NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    total_linea NUMERIC(14, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_detalle_venta_id_venta ON detalle_venta(id_venta);
CREATE INDEX idx_detalle_venta_id_producto ON detalle_venta(id_producto);

-- ============================================================================
-- Table: ruta (Transport Route)
-- ============================================================================
CREATE TABLE ruta (
    id_ruta BIGSERIAL PRIMARY KEY,
    sucursal_origen BIGINT NOT NULL REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    sucursal_destino BIGINT NOT NULL REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    dias_promedio INTEGER NOT NULL DEFAULT 1,
    costo_promedio NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    UNIQUE (sucursal_origen, sucursal_destino)
);

CREATE INDEX idx_ruta_sucursal_origen ON ruta(sucursal_origen);
CREATE INDEX idx_ruta_sucursal_destino ON ruta(sucursal_destino);
CREATE INDEX idx_ruta_activa ON ruta(activa);

COMMENT ON COLUMN ruta.dias_promedio IS 'Average transit days from origin to destination';
COMMENT ON COLUMN ruta.costo_promedio IS 'Average transportation cost';

-- ============================================================================
-- Table: transferencia (Stock Transfer)
-- ============================================================================
CREATE TABLE transferencia (
    id_transferencia BIGSERIAL PRIMARY KEY,
    sucursal_origen BIGINT NOT NULL REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    sucursal_destino BIGINT NOT NULL REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    id_usuario_solicita BIGINT NOT NULL REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    id_usuario_aprueba BIGINT REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    id_ruta BIGINT REFERENCES ruta(id_ruta) ON DELETE SET NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'SOLICITADO', -- SOLICITADO, APROBADO, ENVIADO, RECIBIDO, RECHAZADO
    prioridad VARCHAR(10) NOT NULL DEFAULT 'NORMAL', -- BAJA, NORMAL, ALTA, URGENTE
    notas TEXT,
    fecha_solicitud TIMESTAMPTZ NOT NULL,
    fecha_envio TIMESTAMPTZ,
    fecha_estimada TIMESTAMPTZ,
    fecha_recepcion TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_transferencia_sucursal_origen ON transferencia(sucursal_origen);
CREATE INDEX idx_transferencia_sucursal_destino ON transferencia(sucursal_destino);
CREATE INDEX idx_transferencia_estado ON transferencia(estado);
CREATE INDEX idx_transferencia_prioridad ON transferencia(prioridad);
CREATE INDEX idx_transferencia_fecha_solicitud ON transferencia(fecha_solicitud);

COMMENT ON COLUMN transferencia.estado IS 'Transfer status: SOLICITADO, APROBADO, ENVIADO, RECIBIDO, RECHAZADO';
COMMENT ON COLUMN transferencia.prioridad IS 'Transfer priority: BAJA, NORMAL, ALTA, URGENTE';

-- ============================================================================
-- Table: detalle_transferencia (Transfer Detail)
-- ============================================================================
CREATE TABLE detalle_transferencia (
    id_detalle BIGSERIAL PRIMARY KEY,
    id_transferencia BIGINT NOT NULL REFERENCES transferencia(id_transferencia) ON DELETE CASCADE,
    id_producto BIGINT NOT NULL REFERENCES producto(id_producto) ON DELETE RESTRICT,
    cantidad_solicitada INTEGER NOT NULL,
    cantidad_enviada INTEGER NOT NULL DEFAULT 0,
    cantidad_recibida INTEGER NOT NULL DEFAULT 0,
    accion_faltante VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE, RECHAZADA, PARCIAL
    notas TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_detalle_transferencia_id_transferencia ON detalle_transferencia(id_transferencia);
CREATE INDEX idx_detalle_transferencia_id_producto ON detalle_transferencia(id_producto);

COMMENT ON COLUMN detalle_transferencia.accion_faltante IS 'Action for missing quantity: PENDIENTE, RECHAZADA, PARCIAL';

-- ============================================================================
-- Table: alerta (Alert)
-- ============================================================================
CREATE TABLE alerta (
    id_alerta BIGSERIAL PRIMARY KEY,
    id_sucursal BIGINT NOT NULL REFERENCES sucursal(id_sucursal) ON DELETE CASCADE,
    id_producto BIGINT REFERENCES producto(id_producto) ON DELETE CASCADE,
    tipo_alerta VARCHAR(25) NOT NULL, -- STOCK_BAJO, STOCK_ALTO, VENCIMIENTO, COMPRA_PENDIENTE, TRANSFERENCIA_RETRASADA
    mensaje VARCHAR(500) NOT NULL,
    resuelta BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resuelto_en TIMESTAMPTZ,
    created_by VARCHAR(255)
);

CREATE INDEX idx_alerta_id_sucursal ON alerta(id_sucursal);
CREATE INDEX idx_alerta_id_producto ON alerta(id_producto);
CREATE INDEX idx_alerta_tipo_alerta ON alerta(tipo_alerta);
CREATE INDEX idx_alerta_resuelta ON alerta(resuelta);
CREATE INDEX idx_alerta_created_at ON alerta(created_at);

COMMENT ON COLUMN alerta.tipo_alerta IS 'Alert type: STOCK_BAJO, STOCK_ALTO, VENCIMIENTO, COMPRA_PENDIENTE, TRANSFERENCIA_RETRASADA';

-- ============================================================================
-- Trigger Function for updated_at Auto-Update
-- ============================================================================
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to all tables that have updated_at column
CREATE TRIGGER trigger_update_sucursal BEFORE UPDATE ON sucursal
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_usuario BEFORE UPDATE ON usuario
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_categoria BEFORE UPDATE ON categoria
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_unidad_medida BEFORE UPDATE ON unidad_medida
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_producto BEFORE UPDATE ON producto
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_proveedor BEFORE UPDATE ON proveedor
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_inventario BEFORE UPDATE ON inventario
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_compra BEFORE UPDATE ON compra
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_detalle_compra BEFORE UPDATE ON detalle_compra
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_venta BEFORE UPDATE ON venta
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_detalle_venta BEFORE UPDATE ON detalle_venta
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_ruta BEFORE UPDATE ON ruta
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_transferencia BEFORE UPDATE ON transferencia
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trigger_update_detalle_transferencia BEFORE UPDATE ON detalle_transferencia
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
