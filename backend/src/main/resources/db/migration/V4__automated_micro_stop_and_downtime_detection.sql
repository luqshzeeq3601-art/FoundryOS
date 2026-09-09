-- =============================================================================
-- FactoryOS V4: Automated Micro-Stop and Downtime Detection
-- Sprint 7 (Epic 4: Story E4-S3)
-- =============================================================================

-- 1. Modify reason_code constraint on downtime_events to support MICRO_STOP, TOOLING_JAM, OPERATOR_PAUSE, UNPLANNED_MAINTENANCE
ALTER TABLE downtime_events DROP CONSTRAINT IF EXISTS downtime_events_reason_code_check;
ALTER TABLE downtime_events DROP CONSTRAINT IF EXISTS chk_downtime_reason_code;

ALTER TABLE downtime_events ADD CONSTRAINT chk_downtime_reason_code CHECK (
    reason_code IN (
        'BREAKDOWN', 
        'SETUP', 
        'MATERIAL_SHORTAGE', 
        'OTHER',
        'MICRO_STOP', 
        'TOOLING_JAM', 
        'OPERATOR_PAUSE', 
        'UNPLANNED_MAINTENANCE'
    )
);

-- 2. Add trigger_source, is_micro_stop, root_cause_prompted_at, root_cause_acknowledged_at
ALTER TABLE downtime_events ADD COLUMN IF NOT EXISTS trigger_source VARCHAR(32) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE downtime_events DROP CONSTRAINT IF EXISTS chk_downtime_trigger_source;
ALTER TABLE downtime_events ADD CONSTRAINT chk_downtime_trigger_source CHECK (
    trigger_source IN ('MANUAL', 'AUTOMATED_SENSOR', 'HEARTBEAT_TIMEOUT')
);

ALTER TABLE downtime_events ADD COLUMN IF NOT EXISTS is_micro_stop BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE downtime_events ADD COLUMN IF NOT EXISTS root_cause_prompted_at TIMESTAMPTZ NULL;
ALTER TABLE downtime_events ADD COLUMN IF NOT EXISTS root_cause_acknowledged_at TIMESTAMPTZ NULL;

-- 3. Relax resolution check constraint to allow system auto-resolved micro-stops where resolved_by is null
ALTER TABLE downtime_events DROP CONSTRAINT IF EXISTS chk_downtime_resolution;
ALTER TABLE downtime_events ADD CONSTRAINT chk_downtime_resolution CHECK (
    (end_time IS NULL AND resolution_note IS NULL) OR
    (end_time IS NOT NULL AND resolution_note IS NOT NULL)
);

-- 4. Create performance indexes for micro-stop metrics and pending root-cause acknowledgments
CREATE INDEX IF NOT EXISTS ix_downtime_micro_stops 
    ON downtime_events (machine_id, is_micro_stop, start_time DESC);

CREATE INDEX IF NOT EXISTS ix_downtime_pending_root_cause 
    ON downtime_events (machine_id, root_cause_prompted_at) 
    WHERE root_cause_acknowledged_at IS NULL AND end_time IS NULL;
