-- V5__barcode_scanning_and_traceability.sql
-- FactoryOS Version 2.0 Sprint 7: Epic 8 (E8-S1) Barcode Scanning & Traceability

CREATE TABLE IF NOT EXISTS material_lots (
    id UUID PRIMARY KEY,
    lot_number VARCHAR(80) NOT NULL UNIQUE,
    material_code VARCHAR(100) NOT NULL,
    material_name VARCHAR(255) NOT NULL,
    quantity NUMERIC(14, 4) NOT NULL DEFAULT 0.0,
    uom VARCHAR(32) NOT NULL DEFAULT 'PCS',
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    expiry_date TIMESTAMP WITH TIME ZONE,
    supplier_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_material_lots_code ON material_lots (material_code);
CREATE INDEX IF NOT EXISTS ix_material_lots_status ON material_lots (status);

CREATE TABLE IF NOT EXISTS bill_of_materials (
    id UUID PRIMARY KEY,
    product_code VARCHAR(100) NOT NULL,
    material_code VARCHAR(100) NOT NULL,
    material_name VARCHAR(255) NOT NULL,
    required_quantity_per_unit NUMERIC(14, 4) NOT NULL DEFAULT 1.0,
    uom VARCHAR(32) NOT NULL DEFAULT 'PCS',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_bom_product_material UNIQUE (product_code, material_code)
);

CREATE INDEX IF NOT EXISTS ix_bom_product_code ON bill_of_materials (product_code);

CREATE TABLE IF NOT EXISTS barcode_scan_logs (
    id UUID PRIMARY KEY,
    scan_payload VARCHAR(1000) NOT NULL,
    barcode_format VARCHAR(64) NOT NULL DEFAULT 'UNKNOWN',
    barcode_type VARCHAR(64) NOT NULL DEFAULT 'UNKNOWN',
    scanner_source VARCHAR(64) NOT NULL DEFAULT 'HARDWARE_WEDGE',
    resolved_entity_type VARCHAR(64) NOT NULL DEFAULT 'NONE',
    resolved_entity_id VARCHAR(128),
    resolved_entity_summary VARCHAR(500),
    machine_id UUID,
    production_order_id UUID,
    validation_status VARCHAR(64) NOT NULL DEFAULT 'VALID',
    bom_matched BOOLEAN NOT NULL DEFAULT TRUE,
    error_message VARCHAR(1000),
    scanned_by_user_id UUID,
    scanned_by_name VARCHAR(255),
    latency_ms BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_barcode_scans_machine ON barcode_scan_logs (machine_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_barcode_scans_order ON barcode_scan_logs (production_order_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_barcode_scans_user ON barcode_scan_logs (scanned_by_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_barcode_scans_status ON barcode_scan_logs (validation_status);

-- Seed initial materials and BOM items for standard demo parts
INSERT INTO material_lots (id, lot_number, material_code, material_name, quantity, uom, status, expiry_date, supplier_name)
VALUES 
    ('11111111-1111-1111-1111-111111111101', 'LOT-ALU-6061-001', 'RAW-ALU-6061', 'Aerospace Aluminum Alloy 6061-T6 Billet', 500.0, 'KG', 'AVAILABLE', NOW() + INTERVAL '180 days', 'Alcoa Extrusions'),
    ('11111111-1111-1111-1111-111111111102', 'LOT-STL-4140-002', 'RAW-STL-4140', 'Alloy Steel Bar Stock 4140 Annealed', 350.0, 'KG', 'AVAILABLE', NOW() + INTERVAL '365 days', 'Timken Steel'),
    ('11111111-1111-1111-1111-111111111103', 'LOT-POLY-POM-003', 'RAW-POM-BLK', 'Polyoxymethylene Delrin Black 500P', 120.0, 'KG', 'AVAILABLE', NOW() + INTERVAL '90 days', 'DuPont Performance Polymers'),
    ('11111111-1111-1111-1111-111111111104', 'LOT-EXP-RESIN-004', 'RAW-EPOXY-202', 'Epoxy Binding Agent Fast Cure', 15.0, 'L', 'EXPIRED', NOW() - INTERVAL '10 days', 'Henkel Industrial')
ON CONFLICT (lot_number) DO NOTHING;

INSERT INTO bill_of_materials (id, product_code, material_code, material_name, required_quantity_per_unit, uom)
VALUES
    ('22222222-2222-2222-2222-222222222201', 'BRACKET-01', 'RAW-ALU-6061', 'Aerospace Aluminum Alloy 6061-T6 Billet', 1.25, 'KG'),
    ('22222222-2222-2222-2222-222222222202', 'GEAR-02', 'RAW-STL-4140', 'Alloy Steel Bar Stock 4140 Annealed', 2.10, 'KG'),
    ('22222222-2222-2222-2222-222222222203', 'HOUSING-03', 'RAW-ALU-6061', 'Aerospace Aluminum Alloy 6061-T6 Billet', 3.50, 'KG'),
    ('22222222-2222-2222-2222-222222222204', 'BUSHING-04', 'RAW-POM-BLK', 'Polyoxymethylene Delrin Black 500P', 0.45, 'KG')
ON CONFLICT (product_code, material_code) DO NOTHING;
