-- ==============================================================================
-- FactoryOS V2 Migration: Automated Machine Telemetry & IIoT Protocol Schema
-- Epic 4: Story E4-S1 (Industrial Protocol Gateway: OPC-UA & MQTT)
-- ==============================================================================

-- 1. MACHINE_TAG_MAPPINGS TABLE
-- Maps industrial controller tags (OPC-UA node IDs, MQTT topics, Modbus registers) to FactoryOS machines
CREATE TABLE machine_tag_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id UUID NOT NULL REFERENCES machines(id) ON DELETE CASCADE,
    tag_name VARCHAR(100) NOT NULL,
    protocol VARCHAR(32) NOT NULL CHECK (protocol IN ('OPC_UA', 'MQTT_SPARKPLUG_B', 'MODBUS_TCP')),
    tag_address VARCHAR(255) NOT NULL,
    data_type VARCHAR(32) NOT NULL DEFAULT 'DOUBLE',
    unit_of_measure VARCHAR(32) NULL,
    scale_factor NUMERIC(12, 4) NOT NULL DEFAULT 1.0000,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uq_machine_tag_name UNIQUE (machine_id, tag_name)
);

CREATE INDEX idx_tag_mappings_machine ON machine_tag_mappings(machine_id) WHERE is_deleted = false;
CREATE INDEX idx_tag_mappings_protocol ON machine_tag_mappings(protocol) WHERE is_deleted = false;

-- 2. MACHINE_TELEMETRY_POINTS TABLE
-- High-throughput time-series sensor telemetry points ingested from edge gateways
CREATE TABLE machine_telemetry_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id UUID NOT NULL REFERENCES machines(id) ON DELETE CASCADE,
    tag_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    metric_unit VARCHAR(32) NULL,
    quality VARCHAR(20) NOT NULL DEFAULT 'GOOD' CHECK (quality IN ('GOOD', 'BAD', 'UNCERTAIN')),
    timestamp TIMESTAMPTZ NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Compound indexes for fast time-series retrieval and downsampling queries
CREATE INDEX idx_telemetry_machine_time ON machine_telemetry_points(machine_id, timestamp DESC);
CREATE INDEX idx_telemetry_tag_time ON machine_telemetry_points(machine_id, tag_name, timestamp DESC);
