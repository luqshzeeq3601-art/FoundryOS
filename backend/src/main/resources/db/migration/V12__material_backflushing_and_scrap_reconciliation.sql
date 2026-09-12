-- V12__material_backflushing_and_scrap_reconciliation.sql
-- FactoryOS Version 2.0 Sprint 11: Epic 7 Story 2 (E7-S2) Material Backflushing & Scrap Reconciliation

CREATE TABLE IF NOT EXISTS materials (
    id UUID PRIMARY KEY,
    plant_id UUID REFERENCES plants(id),
    material_code VARCHAR(100) NOT NULL UNIQUE,
    material_name VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL DEFAULT 'RAW_MATERIAL',
    uom VARCHAR(32) NOT NULL DEFAULT 'KG',
    current_stock NUMERIC(14, 4) NOT NULL DEFAULT 0.0,
    minimum_stock NUMERIC(14, 4) NOT NULL DEFAULT 50.0,
    standard_cost NUMERIC(14, 4) NOT NULL DEFAULT 10.0,
    scrap_cost_center VARCHAR(64) NOT NULL DEFAULT 'CC-SCRAP-MACHINING',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_materials_code ON materials (material_code);
CREATE INDEX IF NOT EXISTS ix_materials_plant ON materials (plant_id);

CREATE TABLE IF NOT EXISTS material_consumption_records (
    id UUID PRIMARY KEY,
    production_order_id UUID NOT NULL REFERENCES production_orders(id),
    plant_id UUID REFERENCES plants(id),
    material_id UUID REFERENCES materials(id),
    material_code VARCHAR(100) NOT NULL,
    material_name VARCHAR(255) NOT NULL,
    lot_number VARCHAR(80),
    good_pieces_produced INT NOT NULL DEFAULT 0,
    scrap_pieces_produced INT NOT NULL DEFAULT 0,
    scrap_reason_code VARCHAR(64),
    scrap_cost_center VARCHAR(64) NOT NULL DEFAULT 'CC-SCRAP-MACHINING',
    theoretical_quantity NUMERIC(14, 4) NOT NULL,
    actual_quantity NUMERIC(14, 4) NOT NULL,
    variance_percentage NUMERIC(8, 4) NOT NULL DEFAULT 0.0,
    variance_alert_triggered BOOLEAN NOT NULL DEFAULT FALSE,
    uom VARCHAR(32) NOT NULL DEFAULT 'KG',
    recorded_by_id UUID,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_consumption_order ON material_consumption_records (production_order_id, recorded_at DESC);
CREATE INDEX IF NOT EXISTS ix_consumption_plant ON material_consumption_records (plant_id, recorded_at DESC);
CREATE INDEX IF NOT EXISTS ix_consumption_material ON material_consumption_records (material_code);
CREATE INDEX IF NOT EXISTS ix_consumption_variance ON material_consumption_records (variance_alert_triggered);

-- Seed Materials Catalog
INSERT INTO materials (id, material_code, material_name, category, uom, current_stock, minimum_stock, standard_cost, scrap_cost_center)
VALUES
    ('33333333-3333-3333-3333-333333333301', 'RAW-ALU-6061', 'Aerospace Aluminum Alloy 6061-T6 Billet', 'RAW_MATERIAL', 'KG', 2500.0, 200.0, 14.50, 'CC-SCRAP-MACHINING'),
    ('33333333-3333-3333-3333-333333333302', 'RAW-STL-4140', 'Alloy Steel Bar Stock 4140 Annealed', 'RAW_MATERIAL', 'KG', 1800.0, 150.0, 22.80, 'CC-SCRAP-HEAT-TREAT'),
    ('33333333-3333-3333-3333-333333333303', 'RAW-POM-BLK', 'Polyoxymethylene Delrin Black 500P', 'RAW_MATERIAL', 'KG', 950.0, 100.0, 18.20, 'CC-SCRAP-MACHINING'),
    ('33333333-3333-3333-3333-333333333304', 'MAT-BEARING-CERAMIC', 'Ceramic Ball Elements Si3N4 P4', 'COMPONENT', 'EA', 5000.0, 500.0, 6.75, 'CC-SCRAP-ASSEMBLY'),
    ('33333333-3333-3333-3333-333333333305', 'MAT-SPINDLE-SHAFT', 'Precision Hardened CNC Spindle Shaft', 'SUBASSEMBLY', 'EA', 450.0, 50.0, 85.00, 'CC-SCRAP-QUALITY-GATE')
ON CONFLICT (material_code) DO UPDATE 
SET current_stock = EXCLUDED.current_stock,
    standard_cost = EXCLUDED.standard_cost,
    scrap_cost_center = EXCLUDED.scrap_cost_center;

-- Additional BOM Seeds for ERP standard items
INSERT INTO bill_of_materials (id, product_code, material_code, material_name, required_quantity_per_unit, uom)
VALUES
    ('22222222-2222-2222-2222-222222222205', 'SPINDLE-ROT-V2', 'MAT-SPINDLE-SHAFT', 'Precision Hardened CNC Spindle Shaft', 1.0, 'EA'),
    ('22222222-2222-2222-2222-222222222206', 'SPINDLE-ROT-V2', 'MAT-BEARING-CERAMIC', 'Ceramic Ball Elements Si3N4 P4', 4.0, 'EA'),
    ('22222222-2222-2222-2222-222222222207', 'SPINDLE-ROT-V2', 'RAW-ALU-6061', 'Aerospace Aluminum Alloy 6061-T6 Billet', 2.5, 'KG'),
    ('22222222-2222-2222-2222-222222222208', 'BEARING-CERAMIC-608', 'MAT-BEARING-CERAMIC', 'Ceramic Ball Elements Si3N4 P4', 8.0, 'EA'),
    ('22222222-2222-2222-2222-222222222209', 'BEARING-CERAMIC-608', 'RAW-STL-4140', 'Alloy Steel Bar Stock 4140 Annealed', 0.15, 'KG')
ON CONFLICT (product_code, material_code) DO NOTHING;
