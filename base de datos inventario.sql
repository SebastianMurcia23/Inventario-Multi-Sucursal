-- ============================================================
-- SISTEMA DE INVENTARIO MULTI-SUCURSAL
-- Script completo: DROP + CREATE
-- OptiPlant Consultores - Prueba Técnica
-- ============================================================

-- ============================================================
-- 1. LIMPIEZA TOTAL (orden inverso a las dependencias)
-- ============================================================

DROP VIEW  IF EXISTS v_historial_movimientos    CASCADE;
DROP VIEW  IF EXISTS v_transferencias_activas   CASCADE;
DROP VIEW  IF EXISTS v_ventas_por_mes           CASCADE;
DROP VIEW  IF EXISTS v_stock_con_alertas        CASCADE;

DROP TABLE IF EXISTS alerta                     CASCADE;
DROP TABLE IF EXISTS envio                      CASCADE;
DROP TABLE IF EXISTS detalle_transferencia      CASCADE;
DROP TABLE IF EXISTS transferencia              CASCADE;
DROP TABLE IF EXISTS detalle_venta              CASCADE;
DROP TABLE IF EXISTS venta                      CASCADE;
DROP TABLE IF EXISTS detalle_compra             CASCADE;
DROP TABLE IF EXISTS compra                     CASCADE;
DROP TABLE IF EXISTS movimiento_inventario      CASCADE;
DROP TABLE IF EXISTS inventario                 CASCADE;
DROP TABLE IF EXISTS producto_proveedor         CASCADE;
DROP TABLE IF EXISTS producto_unidad            CASCADE;
DROP TABLE IF EXISTS producto                   CASCADE;
DROP TABLE IF EXISTS unidad_medida              CASCADE;
DROP TABLE IF EXISTS categoria                  CASCADE;
DROP TABLE IF EXISTS ruta                       CASCADE;
DROP TABLE IF EXISTS proveedor                  CASCADE;
DROP TABLE IF EXISTS usuario                    CASCADE;
DROP TABLE IF EXISTS sucursal                   CASCADE;

DROP FUNCTION IF EXISTS fn_actualizar_timestamp()        CASCADE;
DROP FUNCTION IF EXISTS fn_calcular_costo_promedio(INT, INT, NUMERIC, NUMERIC) CASCADE;
DROP FUNCTION IF EXISTS fn_verificar_stock(INT, INT, NUMERIC) CASCADE;

DROP TYPE IF EXISTS rol_usuario             CASCADE;
DROP TYPE IF EXISTS tipo_movimiento         CASCADE;
DROP TYPE IF EXISTS motivo_movimiento       CASCADE;
DROP TYPE IF EXISTS estado_transferencia    CASCADE;
DROP TYPE IF EXISTS estado_envio            CASCADE;
DROP TYPE IF EXISTS estado_compra           CASCADE;
DROP TYPE IF EXISTS estado_venta            CASCADE;
DROP TYPE IF EXISTS tipo_alerta             CASCADE;
DROP TYPE IF EXISTS prioridad_transferencia CASCADE;

-- ============================================================
-- 2. TIPOS ENUMERADOS
-- ============================================================

CREATE TYPE rol_usuario AS ENUM (
    'ADMIN',
    'GERENTE',
    'OPERADOR'
);

CREATE TYPE tipo_movimiento AS ENUM (
    'INGRESO',
    'SALIDA'
);

CREATE TYPE motivo_movimiento AS ENUM (
    'COMPRA',
    'VENTA',
    'AJUSTE',
    'DEVOLUCION',
    'TRANSFERENCIA_ENTRADA',
    'TRANSFERENCIA_SALIDA',
    'MERMA'
);

CREATE TYPE estado_compra AS ENUM (
    'BORRADOR',
    'ENVIADA',
    'CONFIRMADA',
    'RECIBIDA_COMPLETA',
    'RECIBIDA_PARCIAL',
    'CANCELADA'
);

CREATE TYPE estado_venta AS ENUM (
    'PENDIENTE',
    'CONFIRMADA',
    'ANULADA'
);

CREATE TYPE estado_transferencia AS ENUM (
    'SOLICITADO',
    'APROBADO',
    'ENVIADO',
    'RECIBIDO',
    'PARCIAL',
    'CANCELADO'
);

CREATE TYPE estado_envio AS ENUM (
    'EN_PREPARACION',
    'EN_TRANSITO',
    'ENTREGADO',
    'CON_FALTANTES'
);

CREATE TYPE tipo_alerta AS ENUM (
    'STOCK_MINIMO',
    'STOCK_AGOTADO',
    'RECEPCION_PARCIAL',
    'REABASTECIMIENTO'
);

CREATE TYPE prioridad_transferencia AS ENUM (
    'BAJA',
    'NORMAL',
    'ALTA',
    'URGENTE'
);

-- ============================================================
-- 3. TABLAS BASE
-- ============================================================

CREATE TABLE sucursal (
    id_sucursal     SERIAL PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    direccion       TEXT,
    telefono        VARCHAR(20),
    correo          VARCHAR(100),
    estado          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  sucursal         IS 'Nodos de la red. Cada sucursal opera de forma autónoma.';
COMMENT ON COLUMN sucursal.estado  IS 'FALSE = sucursal inactiva, no recibe ni envía transferencias.';

-- -------------------------------------------------------

CREATE TABLE usuario (
    id_usuario      SERIAL PRIMARY KEY,
    id_sucursal     INT NOT NULL,
    nombre          VARCHAR(100) NOT NULL,
    correo          VARCHAR(100) NOT NULL UNIQUE,
    contrasena_hash VARCHAR(255) NOT NULL,
    rol             rol_usuario NOT NULL,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (id_sucursal) REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT
);

COMMENT ON TABLE  usuario                IS 'Usuarios del sistema. correo + contrasena_hash para autenticación JWT.';
COMMENT ON COLUMN usuario.rol            IS 'ADMIN: visibilidad total. GERENTE: aprueba transferencias. OPERADOR: operaciones diarias.';
COMMENT ON COLUMN usuario.contrasena_hash IS 'Hash bcrypt. Nunca almacenar contraseña en texto plano.';

-- -------------------------------------------------------

CREATE TABLE categoria (
    id_categoria  SERIAL PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL UNIQUE,
    descripcion   VARCHAR(255),
    creado_en     TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE categoria IS 'Clasificación de productos para filtrado y reportes del dashboard.';

-- -------------------------------------------------------

CREATE TABLE unidad_medida (
    id_unidad    SERIAL PRIMARY KEY,
    nombre       VARCHAR(80)  NOT NULL UNIQUE,
    abreviatura  VARCHAR(15)  NOT NULL UNIQUE,
    creado_en    TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE unidad_medida IS 'Unidades de medida: unidad, kg, litro, caja, etc.';

-- -------------------------------------------------------

CREATE TABLE proveedor (
    id_proveedor    SERIAL PRIMARY KEY,
    nombre          VARCHAR(150) NOT NULL,
    nit             VARCHAR(30)  UNIQUE,
    contacto        VARCHAR(150),
    correo          VARCHAR(100),
    telefono        VARCHAR(20),
    condicion_pago  VARCHAR(100),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  proveedor                IS 'Proveedores registrados. Se asocian a productos mediante producto_proveedor.';
COMMENT ON COLUMN proveedor.condicion_pago IS 'Ejemplo: 30 días, 60 días, contado.';

-- -------------------------------------------------------

CREATE TABLE producto (
    id_producto     SERIAL PRIMARY KEY,
    sku             VARCHAR(60)  NOT NULL UNIQUE,
    nombre          VARCHAR(150) NOT NULL,
    descripcion     TEXT,
    id_categoria    INT          REFERENCES categoria(id_categoria) ON DELETE RESTRICT,
    stock_minimo    INT          NOT NULL DEFAULT 0,
    stock_maximo    INT,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP    NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  producto              IS 'Catálogo global de productos. El stock por sucursal vive en la tabla inventario.';
COMMENT ON COLUMN producto.stock_minimo IS 'Umbral mínimo global. La tabla inventario puede sobreescribirlo por sucursal.';

-- -------------------------------------------------------

CREATE TABLE producto_unidad (
    id_producto       INT  NOT NULL,
    id_unidad         INT  NOT NULL,
    factor_conversion NUMERIC(10,2) NOT NULL DEFAULT 1,
    es_principal      BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id_producto, id_unidad),
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto)     ON DELETE CASCADE,
    FOREIGN KEY (id_unidad)   REFERENCES unidad_medida(id_unidad)  ON DELETE RESTRICT
);

COMMENT ON TABLE  producto_unidad                IS 'Múltiples unidades de medida por producto con factor de conversión.';
COMMENT ON COLUMN producto_unidad.es_principal   IS 'TRUE = unidad de medida base del producto.';
COMMENT ON COLUMN producto_unidad.factor_conversion IS 'Cuántas unidades base equivalen a esta unidad.';

-- -------------------------------------------------------

CREATE TABLE producto_proveedor (
    id_producto_proveedor SERIAL PRIMARY KEY,
    id_producto           INT  NOT NULL REFERENCES producto(id_producto)   ON DELETE CASCADE,
    id_proveedor          INT  NOT NULL REFERENCES proveedor(id_proveedor) ON DELETE CASCADE,
    precio_unitario       NUMERIC(14,4) NOT NULL,
    dias_entrega          INT  NOT NULL DEFAULT 1,
    preferido             BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en             TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (id_producto, id_proveedor)
);

COMMENT ON TABLE  producto_proveedor          IS 'Condiciones comerciales por producto y proveedor.';
COMMENT ON COLUMN producto_proveedor.preferido IS 'TRUE = proveedor principal para reabastecimiento automático.';

-- ============================================================
-- 4. INVENTARIO Y TRAZABILIDAD
-- ============================================================

CREATE TABLE inventario (
    id_inventario   SERIAL PRIMARY KEY,
    id_producto     INT  NOT NULL,
    id_sucursal     INT  NOT NULL,
    stock_actual    INT  NOT NULL DEFAULT 0,
    stock_minimo    INT,
    costo_promedio  NUMERIC(14,4) NOT NULL DEFAULT 0,
    actualizado_en  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (id_producto, id_sucursal),
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto)   ON DELETE RESTRICT,
    FOREIGN KEY (id_sucursal) REFERENCES sucursal(id_sucursal)   ON DELETE RESTRICT
);

COMMENT ON TABLE  inventario               IS 'Stock actual por sucursal. Fuente de verdad para disponibilidad. Nunca modificar directamente: usar movimiento_inventario.';
COMMENT ON COLUMN inventario.stock_minimo  IS 'Sobreescribe el stock_minimo global del producto para esta sucursal. NULL = usar el del producto.';
COMMENT ON COLUMN inventario.costo_promedio IS 'Costo promedio ponderado. Se recalcula en cada entrada de mercancía.';

-- -------------------------------------------------------

CREATE TABLE movimiento_inventario (
    id_movimiento   SERIAL PRIMARY KEY,
    id_inventario   INT,
    id_producto     INT  NOT NULL,
    id_sucursal     INT  NOT NULL,
    id_usuario      INT  NOT NULL,
    tipo            tipo_movimiento    NOT NULL,
    motivo          motivo_movimiento  NOT NULL,
    cantidad        INT  NOT NULL,
    costo_unitario  NUMERIC(14,4) NOT NULL DEFAULT 0,
    referencia_id   INT,
    referencia_tipo VARCHAR(50),
    fecha           TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (id_inventario) REFERENCES inventario(id_inventario) ON DELETE RESTRICT,
    FOREIGN KEY (id_producto)   REFERENCES producto(id_producto)     ON DELETE RESTRICT,
    FOREIGN KEY (id_sucursal)   REFERENCES sucursal(id_sucursal)     ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario)    REFERENCES usuario(id_usuario)       ON DELETE RESTRICT
);

COMMENT ON TABLE  movimiento_inventario              IS 'Registro inmutable de todos los movimientos. Solo INSERT, nunca UPDATE. Es el historial auditable.';
COMMENT ON COLUMN movimiento_inventario.costo_unitario IS 'Costo por unidad en el momento del movimiento. Necesario para costo promedio ponderado.';
COMMENT ON COLUMN movimiento_inventario.referencia_id  IS 'ID del documento origen: id_compra, id_venta o id_transferencia.';
COMMENT ON COLUMN movimiento_inventario.referencia_tipo IS 'Tipo del documento origen: COMPRA, VENTA, TRANSFERENCIA.';

-- ============================================================
-- 5. MÓDULO DE COMPRAS
-- ============================================================

CREATE TABLE compra (
    id_compra       SERIAL PRIMARY KEY,
    id_proveedor    INT  NOT NULL,
    id_sucursal     INT  NOT NULL,
    id_usuario      INT  NOT NULL,
    estado          estado_compra NOT NULL DEFAULT 'BORRADOR',
    descuento_pct   NUMERIC(5,2)  NOT NULL DEFAULT 0,
    dias_pago       INT  NOT NULL DEFAULT 30,
    total           NUMERIC(14,2),
    notas           TEXT,
    fecha           TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_recepcion TIMESTAMP,
    actualizado_en  TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (id_proveedor) REFERENCES proveedor(id_proveedor) ON DELETE RESTRICT,
    FOREIGN KEY (id_sucursal)  REFERENCES sucursal(id_sucursal)   ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario)   REFERENCES usuario(id_usuario)     ON DELETE RESTRICT
);

COMMENT ON TABLE  compra        IS 'Cabecera de orden de compra. El inventario se actualiza al pasar a RECIBIDA_COMPLETA o RECIBIDA_PARCIAL.';
COMMENT ON COLUMN compra.estado IS 'Máquina de estados: BORRADOR → ENVIADA → CONFIRMADA → RECIBIDA_COMPLETA / RECIBIDA_PARCIAL.';

-- -------------------------------------------------------

CREATE TABLE detalle_compra (
    id_detalle          SERIAL PRIMARY KEY,
    id_compra           INT  NOT NULL,
    id_producto         INT  NOT NULL,
    cantidad            INT  NOT NULL,
    precio_unitario     NUMERIC(14,4) NOT NULL,
    descuento           NUMERIC(5,2)  NOT NULL DEFAULT 0,
    cantidad_recibida   INT  NOT NULL DEFAULT 0,
    creado_en           TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (id_compra)   REFERENCES compra(id_compra)     ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto) ON DELETE RESTRICT
);

COMMENT ON COLUMN detalle_compra.cantidad_recibida IS 'Puede ser menor que cantidad para registrar recepción parcial.';

-- ============================================================
-- 6. MÓDULO DE VENTAS
-- ============================================================

CREATE TABLE venta (
    id_venta        SERIAL PRIMARY KEY,
    id_sucursal     INT  NOT NULL,
    id_usuario      INT  NOT NULL,
    estado          estado_venta NOT NULL DEFAULT 'PENDIENTE',
    lista_precios   VARCHAR(60)  NOT NULL DEFAULT 'LISTA_1',
    subtotal        NUMERIC(14,2) NOT NULL DEFAULT 0,
    descuento_pct   NUMERIC(5,2)  NOT NULL DEFAULT 0,
    total           NUMERIC(14,2) NOT NULL DEFAULT 0,
    notas           TEXT,
    fecha           TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (id_sucursal) REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario)  REFERENCES usuario(id_usuario)   ON DELETE RESTRICT
);

COMMENT ON TABLE  venta        IS 'Cabecera de venta. El stock se descuenta al confirmar (CONFIRMADA). ANULADA devuelve el stock.';
COMMENT ON COLUMN venta.estado IS 'La validación de stock disponible ocurre ANTES de pasar a CONFIRMADA.';

-- -------------------------------------------------------

CREATE TABLE detalle_venta (
    id_detalle      SERIAL PRIMARY KEY,
    id_venta        INT  NOT NULL,
    id_producto     INT  NOT NULL,
    cantidad        INT  NOT NULL,
    precio          NUMERIC(14,4) NOT NULL,
    descuento       NUMERIC(5,2)  NOT NULL DEFAULT 0,
    total_linea     NUMERIC(14,2) NOT NULL,
    creado_en       TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (id_venta)    REFERENCES venta(id_venta)       ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto) ON DELETE RESTRICT
);

-- ============================================================
-- 7. MÓDULO DE TRANSFERENCIAS Y LOGÍSTICA
-- ============================================================

CREATE TABLE ruta (
    id_ruta          SERIAL PRIMARY KEY,
    sucursal_origen  INT  NOT NULL REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    sucursal_destino INT  NOT NULL REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    dias_promedio    INT  NOT NULL DEFAULT 1,
    costo_promedio   NUMERIC(12,2) NOT NULL DEFAULT 0,
    activa           BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (sucursal_origen, sucursal_destino),
    CHECK  (sucursal_origen <> sucursal_destino)
);

COMMENT ON TABLE ruta IS 'Rutas configuradas entre sucursales con tiempos y costos estimados para el módulo de logística.';

-- -------------------------------------------------------

CREATE TABLE transferencia (
    id_transferencia    SERIAL PRIMARY KEY,
    sucursal_origen     INT  NOT NULL,
    sucursal_destino    INT  NOT NULL,
    id_usuario_solicita INT  NOT NULL,
    id_usuario_aprueba  INT,
    id_ruta             INT,
    estado              estado_transferencia NOT NULL DEFAULT 'SOLICITADO',
    prioridad           prioridad_transferencia NOT NULL DEFAULT 'NORMAL',
    notas               TEXT,
    fecha_solicitud     TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_envio         TIMESTAMP,
    fecha_estimada      TIMESTAMP,
    fecha_recepcion     TIMESTAMP,
    actualizado_en      TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (sucursal_origen)     REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    FOREIGN KEY (sucursal_destino)    REFERENCES sucursal(id_sucursal) ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario_solicita) REFERENCES usuario(id_usuario)   ON DELETE RESTRICT,
    FOREIGN KEY (id_usuario_aprueba)  REFERENCES usuario(id_usuario)   ON DELETE RESTRICT,
    FOREIGN KEY (id_ruta)             REFERENCES ruta(id_ruta)         ON DELETE SET NULL,
    CHECK (sucursal_origen <> sucursal_destino)
);

COMMENT ON TABLE  transferencia        IS 'Traslado de mercancía entre sucursales. Máquina de estados: SOLICITADO → APROBADO → ENVIADO → RECIBIDO / PARCIAL.';
COMMENT ON COLUMN transferencia.prioridad IS 'Permite a la sucursal origen priorizar el despacho según urgencia.';

-- -------------------------------------------------------

CREATE TABLE detalle_transferencia (
    id_detalle           SERIAL PRIMARY KEY,
    id_transferencia     INT  NOT NULL,
    id_producto          INT  NOT NULL,
    cantidad_solicitada  INT  NOT NULL,
    cantidad_enviada     INT  NOT NULL DEFAULT 0,
    cantidad_recibida    INT  NOT NULL DEFAULT 0,
    accion_faltante      VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    notas                TEXT,
    creado_en            TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (id_transferencia) REFERENCES transferencia(id_transferencia) ON DELETE CASCADE,
    FOREIGN KEY (id_producto)      REFERENCES producto(id_producto)           ON DELETE RESTRICT
);

COMMENT ON COLUMN detalle_transferencia.accion_faltante IS 'Acción ante faltante en recepción: REENVIO, AJUSTE, RECLAMACION, PENDIENTE.';

-- -------------------------------------------------------

CREATE TABLE envio (
    id_envio         SERIAL PRIMARY KEY,
    id_transferencia INT  NOT NULL UNIQUE,
    transportista    VARCHAR(100),
    tiempo_estimado  INT,
    tiempo_real      INT,
    estado           estado_envio NOT NULL DEFAULT 'EN_PREPARACION',
    creado_en        TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en   TIMESTAMP NOT NULL DEFAULT NOW(),
    FOREIGN KEY (id_transferencia) REFERENCES transferencia(id_transferencia) ON DELETE CASCADE
);

COMMENT ON TABLE envio IS 'Datos logísticos del despacho. Permite comparar tiempo_estimado vs tiempo_real para reportes de cumplimiento.';

-- ============================================================
-- 8. MÓDULO DE ALERTAS
-- ============================================================

CREATE TABLE alerta (
    id_alerta    SERIAL PRIMARY KEY,
    id_sucursal  INT  NOT NULL REFERENCES sucursal(id_sucursal) ON DELETE CASCADE,
    id_producto  INT  REFERENCES producto(id_producto)          ON DELETE CASCADE,
    tipo_alerta  tipo_alerta NOT NULL,
    mensaje      VARCHAR(500) NOT NULL,
    resuelta     BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en    TIMESTAMP NOT NULL DEFAULT NOW(),
    resuelto_en  TIMESTAMP
);

COMMENT ON TABLE alerta IS 'Alertas automáticas: stock mínimo, stock agotado, recepciones parciales. Generadas por el servicio de alertas en Spring Boot.';

-- ============================================================
-- 9. ÍNDICES DE RENDIMIENTO
-- ============================================================

-- Inventario
CREATE INDEX idx_inventario_producto     ON inventario(id_producto);
CREATE INDEX idx_inventario_sucursal     ON inventario(id_sucursal);
CREATE INDEX idx_inventario_stock_bajo   ON inventario(id_sucursal, stock_actual) WHERE stock_actual <= 0;

-- Movimientos
CREATE INDEX idx_movimiento_producto     ON movimiento_inventario(id_producto);
CREATE INDEX idx_movimiento_sucursal     ON movimiento_inventario(id_sucursal);
CREATE INDEX idx_movimiento_usuario      ON movimiento_inventario(id_usuario);
CREATE INDEX idx_movimiento_fecha        ON movimiento_inventario(fecha DESC);
CREATE INDEX idx_movimiento_tipo         ON movimiento_inventario(tipo);
CREATE INDEX idx_movimiento_motivo       ON movimiento_inventario(motivo);
CREATE INDEX idx_movimiento_referencia   ON movimiento_inventario(referencia_id) WHERE referencia_id IS NOT NULL;

-- Ventas
CREATE INDEX idx_venta_sucursal          ON venta(id_sucursal);
CREATE INDEX idx_venta_fecha             ON venta(fecha DESC);
CREATE INDEX idx_venta_estado            ON venta(estado);
CREATE INDEX idx_detalle_venta_producto  ON detalle_venta(id_producto);

-- Compras
CREATE INDEX idx_compra_sucursal         ON compra(id_sucursal);
CREATE INDEX idx_compra_proveedor        ON compra(id_proveedor);
CREATE INDEX idx_compra_estado           ON compra(estado);
CREATE INDEX idx_compra_fecha            ON compra(fecha DESC);
CREATE INDEX idx_detalle_compra_producto ON detalle_compra(id_producto);

-- Transferencias
CREATE INDEX idx_transferencia_origen    ON transferencia(sucursal_origen);
CREATE INDEX idx_transferencia_destino   ON transferencia(sucursal_destino);
CREATE INDEX idx_transferencia_estado    ON transferencia(estado);
CREATE INDEX idx_transferencia_fecha     ON transferencia(fecha_solicitud DESC);
CREATE INDEX idx_detalle_trans_producto  ON detalle_transferencia(id_producto);

-- Alertas
CREATE INDEX idx_alerta_sucursal         ON alerta(id_sucursal);
CREATE INDEX idx_alerta_no_resuelta      ON alerta(id_sucursal, resuelta) WHERE resuelta = FALSE;

-- Productos y usuarios
CREATE INDEX idx_producto_sku            ON producto(sku);
CREATE INDEX idx_producto_categoria      ON producto(id_categoria);
CREATE INDEX idx_usuario_correo          ON usuario(correo);
CREATE INDEX idx_usuario_sucursal        ON usuario(id_sucursal);

-- ============================================================
-- 10. FUNCIÓN: TIMESTAMP AUTOMÁTICO
-- ============================================================

CREATE OR REPLACE FUNCTION fn_actualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sucursal_ts
    BEFORE UPDATE ON sucursal
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_usuario_ts
    BEFORE UPDATE ON usuario
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_producto_ts
    BEFORE UPDATE ON producto
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_proveedor_ts
    BEFORE UPDATE ON proveedor
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_inventario_ts
    BEFORE UPDATE ON inventario
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_compra_ts
    BEFORE UPDATE ON compra
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_venta_ts
    BEFORE UPDATE ON venta
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_transferencia_ts
    BEFORE UPDATE ON transferencia
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_envio_ts
    BEFORE UPDATE ON envio
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

-- ============================================================
-- 11. FUNCIÓN: COSTO PROMEDIO PONDERADO
-- ============================================================

CREATE OR REPLACE FUNCTION fn_calcular_costo_promedio(
    p_id_sucursal    INT,
    p_id_producto    INT,
    p_cantidad_nueva NUMERIC,
    p_costo_nuevo    NUMERIC
) RETURNS NUMERIC AS $$
DECLARE
    v_stock_actual  NUMERIC;
    v_costo_actual  NUMERIC;
BEGIN
    SELECT stock_actual, costo_promedio
    INTO   v_stock_actual, v_costo_actual
    FROM   inventario
    WHERE  id_sucursal = p_id_sucursal
      AND  id_producto = p_id_producto;

    IF v_stock_actual IS NULL OR (v_stock_actual + p_cantidad_nueva) = 0 THEN
        RETURN p_costo_nuevo;
    END IF;

    RETURN ROUND(
        (v_stock_actual * v_costo_actual + p_cantidad_nueva * p_costo_nuevo)
        / (v_stock_actual + p_cantidad_nueva),
        4
    );
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION fn_calcular_costo_promedio IS
    'Fórmula: (stock_actual × costo_actual + cantidad_nueva × costo_nuevo) / (stock_actual + cantidad_nueva). Llamar desde CompraService al confirmar recepción.';

-- ============================================================
-- 12. FUNCIÓN: VERIFICAR STOCK DISPONIBLE
-- ============================================================

CREATE OR REPLACE FUNCTION fn_verificar_stock(
    p_id_sucursal INT,
    p_id_producto INT,
    p_cantidad    NUMERIC
) RETURNS BOOLEAN AS $$
DECLARE
    v_stock NUMERIC;
BEGIN
    SELECT stock_actual INTO v_stock
    FROM   inventario
    WHERE  id_sucursal = p_id_sucursal
      AND  id_producto = p_id_producto;

    RETURN COALESCE(v_stock, 0) >= p_cantidad;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION fn_verificar_stock IS
    'Retorna TRUE si hay stock suficiente. Llamar desde VentaService y TransferenciaService antes de confirmar.';

-- ============================================================
-- 13. VISTAS PARA EL DASHBOARD
-- ============================================================

CREATE VIEW v_stock_con_alertas AS
SELECT
    s.nombre                                                AS sucursal,
    p.sku,
    p.nombre                                                AS producto,
    c.nombre                                                AS categoria,
    um.abreviatura                                          AS unidad,
    inv.stock_actual,
    COALESCE(inv.stock_minimo, p.stock_minimo)              AS stock_minimo,
    inv.costo_promedio,
    CASE
        WHEN inv.stock_actual = 0
            THEN 'AGOTADO'
        WHEN inv.stock_actual <= COALESCE(inv.stock_minimo, p.stock_minimo)
            THEN 'STOCK_MINIMO'
        ELSE 'NORMAL'
    END                                                     AS estado_stock
FROM inventario inv
JOIN sucursal   s   ON s.id_sucursal   = inv.id_sucursal
JOIN producto   p   ON p.id_producto   = inv.id_producto
LEFT JOIN categoria c ON c.id_categoria = p.id_categoria
LEFT JOIN producto_unidad pu ON pu.id_producto = p.id_producto AND pu.es_principal = TRUE
LEFT JOIN unidad_medida  um ON um.id_unidad    = pu.id_unidad
WHERE s.estado = TRUE AND p.activo = TRUE;

COMMENT ON VIEW v_stock_con_alertas IS 'Dashboard: estado del stock por sucursal con indicador de alerta.';

-- -------------------------------------------------------

CREATE VIEW v_ventas_por_mes AS
SELECT
    s.nombre                            AS sucursal,
    DATE_TRUNC('month', v.fecha)        AS mes,
    COUNT(v.id_venta)                   AS cantidad_ventas,
    SUM(v.total)                        AS total_ventas
FROM venta v
JOIN sucursal s ON s.id_sucursal = v.id_sucursal
WHERE v.estado = 'CONFIRMADA'
GROUP BY s.nombre, DATE_TRUNC('month', v.fecha)
ORDER BY mes DESC, total_ventas DESC;

COMMENT ON VIEW v_ventas_por_mes IS 'Dashboard: volumen de ventas mensual por sucursal para gráfica comparativa.';

-- -------------------------------------------------------

CREATE VIEW v_transferencias_activas AS
SELECT
    t.id_transferencia,
    so.nombre                           AS origen,
    sd.nombre                           AS destino,
    t.estado,
    t.prioridad,
    e.transportista,
    e.estado                            AS estado_envio,
    e.tiempo_estimado,
    e.tiempo_real,
    t.fecha_solicitud,
    t.fecha_envio,
    t.fecha_estimada,
    u.nombre                            AS solicitado_por
FROM transferencia t
JOIN sucursal so ON so.id_sucursal = t.sucursal_origen
JOIN sucursal sd ON sd.id_sucursal = t.sucursal_destino
JOIN usuario  u  ON u.id_usuario   = t.id_usuario_solicita
LEFT JOIN envio e ON e.id_transferencia = t.id_transferencia
WHERE t.estado NOT IN ('RECIBIDO', 'CANCELADO');

COMMENT ON VIEW v_transferencias_activas IS 'Dashboard: transferencias en curso con estado logístico y tiempos.';

-- -------------------------------------------------------

CREATE VIEW v_historial_movimientos AS
SELECT
    m.fecha,
    s.nombre            AS sucursal,
    p.sku,
    p.nombre            AS producto,
    m.tipo,
    m.motivo,
    m.cantidad,
    m.costo_unitario,
    u.nombre            AS responsable,
    m.referencia_id,
    m.referencia_tipo
FROM movimiento_inventario m
JOIN sucursal s ON s.id_sucursal = m.id_sucursal
JOIN producto p ON p.id_producto = m.id_producto
JOIN usuario  u ON u.id_usuario  = m.id_usuario
ORDER BY m.fecha DESC;

COMMENT ON VIEW v_historial_movimientos IS 'Auditoría completa: todos los movimientos con responsable y documento origen.';

-- ============================================================
-- 14. DATOS SEMILLA
-- ============================================================

-- Unidades de medida
INSERT INTO unidad_medida (nombre, abreviatura) VALUES
    ('Unidad',    'UND'),
    ('Kilogramo', 'KG'),
    ('Gramo',     'GR'),
    ('Litro',     'LT'),
    ('Mililitro', 'ML'),
    ('Caja',      'CJA'),
    ('Paquete',   'PKT'),
    ('Docena',    'DOC'),
    ('Metro',     'MT'),
    ('Par',       'PAR');

-- Categorías
INSERT INTO categoria (nombre, descripcion) VALUES
    ('Alimentos',       'Productos alimenticios perecederos y no perecederos'),
    ('Bebidas',         'Bebidas alcohólicas y no alcohólicas'),
    ('Limpieza',        'Productos de aseo y limpieza'),
    ('Papelería',       'Útiles de oficina y papelería'),
    ('Tecnología',      'Equipos electrónicos y accesorios'),
    ('Herramientas',    'Herramientas manuales y eléctricas'),
    ('Salud y Belleza', 'Cuidado personal y salud'),
    ('Otros',           'Sin clasificar');

-- Sucursales
INSERT INTO sucursal (nombre, direccion, telefono, correo) VALUES
    ('Sede Principal',    'Calle 1 # 10-20, Bogotá',       '6011234567', 'principal@empresa.com'),
    ('Sucursal Norte',    'Carrera 15 # 80-30, Bogotá',    '6017654321', 'norte@empresa.com'),
    ('Sucursal Sur',      'Avenida 68 # 5-10, Bogotá',     '6019876543', 'sur@empresa.com'),
    ('Sucursal Medellín', 'Calle 50 # 40-20, Medellín',    '6042345678', 'medellin@empresa.com');

-- Usuarios (contraseñas se hashean con bcrypt en la app)
INSERT INTO usuario (id_sucursal, nombre, correo, contrasena_hash, rol) VALUES
    (1, 'Administrador General',   'admin@empresa.com',             '$2a$10$placeholder_admin',     'ADMIN'),
    (1, 'Gerente Sede Principal',  'gerente.principal@empresa.com', '$2a$10$placeholder_gerente1',  'GERENTE'),
    (2, 'Gerente Sucursal Norte',  'gerente.norte@empresa.com',     '$2a$10$placeholder_gerente2',  'GERENTE'),
    (2, 'Operador Norte',          'operador.norte@empresa.com',    '$2a$10$placeholder_operador1', 'OPERADOR'),
    (3, 'Operador Sur',            'operador.sur@empresa.com',      '$2a$10$placeholder_operador2', 'OPERADOR'),
    (4, 'Gerente Medellín',        'gerente.medellin@empresa.com',  '$2a$10$placeholder_gerente3',  'GERENTE');

-- Proveedores
INSERT INTO proveedor (nombre, nit, contacto, correo, telefono, condicion_pago) VALUES
    ('Distribuidora Nacional S.A.', '900123456-1', 'Carlos Pérez',  'ventas@distnac.com',   '3001234567', '30 días'),
    ('Importadora Global Ltda.',    '800987654-2', 'Ana Rodríguez', 'compras@impglobal.com','3109876543', '60 días'),
    ('Suministros Rápidos SAS',     '700456789-3', 'Luis Martínez', 'pedidos@sumrap.com',   '3207654321', 'Contado');

-- Rutas bidireccionales entre sucursales
INSERT INTO ruta (sucursal_origen, sucursal_destino, dias_promedio, costo_promedio) VALUES
    (1, 2,  1,  50000), (2, 1,  1,  50000),
    (1, 3,  1,  60000), (3, 1,  1,  60000),
    (1, 4,  3, 180000), (4, 1,  3, 180000),
    (2, 3,  1,  55000), (3, 2,  1,  55000);

-- Productos
INSERT INTO producto (sku, nombre, descripcion, id_categoria, stock_minimo) VALUES
    ('PRD-001', 'Arroz Diana 500g',    'Arroz blanco empacado 500 gramos',   1, 10),
    ('PRD-002', 'Aceite Girasol 1L',   'Aceite de girasol botella 1 litro',  1,  5),
    ('PRD-003', 'Detergente Fab 1kg',  'Detergente en polvo 1 kilogramo',    3, 10),
    ('PRD-004', 'Cuaderno Norma 100h', 'Cuaderno rayado 100 hojas',          4, 20),
    ('PRD-005', 'Gaseosa Cola 2L',     'Bebida gaseosa sabor cola 2 litros', 2,  8);

-- Unidad principal por producto
INSERT INTO producto_unidad (id_producto, id_unidad, es_principal) VALUES
    (1, 6, TRUE),
    (2, 4, TRUE),
    (3, 2, TRUE),
    (4, 1, TRUE),
    (5, 4, TRUE);

-- Relación producto-proveedor
INSERT INTO producto_proveedor (id_producto, id_proveedor, precio_unitario, dias_entrega, preferido) VALUES
    (1, 1, 2500.00, 2, TRUE),
    (2, 1, 8900.00, 2, TRUE),
    (3, 2, 5800.00, 3, TRUE),
    (4, 3, 1600.00, 1, TRUE),
    (5, 1, 3900.00, 1, TRUE);

-- Inventario inicial en todas las sucursales
INSERT INTO inventario (id_producto, id_sucursal, stock_actual, costo_promedio, stock_minimo) VALUES
    (1, 1, 100, 2800, 10), (1, 2,  50, 2800, 10), (1, 3, 30, 2800,  5), (1, 4, 20, 2800,  5),
    (2, 1,  80, 9500,  5), (2, 2,  40, 9500,  5), (2, 3, 15, 9500,  5), (2, 4, 10, 9500,  5),
    (3, 1,  60, 6200, 10), (3, 2,  25, 6200, 10), (3, 3,  8, 6200,  5), (3, 4, 12, 6200,  5),
    (4, 1, 200, 1800, 20), (4, 2,  80, 1800, 20), (4, 3, 35, 1800, 10), (4, 4, 15, 1800, 10),
    (5, 1,  90, 4200,  8), (5, 2,  30, 4200,  8), (5, 3,  6, 4200,  5), (5, 4,  4, 4200,  5);