-- OptiPlant Inventory Management System - Update existing sucursal data
-- Version 5: Update sucursal data with codigo and ciudad

-- Update existing sucursales with proper codigo and ciudad values
UPDATE sucursal SET codigo = 'CENTRAL', ciudad = 'Ciudad Central' WHERE nombre = 'CENTRAL';
UPDATE sucursal SET codigo = 'NORTE', ciudad = 'Ciudad Norte' WHERE nombre = 'SUCURSAL_NORTE';
UPDATE sucursal SET codigo = 'SUR', ciudad = 'Ciudad Sur' WHERE nombre = 'SUCURSAL_SUR';
