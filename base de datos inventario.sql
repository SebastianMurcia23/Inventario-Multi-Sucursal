-- =====================================
-- ENUMS
-- =====================================

CREATE TYPE rol_usuario AS ENUM ('ADMIN', 'GERENTE', 'OPERADOR');

CREATE TYPE tipo_movimiento AS ENUM ('INGRESO', 'SALIDA');

CREATE TYPE motivo_movimiento AS ENUM (
    'COMPRA',
    'VENTA',
    'AJUSTE',
    'TRANSFERENCIA_ENTRADA',
    'TRANSFERENCIA_SALIDA'
);

CREATE TYPE estado_transferencia AS ENUM (
    'SOLICITADO',
    'APROBADO',
    'ENVIADO',
    'RECIBIDO',
    'PARCIAL'
);

CREATE TYPE estado_envio AS ENUM (
    'EN_PREPARACION',
    'EN_TRANSITO',
    'ENTREGADO',
    'CON_FALTANTES'
);

-- =====================================
-- TABLAS BASE
-- =====================================

CREATE TABLE sucursal (
    id_sucursal SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    direccion TEXT,
    estado BOOLEAN DEFAULT TRUE
);

CREATE TABLE usuario (
    id_usuario SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    rol rol_usuario NOT NULL,
    id_sucursal INT,
    FOREIGN KEY (id_sucursal) REFERENCES sucursal(id_sucursal)
);

CREATE TABLE producto (
    id_producto SERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    stock_minimo INT DEFAULT 0,
    activo BOOLEAN DEFAULT TRUE
);

CREATE TABLE unidad_medida (
    id_unidad SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL
);

-- =====================================
-- RELACIONES PRODUCTO - UNIDAD (PRO)
-- =====================================

CREATE TABLE producto_unidad (
    id_producto INT,
    id_unidad INT,
    factor_conversion NUMERIC(10,2) DEFAULT 1,
    es_principal BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (id_producto, id_unidad),
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
    FOREIGN KEY (id_unidad) REFERENCES unidad_medida(id_unidad)
);

-- =====================================
-- INVENTARIO
-- =====================================

CREATE TABLE inventario (
    id_inventario SERIAL PRIMARY KEY,
    id_producto INT NOT NULL,
    id_sucursal INT NOT NULL,
    stock_actual INT DEFAULT 0,
    costo_promedio NUMERIC(12,2) DEFAULT 0,

    UNIQUE (id_producto, id_sucursal),

    FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
    FOREIGN KEY (id_sucursal) REFERENCES sucursal(id_sucursal)
);

-- =====================================
-- MOVIMIENTOS (TRAZABILIDAD)
-- =====================================

CREATE TABLE movimiento_inventario (
    id_movimiento SERIAL PRIMARY KEY,
    id_inventario INT,
    id_producto INT NOT NULL,
    id_sucursal INT NOT NULL,
    tipo tipo_movimiento NOT NULL,
    motivo motivo_movimiento NOT NULL,
    cantidad INT NOT NULL,
    referencia_id INT,
    referencia_tipo VARCHAR(50),
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_usuario INT,

    FOREIGN KEY (id_inventario) REFERENCES inventario(id_inventario),
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
    FOREIGN KEY (id_sucursal) REFERENCES sucursal(id_sucursal),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

-- =====================================
-- PROVEEDOR Y COMPRAS
-- =====================================

CREATE TABLE proveedor (
    id_proveedor SERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    contacto VARCHAR(100)
);

CREATE TABLE compra (
    id_compra SERIAL PRIMARY KEY,
    id_proveedor INT NOT NULL,
    id_sucursal INT NOT NULL,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total NUMERIC(12,2),

    FOREIGN KEY (id_proveedor) REFERENCES proveedor(id_proveedor),
    FOREIGN KEY (id_sucursal) REFERENCES sucursal(id_sucursal)
);

CREATE TABLE detalle_compra (
    id_detalle SERIAL PRIMARY KEY,
    id_compra INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario NUMERIC(12,2),
    descuento NUMERIC(5,2) DEFAULT 0,

    FOREIGN KEY (id_compra) REFERENCES compra(id_compra) ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);

-- =====================================
-- VENTAS
-- =====================================

CREATE TABLE venta (
    id_venta SERIAL PRIMARY KEY,
    id_sucursal INT NOT NULL,
    id_usuario INT,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total NUMERIC(12,2),

    FOREIGN KEY (id_sucursal) REFERENCES sucursal(id_sucursal),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

CREATE TABLE detalle_venta (
    id_detalle SERIAL PRIMARY KEY,
    id_venta INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    precio NUMERIC(12,2),
    descuento NUMERIC(5,2) DEFAULT 0,

    FOREIGN KEY (id_venta) REFERENCES venta(id_venta) ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);

-- =====================================
-- TRANSFERENCIAS
-- =====================================

CREATE TABLE transferencia (
    id_transferencia SERIAL PRIMARY KEY,
    sucursal_origen INT NOT NULL,
    sucursal_destino INT NOT NULL,
    estado estado_transferencia DEFAULT 'SOLICITADO',
    fecha_solicitud TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_envio TIMESTAMP,
    fecha_recepcion TIMESTAMP,
    id_usuario_solicita INT,
    id_usuario_aprueba INT,

    FOREIGN KEY (sucursal_origen) REFERENCES sucursal(id_sucursal),
    FOREIGN KEY (sucursal_destino) REFERENCES sucursal(id_sucursal),
    FOREIGN KEY (id_usuario_solicita) REFERENCES usuario(id_usuario),
    FOREIGN KEY (id_usuario_aprueba) REFERENCES usuario(id_usuario)
);

CREATE TABLE detalle_transferencia (
    id_detalle SERIAL PRIMARY KEY,
    id_transferencia INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad_enviada INT,
    cantidad_recibida INT,

    FOREIGN KEY (id_transferencia) REFERENCES transferencia(id_transferencia) ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);

-- =====================================
-- ENVÍOS
-- =====================================

CREATE TABLE envio (
    id_envio SERIAL PRIMARY KEY,
    id_transferencia INT UNIQUE,
    transportista VARCHAR(100),
    tiempo_estimado INT,
    tiempo_real INT,
    estado estado_envio,

    FOREIGN KEY (id_transferencia) REFERENCES transferencia(id_transferencia)
);

-- =====================================
-- ÍNDICES (PERFORMANCE)
-- =====================================

CREATE INDEX idx_inventario_producto ON inventario(id_producto);
CREATE INDEX idx_inventario_sucursal ON inventario(id_sucursal);

CREATE INDEX idx_movimiento_producto ON movimiento_inventario(id_producto);
CREATE INDEX idx_movimiento_fecha ON movimiento_inventario(fecha);

CREATE INDEX idx_venta_fecha ON venta(fecha);
CREATE INDEX idx_compra_fecha ON compra(fecha);