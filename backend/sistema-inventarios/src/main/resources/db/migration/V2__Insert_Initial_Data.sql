-- OptiPlant Inventory Management System - Initial Data
-- Version 2: Insert seed data for testing and development

-- ============================================================================
-- Insert: sucursal (Branches)
-- ============================================================================
INSERT INTO sucursal (nombre, direccion, telefono, correo, estado, created_by)
VALUES
    ('CENTRAL', 'Calle Principal 123, Piso 10, Centro', '(555) 1001-1000', 'central@optiplan.com', TRUE, 'SYSTEM'),
    ('SUCURSAL_NORTE', 'Avenida Norte 456, Zona Industrial', '(555) 2002-2000', 'norte@optiplan.com', TRUE, 'SYSTEM'),
    ('SUCURSAL_SUR', 'Carrera Sur 789, Comercial', '(555) 3003-3000', 'sur@optiplan.com', TRUE, 'SYSTEM');

-- ============================================================================
-- Insert: usuario (Users)
-- Note: Passwords are BCrypt hashed (using sample hashes for demonstration)
-- In production, use actual BCrypt hashes: e.g., BCrypt('password123')
-- Sample hashes: $2a$10$... format
-- ============================================================================
INSERT INTO usuario (id_sucursal, nombre, correo, contrasena_hash, rol, activo, created_by)
VALUES
    (1, 'Admin User', 'admin@optiplan.com', '$2a$10$slYQmyNdGzin7olVN3p5be4DlH.PKZbv5H8KnzzVgXXbVxzy2QDAG', 'ADMIN', TRUE, 'SYSTEM'),
    (1, 'Gerente Central', 'gerente@sucursal.com', '$2a$10$T5pGlM8rF9vJkL2nQ6sP.eX1Y3zZ4aB5cD6eF7gH8iJ9kL0mN1oP', 'GERENTE', TRUE, 'SYSTEM'),
    (2, 'Operador Inventario', 'operador@sucursal.com', '$2a$10$W2qR3sT4uV5wX6yZ7aB8cD9eF0gH1iJ2kL3mN4oP5qR6sT7uV8wX', 'OPERADOR', TRUE, 'SYSTEM'),
    (2, 'Gerente Norte', 'gerente.norte@optiplan.com', '$2a$10$S4tU5vW6xY7zA8bC9dE0fG1hI2jK3lM4nO5pQ6rR7sT8uV9wX0yZ', 'GERENTE', TRUE, 'SYSTEM'),
    (3, 'Operador Sur', 'operador.sur@optiplan.com', '$2a$10$Z1aB2cD3eF4gH5iJ6kL7mN8oP9qR0sT1uV2wX3yZ4aB5cD6eF7gH', 'OPERADOR', TRUE, 'SYSTEM');

-- ============================================================================
-- Insert: categoria (Product Categories)
-- ============================================================================
INSERT INTO categoria (nombre, descripcion, created_by)
VALUES
    ('FRUTAS', 'Frutas frescas y tropicales', 'SYSTEM'),
    ('VEGETALES', 'Vegetales frescos para consumo humano', 'SYSTEM'),
    ('CONDIMENTOS', 'Especias y condimentos diversos', 'SYSTEM'),
    ('BEBIDAS', 'Bebidas sin alcohol', 'SYSTEM'),
    ('LACTEOS', 'Productos lácteos frescos', 'SYSTEM');

-- ============================================================================
-- Insert: unidad_medida (Units of Measurement)
-- ============================================================================
INSERT INTO unidad_medida (nombre, abreviatura, created_by)
VALUES
    ('KILOGRAMOS', 'KG', 'SYSTEM'),
    ('LITROS', 'L', 'SYSTEM'),
    ('UNIDADES', 'UND', 'SYSTEM'),
    ('CAJAS', 'CJ', 'SYSTEM'),
    ('METROS', 'M', 'SYSTEM');

-- ============================================================================
-- Insert: producto (Products)
-- ============================================================================
INSERT INTO producto (sku, nombre, descripcion, id_categoria, stock_minimo, stock_maximo, activo, created_by)
VALUES
    ('MANZANA-001', 'Manzana Roja', 'Manzanas rojas frescas de origen local', 1, 50, 500, TRUE, 'SYSTEM'),
    ('PLATANO-001', 'Plátano Amarillo', 'Plátanos amarillos maduros de primera calidad', 1, 100, 800, TRUE, 'SYSTEM'),
    ('ZANAHORIA-001', 'Zanahoria Naranja', 'Zanahorias naranjas frescas sin pesticidas', 2, 75, 600, TRUE, 'SYSTEM'),
    ('TOMATE-001', 'Tomate Rojo', 'Tomates rojos maduros para ensaladas', 2, 100, 1000, TRUE, 'SYSTEM'),
    ('CEBOLLA-001', 'Cebolla Blanca', 'Cebollas blancas almacenables de calidad', 2, 60, 400, TRUE, 'SYSTEM'),
    ('PIMIENTA-001', 'Pimienta Negra', 'Pimienta negra molida grado A', 3, 20, 200, TRUE, 'SYSTEM'),
    ('AGUA-001', 'Agua Purificada', 'Agua purificada en botellas de 500ml', 4, 500, 5000, TRUE, 'SYSTEM'),
    ('LECHE-001', 'Leche Entera', 'Leche entera fresca homogeneizada 1L', 5, 100, 800, TRUE, 'SYSTEM'),
    ('QUESO-001', 'Queso Cheddar', 'Queso cheddar maduro importado', 5, 40, 300, TRUE, 'SYSTEM'),
    ('NARANJA-001', 'Naranja Valencia', 'Naranjas valencia frescas y dulces', 1, 80, 700, TRUE, 'SYSTEM');

-- ============================================================================
-- Insert: proveedor (Suppliers)
-- ============================================================================
INSERT INTO proveedor (nombre, nit, contacto, correo, telefono, condicion_pago, activo, created_by)
VALUES
    ('Productor Agrícola Central', '901.234.567-8', 'Juan García', 'contacto@agricolacentral.com', '(555) 5001-5000', 'Crédito 30 días', TRUE, 'SYSTEM'),
    ('Distribuidora Nacional Ltda', '801.345.678-9', 'María López', 'ventas@distribuitoranacional.com', '(555) 6001-6000', 'Crédito 15 días', TRUE, 'SYSTEM');

-- ============================================================================
-- Insert: inventario (Inventory - Initial Stock)
-- ============================================================================
INSERT INTO inventario (id_producto, id_sucursal, stock_actual, stock_minimo, costo_promedio, created_by)
VALUES
    (1, 1, 200, 50, 1.50, 'SYSTEM'),
    (1, 2, 150, 50, 1.50, 'SYSTEM'),
    (2, 1, 300, 100, 0.80, 'SYSTEM'),
    (2, 2, 250, 100, 0.80, 'SYSTEM'),
    (3, 1, 180, 75, 0.70, 'SYSTEM'),
    (3, 3, 120, 75, 0.70, 'SYSTEM'),
    (4, 1, 220, 100, 2.10, 'SYSTEM'),
    (4, 2, 180, 100, 2.10, 'SYSTEM'),
    (5, 1, 160, 60, 1.20, 'SYSTEM'),
    (5, 3, 140, 60, 1.20, 'SYSTEM'),
    (6, 1, 85, 20, 8.50, 'SYSTEM'),
    (6, 2, 65, 20, 8.50, 'SYSTEM'),
    (7, 1, 2000, 500, 0.35, 'SYSTEM'),
    (7, 2, 1800, 500, 0.35, 'SYSTEM'),
    (7, 3, 1200, 500, 0.35, 'SYSTEM'),
    (8, 1, 400, 100, 1.80, 'SYSTEM'),
    (8, 2, 350, 100, 1.80, 'SYSTEM'),
    (9, 1, 120, 40, 12.50, 'SYSTEM'),
    (9, 2, 90, 40, 12.50, 'SYSTEM'),
    (10, 1, 250, 80, 1.80, 'SYSTEM');

-- ============================================================================
-- Insert: ruta (Transport Routes)
-- ============================================================================
INSERT INTO ruta (sucursal_origen, sucursal_destino, dias_promedio, costo_promedio, activa, created_by)
VALUES
    (1, 2, 1, 125.00, TRUE, 'SYSTEM'),
    (1, 3, 2, 185.00, TRUE, 'SYSTEM'),
    (2, 1, 1, 125.00, TRUE, 'SYSTEM'),
    (2, 3, 1, 95.00, TRUE, 'SYSTEM'),
    (3, 1, 2, 185.00, TRUE, 'SYSTEM'),
    (3, 2, 1, 95.00, TRUE, 'SYSTEM');

-- ============================================================================
-- Insert: compra (Sample Purchase Orders)
-- ============================================================================
INSERT INTO compra (id_proveedor, id_sucursal, id_usuario, estado, descuento_pct, dias_pago, total, notas, fecha, created_by)
VALUES
    (1, 1, 1, 'RECIBIDO', 5.00, 30, 1250.50, 'Compra de frutas frescas semana 1', NOW() - INTERVAL '10 days', 'SYSTEM'),
    (2, 2, 2, 'CONFIRMADO', 3.50, 15, 890.75, 'Compra de vegetales y condimentos', NOW() - INTERVAL '2 days', 'SYSTEM'),
    (1, 1, 1, 'BORRADOR', 0.00, 30, NULL, 'Compra pendiente de confirmar', NOW(), 'SYSTEM');

-- ============================================================================
-- Insert: detalle_compra (Purchase Order Details)
-- ============================================================================
INSERT INTO detalle_compra (id_compra, id_producto, cantidad, precio_unitario, descuento, cantidad_recibida, created_by)
VALUES
    (1, 1, 150, 1.50, 0.00, 150, 'SYSTEM'),
    (1, 2, 200, 0.80, 5.00, 200, 'SYSTEM'),
    (2, 3, 100, 0.70, 3.50, 100, 'SYSTEM'),
    (2, 6, 50, 8.50, 0.00, 50, 'SYSTEM');

-- ============================================================================
-- Insert: venta (Sample Sales Orders)
-- ============================================================================
INSERT INTO venta (id_sucursal, id_usuario, estado, lista_precios, subtotal, descuento_pct, total, notas, fecha, created_by)
VALUES
    (1, 2, 'ENTREGADO', 'LISTA_1', 450.00, 0.00, 450.00, 'Venta a cliente mayorista', NOW() - INTERVAL '5 days', 'SYSTEM'),
    (2, 4, 'CONFIRMADO', 'LISTA_1', 320.50, 10.00, 288.45, 'Venta a empresa local', NOW() - INTERVAL '1 day', 'SYSTEM'),
    (1, 2, 'PENDIENTE', 'LISTA_1', 600.00, 0.00, 600.00, 'Nueva venta sin confirmar', NOW(), 'SYSTEM');

-- ============================================================================
-- Insert: detalle_venta (Sales Order Details)
-- ============================================================================
INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio, descuento, total_linea, created_by)
VALUES
    (1, 1, 100, 2.50, 0.00, 250.00, 'SYSTEM'),
    (1, 2, 100, 1.20, 0.00, 120.00, 'SYSTEM'),
    (1, 8, 20, 4.00, 0.00, 80.00, 'SYSTEM'),
    (2, 3, 150, 1.20, 10.00, 162.00, 'SYSTEM'),
    (2, 4, 80, 2.50, 0.00, 200.00, 'SYSTEM');

-- ============================================================================
-- Insert: transferencia (Sample Stock Transfers)
-- ============================================================================
INSERT INTO transferencia (sucursal_origen, sucursal_destino, id_usuario_solicita, id_usuario_aprueba, id_ruta, estado, prioridad, notas, fecha_solicitud, fecha_envio, fecha_estimada, fecha_recepcion, created_by)
VALUES
    (1, 2, 2, 1, 1, 'RECIBIDO', 'NORMAL', 'Transferencia de frutas frescas', NOW() - INTERVAL '7 days', NOW() - INTERVAL '6 days', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', 'SYSTEM'),
    (2, 1, 4, 1, 2, 'ENVIADO', 'ALTA', 'Reposición de stock urgente', NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', NULL, 'SYSTEM'),
    (1, 3, 2, NULL, 3, 'SOLICITADO', 'NORMAL', 'Solicitud nueva de transferencia', NOW(), NULL, NOW() + INTERVAL '2 days', NULL, 'SYSTEM');

-- ============================================================================
-- Insert: detalle_transferencia (Transfer Details)
-- ============================================================================
INSERT INTO detalle_transferencia (id_transferencia, id_producto, cantidad_solicitada, cantidad_enviada, cantidad_recibida, accion_faltante, created_by)
VALUES
    (1, 1, 100, 100, 100, 'PENDIENTE', 'SYSTEM'),
    (1, 2, 150, 150, 150, 'PENDIENTE', 'SYSTEM'),
    (2, 3, 80, 80, 0, 'PENDIENTE', 'SYSTEM'),
    (2, 5, 60, 60, 0, 'PENDIENTE', 'SYSTEM'),
    (3, 6, 50, 0, 0, 'PENDIENTE', 'SYSTEM'),
    (3, 7, 500, 0, 0, 'PENDIENTE', 'SYSTEM');

-- ============================================================================
-- Insert: alerta (Sample Alerts)
-- ============================================================================
INSERT INTO alerta (id_sucursal, id_producto, tipo_alerta, mensaje, resuelta, created_by)
VALUES
    (1, 5, 'STOCK_BAJO', 'Stock de cebollas blancas por debajo del mínimo. Cantidad actual: 45', FALSE, 'SYSTEM'),
    (2, 3, 'STOCK_BAJO', 'Stock de zanahorias naranjas por debajo del mínimo. Cantidad actual: 60', FALSE, 'SYSTEM'),
    (1, 9, 'STOCK_BAJO', 'Stock de queso cheddar bajo. Cantidad actual: 35', FALSE, 'SYSTEM'),
    (3, 8, 'STOCK_ALTO', 'Stock de leche entera muy alto. Cantidad actual: 820 (máximo: 800)', FALSE, 'SYSTEM'),
    (2, 7, 'COMPRA_PENDIENTE', 'Necesario realizar compra de agua purificada para mantener stock seguro', FALSE, 'SYSTEM');
