-- OptiPlant Inventory Management System - Add missing columns to sucursal
-- Version 4: Add codigo and ciudad columns to sucursal table

-- Add codigo column (required, unique identifier for branches)
ALTER TABLE sucursal ADD COLUMN IF NOT EXISTS codigo VARCHAR(20);

-- Add ciudad column
ALTER TABLE sucursal ADD COLUMN IF NOT EXISTS ciudad VARCHAR(50);

-- Update existing records with default values
UPDATE sucursal SET codigo = 'SUC-' || id_sucursal WHERE codigo IS NULL;
UPDATE sucursal SET ciudad = 'Sin especificar' WHERE ciudad IS NULL;

-- Make codigo NOT NULL after setting defaults
ALTER TABLE sucursal ALTER COLUMN codigo SET NOT NULL;

-- Add unique constraint on codigo
CREATE UNIQUE INDEX IF NOT EXISTS idx_sucursal_codigo ON sucursal(codigo);

COMMENT ON COLUMN sucursal.codigo IS 'Unique branch code identifier';
COMMENT ON COLUMN sucursal.ciudad IS 'City where the branch is located';
