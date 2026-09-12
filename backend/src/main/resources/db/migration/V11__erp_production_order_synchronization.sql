-- ==============================================================================
-- FoundryOS Migration V11: ERP Production Order Synchronization (Sprint 11 E7-S1)
-- SAP S/4HANA OData v4 & Oracle NetSuite REST Integration
-- ==============================================================================

-- 1. ERP CONNECTORS TABLE
CREATE TABLE erp_connectors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plant_id UUID NULL REFERENCES plants(id) ON DELETE RESTRICT,
    name VARCHAR(128) NOT NULL,
    erp_type VARCHAR(32) NOT NULL DEFAULT 'SAP_S4HANA' CHECK (erp_type IN ('SAP_S4HANA', 'ORACLE_NETSUITE', 'GENERIC_ODATA_V4', 'MOCK_ERP')),
    base_url VARCHAR(512) NOT NULL,
    auth_type VARCHAR(32) NOT NULL DEFAULT 'BASIC' CHECK (auth_type IN ('BASIC', 'BEARER_TOKEN', 'OAUTH2', 'API_KEY')),
    api_key_or_user VARCHAR(256) NULL,
    secret_or_token VARCHAR(1024) NULL,
    client_id VARCHAR(128) NULL,
    company_id_or_client VARCHAR(64) NULL,
    sync_interval_seconds INT NOT NULL DEFAULT 300,
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_auto_sync_enabled BOOLEAN NOT NULL DEFAULT true,
    health_status VARCHAR(32) NOT NULL DEFAULT 'HEALTHY' CHECK (health_status IN ('HEALTHY', 'DEGRADED', 'DISCONNECTED')),
    last_health_check_at TIMESTAMPTZ NULL,
    last_sync_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 2. EXTEND PRODUCTION_ORDERS WITH ERP COLUMNS
ALTER TABLE production_orders
    ADD COLUMN erp_system VARCHAR(32) NULL,
    ADD COLUMN erp_order_id VARCHAR(64) NULL,
    ADD COLUMN erp_batch_number VARCHAR(64) NULL,
    ADD COLUMN erp_sync_status VARCHAR(32) NOT NULL DEFAULT 'LOCAL_ONLY',
    ADD COLUMN last_erp_sync_at TIMESTAMPTZ NULL;

-- 3. ERP ORDER CONFIRMATIONS TABLE
CREATE TABLE erp_order_confirmations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    production_order_id UUID NOT NULL REFERENCES production_orders(id) ON DELETE CASCADE,
    connector_id UUID NULL REFERENCES erp_connectors(id) ON DELETE SET NULL,
    plant_id UUID NULL REFERENCES plants(id) ON DELETE RESTRICT,
    confirmation_number VARCHAR(64) NOT NULL UNIQUE,
    erp_order_id VARCHAR(64) NOT NULL,
    confirmed_good_qty INT NOT NULL DEFAULT 0,
    confirmed_scrap_qty INT NOT NULL DEFAULT 0,
    scrap_reason VARCHAR(64) NULL,
    labor_hours DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    machine_hours DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    erp_posting_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (erp_posting_status IN ('PENDING', 'POSTED', 'FAILED', 'RETRY')),
    erp_document_number VARCHAR(64) NULL,
    error_message TEXT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    posted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 4. ERP SYNC LOGS TABLE (AUDIT & TELEMETRY)
CREATE TABLE erp_sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    connector_id UUID NULL REFERENCES erp_connectors(id) ON DELETE SET NULL,
    plant_id UUID NULL REFERENCES plants(id) ON DELETE RESTRICT,
    sync_direction VARCHAR(32) NOT NULL CHECK (sync_direction IN ('INBOUND_RELEASE', 'OUTBOUND_CONFIRMATION', 'POLL_ORDERS', 'HEALTH_CHECK')),
    entity_type VARCHAR(64) NOT NULL DEFAULT 'PRODUCTION_ORDER',
    entity_id UUID NULL,
    erp_reference_id VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' CHECK (status IN ('SUCCESS', 'RETRYING', 'FAILED', 'CONFLICT')),
    payload_json TEXT NULL,
    response_json TEXT NULL,
    error_message TEXT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    synced_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 5. PERFORMANCE INDEXES
CREATE INDEX idx_erp_connectors_plant ON erp_connectors (plant_id, is_active) WHERE is_deleted = false;
CREATE INDEX idx_po_erp_order_id ON production_orders (erp_order_id) WHERE is_deleted = false;
CREATE INDEX idx_erp_conf_po ON erp_order_confirmations (production_order_id, erp_posting_status);
CREATE INDEX idx_erp_sync_logs_synced_at ON erp_sync_logs (synced_at DESC);

-- 6. INITIAL SEED CONNECTORS
INSERT INTO erp_connectors (id, plant_id, name, erp_type, base_url, auth_type, api_key_or_user, secret_or_token, company_id_or_client, sync_interval_seconds, is_active, is_auto_sync_enabled, health_status)
SELECT
    '00000000-0000-0000-0000-000000000101'::uuid,
    p.id,
    'SAP S/4HANA Enterprise Cloud (Plant ' || p.code || ')',
    'SAP_S4HANA',
    'https://s4hana.corp.foundryos.com/sap/opu/odata4/sap/api_production_order_2/srvd_a2x/sap/productionorder/0001',
    'OAUTH2',
    'FOUNDRYOS_SAP_CLIENT',
    'ENC_S4_SECRET_A9984B',
    '100',
    120,
    true,
    true,
    'HEALTHY'
FROM plants p WHERE p.code = 'PLANT-AUSTIN'
LIMIT 1;

INSERT INTO erp_connectors (id, plant_id, name, erp_type, base_url, auth_type, api_key_or_user, secret_or_token, company_id_or_client, sync_interval_seconds, is_active, is_auto_sync_enabled, health_status)
SELECT
    '00000000-0000-0000-0000-000000000102'::uuid,
    p.id,
    'Oracle NetSuite ERP (Plant ' || p.code || ')',
    'ORACLE_NETSUITE',
    'https://884192.restlets.api.netsuite.com/app/site/hosting/restlet.nl',
    'BEARER_TOKEN',
    'NS_TOKEN_KEY_AUSTIN',
    'ENC_NS_SECRET_9921',
    '884192',
    300,
    true,
    true,
    'HEALTHY'
FROM plants p WHERE p.code = 'PLANT-STUTTGART'
LIMIT 1;