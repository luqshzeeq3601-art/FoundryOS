-- ==============================================================================
-- FactoryOS V3 Migration: Time-Series Downsampling Rollups & Retention Policies
-- Epic 4: Story E4-S2 (High-Frequency Time-Series Storage & Downsampling)
-- ==============================================================================

-- 1. 1-MINUTE CONTINUOUS ROLLUPS TABLE
-- Downsampled aggregate buckets for fast multi-day queries (1s raw -> 1min aggregates)
CREATE TABLE machine_telemetry_rollups_1m (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id UUID NOT NULL REFERENCES machines(id) ON DELETE CASCADE,
    tag_name VARCHAR(100) NOT NULL,
    bucket_start TIMESTAMPTZ NOT NULL,
    min_value DOUBLE PRECISION NOT NULL,
    max_value DOUBLE PRECISION NOT NULL,
    avg_value DOUBLE PRECISION NOT NULL,
    sample_count INT NOT NULL CHECK (sample_count > 0),
    quality VARCHAR(20) NOT NULL DEFAULT 'GOOD' CHECK (quality IN ('GOOD', 'BAD', 'UNCERTAIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_rollup_1m UNIQUE (machine_id, tag_name, bucket_start)
);

CREATE INDEX idx_rollup_1m_query ON machine_telemetry_rollups_1m(machine_id, tag_name, bucket_start DESC);
CREATE INDEX idx_rollup_1m_bucket ON machine_telemetry_rollups_1m(bucket_start DESC);

-- 2. 1-HOUR CONTINUOUS ROLLUPS TABLE
-- Downsampled aggregate buckets for long-range queries (30+ days to 1 year)
CREATE TABLE machine_telemetry_rollups_1h (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id UUID NOT NULL REFERENCES machines(id) ON DELETE CASCADE,
    tag_name VARCHAR(100) NOT NULL,
    bucket_start TIMESTAMPTZ NOT NULL,
    min_value DOUBLE PRECISION NOT NULL,
    max_value DOUBLE PRECISION NOT NULL,
    avg_value DOUBLE PRECISION NOT NULL,
    sample_count INT NOT NULL CHECK (sample_count > 0),
    quality VARCHAR(20) NOT NULL DEFAULT 'GOOD' CHECK (quality IN ('GOOD', 'BAD', 'UNCERTAIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_rollup_1h UNIQUE (machine_id, tag_name, bucket_start)
);

CREATE INDEX idx_rollup_1h_query ON machine_telemetry_rollups_1h(machine_id, tag_name, bucket_start DESC);
CREATE INDEX idx_rollup_1h_bucket ON machine_telemetry_rollups_1h(bucket_start DESC);

-- 3. RETENTION POLICIES CONFIGURATION TABLE
-- Configurable retention windows per plant or global policy
CREATE TABLE telemetry_retention_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_name VARCHAR(64) NOT NULL UNIQUE,
    raw_retention_days INT NOT NULL DEFAULT 7 CHECK (raw_retention_days >= 1),
    rollup_1m_retention_days INT NOT NULL DEFAULT 30 CHECK (rollup_1m_retention_days >= 7),
    rollup_1h_retention_days INT NOT NULL DEFAULT 365 CHECK (rollup_1h_retention_days >= 30),
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_pruned_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed default standard manufacturing retention policy (7d raw, 30d 1m, 365d 1h)
INSERT INTO telemetry_retention_policies (policy_name, raw_retention_days, rollup_1m_retention_days, rollup_1h_retention_days)
VALUES ('DEFAULT_PLANT_RETENTION', 7, 30, 365)
ON CONFLICT (policy_name) DO NOTHING;
