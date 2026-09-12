-- ==============================================================================
-- FactoryOS V7 Database Schema Migration
-- Epic 5: Store-and-Forward Edge Resilience & Offline Sync Buffer (E5-S3)
-- ==============================================================================

-- 1. EDGE_GATEWAYS TABLE
CREATE TABLE edge_gateways (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gateway_code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    plant_id UUID NOT NULL REFERENCES plants(id) ON DELETE RESTRICT,
    ip_address VARCHAR(64) NULL,
    mac_address VARCHAR(64) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE' CHECK (status IN ('ONLINE', 'OFFLINE', 'SYNCING', 'DEGRADED')),
    last_heartbeat_at TIMESTAMPTZ NULL,
    last_sync_at TIMESTAMPTZ NULL,
    last_sync_sequence_id BIGINT NOT NULL DEFAULT 0,
    buffer_capacity_records INT NOT NULL DEFAULT 100000,
    firmware_version VARCHAR(32) NOT NULL DEFAULT '2.0.0-EDGE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 2. EDGE_OFFLINE_SYNC_BATCHES TABLE
CREATE TABLE edge_offline_sync_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gateway_id UUID NOT NULL REFERENCES edge_gateways(id) ON DELETE RESTRICT,
    plant_id UUID NOT NULL REFERENCES plants(id) ON DELETE RESTRICT,
    batch_id VARCHAR(80) NOT NULL UNIQUE,
    sequence_start BIGINT NOT NULL,
    sequence_end BIGINT NOT NULL,
    total_records INT NOT NULL,
    processed_records INT NOT NULL DEFAULT 0,
    failed_records INT NOT NULL DEFAULT 0,
    disconnected_at TIMESTAMPTZ NOT NULL,
    reconnected_at TIMESTAMPTZ NOT NULL,
    sync_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (sync_status IN ('PENDING', 'PROCESSING', 'RECONCILED', 'FAILED', 'PARTIAL')),
    reconciliation_notes TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reconciled_at TIMESTAMPTZ NULL
);

-- 3. EDGE_OFFLINE_TRANSACTION_LOGS TABLE
CREATE TABLE edge_offline_transaction_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES edge_offline_sync_batches(id) ON DELETE CASCADE,
    gateway_id UUID NOT NULL REFERENCES edge_gateways(id) ON DELETE RESTRICT,
    plant_id UUID NOT NULL REFERENCES plants(id) ON DELETE RESTRICT,
    sequence_id BIGINT NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    transaction_type VARCHAR(40) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id UUID NULL,
    payload_json TEXT NOT NULL,
    vector_clock_version BIGINT NOT NULL DEFAULT 1,
    recorded_at TIMESTAMPTZ NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    execution_status VARCHAR(25) NOT NULL DEFAULT 'PROCESSED' CHECK (execution_status IN ('PROCESSED', 'CONFLICT_RESOLVED', 'FAILED', 'DUPLICATE_IGNORED')),
    conflict_resolution_note VARCHAR(1000) NULL
);

-- 4. INDEXES FOR HIGH-THROUGHPUT REPLAY & SEARCH
CREATE INDEX ix_edge_gateways_plant ON edge_gateways(plant_id);
CREATE INDEX ix_edge_gateways_status ON edge_gateways(status);
CREATE INDEX ix_edge_sync_batches_gateway ON edge_offline_sync_batches(gateway_id, sync_status);
CREATE INDEX ix_edge_sync_batches_plant ON edge_offline_sync_batches(plant_id);
CREATE INDEX ix_edge_tx_logs_batch ON edge_offline_transaction_logs(batch_id, sequence_id);
CREATE INDEX ix_edge_tx_logs_idempotency ON edge_offline_transaction_logs(idempotency_key);
CREATE INDEX ix_edge_tx_logs_type ON edge_offline_transaction_logs(transaction_type);

-- 5. SEED DEFAULT EDGE GATEWAYS
INSERT INTO edge_gateways (id, gateway_code, name, plant_id, ip_address, status, last_heartbeat_at, firmware_version, created_at, updated_at) VALUES
    ('00000000-0000-0000-0000-000000000601', 'EDGE-GW-AUSTIN-01', 'Austin Primary Edge Gateway', '00000000-0000-0000-0000-000000000201', '10.20.1.50', 'ONLINE', now(), '2.0.0-EDGE', now(), now()),
    ('00000000-0000-0000-0000-000000000602', 'EDGE-GW-BERLIN-02', 'Berlin Heavy Press Edge Node', '00000000-0000-0000-0000-000000000202', '10.30.2.80', 'ONLINE', now(), '2.0.0-EDGE', now(), now())
ON CONFLICT (gateway_code) DO NOTHING;
