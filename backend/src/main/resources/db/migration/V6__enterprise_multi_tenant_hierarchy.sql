-- ==============================================================================
-- FactoryOS V6 Database Schema Migration
-- Epic 5: Multi-Plant Federation & Multi-Tenant Organizational Hierarchy (E5-S1)
-- ==============================================================================

-- 1. ENTERPRISES TABLE
CREATE TABLE enterprises (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 2. PLANTS (SITES) TABLE
CREATE TABLE plants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enterprise_id UUID NOT NULL REFERENCES enterprises(id) ON DELETE RESTRICT,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    address VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 3. PRODUCTION_AREAS TABLE
CREATE TABLE production_areas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plant_id UUID NOT NULL REFERENCES plants(id) ON DELETE RESTRICT,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uq_area_plant_code UNIQUE (plant_id, code)
);

-- 4. PRODUCTION_LINES TABLE
CREATE TABLE production_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    area_id UUID NOT NULL REFERENCES production_areas(id) ON DELETE RESTRICT,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uq_line_area_code UNIQUE (area_id, code)
);

-- 5. WORK_CELLS TABLE
CREATE TABLE work_cells (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    line_id UUID NOT NULL REFERENCES production_lines(id) ON DELETE RESTRICT,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uq_cell_line_code UNIQUE (line_id, code)
);

-- 6. USER_PLANT_MEMBERSHIPS TABLE
CREATE TABLE user_plant_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plant_id UUID NOT NULL REFERENCES plants(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_plant_membership UNIQUE (user_id, plant_id)
);

-- 7. ALTER TRANSACTIONAL & OPERATIONAL TABLES FOR PLANT SCOPING
ALTER TABLE machines
    ADD COLUMN plant_id UUID NULL REFERENCES plants(id) ON DELETE RESTRICT,
    ADD COLUMN area_id UUID NULL REFERENCES production_areas(id) ON DELETE RESTRICT,
    ADD COLUMN line_id UUID NULL REFERENCES production_lines(id) ON DELETE RESTRICT,
    ADD COLUMN work_cell_id UUID NULL REFERENCES work_cells(id) ON DELETE RESTRICT;

ALTER TABLE production_orders
    ADD COLUMN plant_id UUID NULL REFERENCES plants(id) ON DELETE RESTRICT;

ALTER TABLE downtime_events
    ADD COLUMN plant_id UUID NULL REFERENCES plants(id) ON DELETE RESTRICT;

ALTER TABLE maintenance_work_orders
    ADD COLUMN plant_id UUID NULL REFERENCES plants(id) ON DELETE RESTRICT;

ALTER TABLE material_lots
    ADD COLUMN plant_id UUID NULL REFERENCES plants(id) ON DELETE RESTRICT;

ALTER TABLE barcode_scan_logs
    ADD COLUMN plant_id UUID NULL REFERENCES plants(id) ON DELETE RESTRICT;

ALTER TABLE audit_events
    ADD COLUMN plant_id UUID NULL REFERENCES plants(id) ON DELETE RESTRICT;

-- 8. INDEXES FOR MULTI-TENANT QUERY OPTIMIZATION
CREATE INDEX ix_plants_enterprise ON plants(enterprise_id);
CREATE INDEX ix_areas_plant ON production_areas(plant_id);
CREATE INDEX ix_lines_area ON production_lines(area_id);
CREATE INDEX ix_work_cells_line ON work_cells(line_id);
CREATE INDEX ix_user_plant_memberships_user ON user_plant_memberships(user_id);
CREATE INDEX ix_user_plant_memberships_plant ON user_plant_memberships(plant_id);

CREATE INDEX ix_machines_plant ON machines(plant_id);
CREATE INDEX ix_machines_area ON machines(area_id);
CREATE INDEX ix_machines_line ON machines(line_id);
CREATE INDEX ix_machines_cell ON machines(work_cell_id);

CREATE INDEX ix_production_orders_plant ON production_orders(plant_id);
CREATE INDEX ix_downtime_events_plant ON downtime_events(plant_id);
CREATE INDEX ix_maintenance_orders_plant ON maintenance_work_orders(plant_id);
CREATE INDEX ix_material_lots_plant ON material_lots(plant_id);
CREATE INDEX ix_barcode_scan_logs_plant ON barcode_scan_logs(plant_id);
CREATE INDEX ix_audit_events_plant ON audit_events(plant_id);

-- 9. SEED ENTERPRISE HIERARCHY AND DEFAULT SITES
INSERT INTO enterprises (id, code, name, description, created_at, updated_at) VALUES
    ('00000000-0000-0000-0000-000000000100', 'ENT-GLOBAL', 'FactoryOS Global Enterprise', 'Global Enterprise Headquarters', now(), now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO plants (id, enterprise_id, code, name, timezone, address, status, created_at, updated_at) VALUES
    ('00000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000100', 'PLANT-AUSTIN-01', 'Austin Gigafactory', 'America/Chicago', '13101 Harold Green Rd, Austin, TX 78725', 'ACTIVE', now(), now()),
    ('00000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000100', 'PLANT-BERLIN-02', 'Berlin Advanced Manufacturing', 'Europe/Berlin', 'Gigafactory Berlin-Brandenburg, 15537 Grünheide', 'ACTIVE', now(), now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO production_areas (id, plant_id, code, name, description, created_at, updated_at) VALUES
    ('00000000-0000-0000-0000-000000000301', '00000000-0000-0000-0000-000000000201', 'AREA-MACHINING', 'Heavy CNC Machining Area', 'High-precision 5-axis milling and turning cells', now(), now()),
    ('00000000-0000-0000-0000-000000000302', '00000000-0000-0000-0000-000000000201', 'AREA-ASSEMBLY', 'Final Assembly & Testing', 'Automated powertrain assembly', now(), now()),
    ('00000000-0000-0000-0000-000000000303', '00000000-0000-0000-0000-000000000202', 'AREA-STAMPING', 'Stamping & Body Structure', 'Automated hydraulic press stamping shop', now(), now())
ON CONFLICT (plant_id, code) DO NOTHING;

INSERT INTO production_lines (id, area_id, code, name, description, created_at, updated_at) VALUES
    ('00000000-0000-0000-0000-000000000401', '00000000-0000-0000-0000-000000000301', 'LINE-MILL-01', 'CNC Milling Line A', 'Automated CNC machining line', now(), now()),
    ('00000000-0000-0000-0000-000000000402', '00000000-0000-0000-0000-000000000302', 'LINE-ASSY-01', 'Final Assembly Line 1', 'Main assembly line', now(), now()),
    ('00000000-0000-0000-0000-000000000403', '00000000-0000-0000-0000-000000000303', 'LINE-PRESS-01', 'Heavy Press Line 1', 'High-tonnage stamping line', now(), now())
ON CONFLICT (area_id, code) DO NOTHING;

INSERT INTO work_cells (id, line_id, code, name, description, created_at, updated_at) VALUES
    ('00000000-0000-0000-0000-000000000501', '00000000-0000-0000-0000-000000000401', 'CELL-CNC-01', 'Milling Cell 01', 'Robotic loaded 5-axis cell', now(), now()),
    ('00000000-0000-0000-0000-000000000502', '00000000-0000-0000-0000-000000000402', 'CELL-ROBOTIC-01', 'Robotic Fastening Cell', 'Automated fastening station', now(), now()),
    ('00000000-0000-0000-0000-000000000503', '00000000-0000-0000-0000-000000000403', 'CELL-STAMP-01', 'Press Feeder Cell', 'Coil feeder & blanking cell', now(), now())
ON CONFLICT (line_id, code) DO NOTHING;

-- 10. BACKFILL EXISTING ASSETS AND TRANSACTIONS TO DEFAULT PLANT (AUSTIN-01)
UPDATE machines SET 
    plant_id = '00000000-0000-0000-0000-000000000201',
    area_id = '00000000-0000-0000-0000-000000000301',
    line_id = '00000000-0000-0000-0000-000000000401',
    work_cell_id = '00000000-0000-0000-0000-000000000501'
WHERE plant_id IS NULL;

UPDATE production_orders SET plant_id = '00000000-0000-0000-0000-000000000201' WHERE plant_id IS NULL;
UPDATE downtime_events SET plant_id = '00000000-0000-0000-0000-000000000201' WHERE plant_id IS NULL;
UPDATE maintenance_work_orders SET plant_id = '00000000-0000-0000-0000-000000000201' WHERE plant_id IS NULL;
UPDATE material_lots SET plant_id = '00000000-0000-0000-0000-000000000201' WHERE plant_id IS NULL;
UPDATE barcode_scan_logs SET plant_id = '00000000-0000-0000-0000-000000000201' WHERE plant_id IS NULL;
UPDATE audit_events SET plant_id = '00000000-0000-0000-0000-000000000201' WHERE plant_id IS NULL;

-- 11. SEED DEFAULT USER PLANT MEMBERSHIPS FOR ALL EXISTING USERS
INSERT INTO user_plant_memberships (id, user_id, plant_id, role_id, is_default, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    u.id,
    '00000000-0000-0000-0000-000000000201',
    u.role_id,
    true,
    now(),
    now()
FROM users u
ON CONFLICT (user_id, plant_id) DO NOTHING;
