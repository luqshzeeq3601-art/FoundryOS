-- ==============================================================================
-- FoundryOS Migration V10: Prescriptive Automated Maintenance Work Orders,
-- Diagnostic Snapshots & 24-Hour Deduplication (Sprint 10 E6-S2)
-- ==============================================================================

-- 1. EXTEND MAINTENANCE_WORK_ORDERS TABLE WITH PRESCRIPTIVE FIELDS
ALTER TABLE maintenance_work_orders
    ADD COLUMN is_prescriptive BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN health_assessment_id UUID NULL REFERENCES machine_health_assessments(id) ON DELETE SET NULL,
    ADD COLUMN diagnostic_snapshot JSONB NULL,
    ADD COLUMN suspected_subsystem VARCHAR(128) NULL,
    ADD COLUMN recommended_parts TEXT NULL,
    ADD COLUMN last_triggered_at TIMESTAMPTZ NULL;

-- 2. CREATE PERFORMANCE INDEXES FOR DEDUPLICATION & FILTERING
CREATE INDEX idx_mwo_prescriptive_machine_status
    ON maintenance_work_orders (machine_id, is_prescriptive, status, created_at)
    WHERE is_deleted = false;

CREATE INDEX idx_mwo_health_assessment
    ON maintenance_work_orders (health_assessment_id)
    WHERE health_assessment_id IS NOT NULL;
