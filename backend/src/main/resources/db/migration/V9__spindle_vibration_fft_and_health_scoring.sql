-- ==============================================================================
-- FoundryOS Migration V9: Spindle Vibration Spectral Analysis (FFT),
-- ISO 10816 Anomaly Detection, and Machine Health Scoring (Sprint 10 E6-S1)
-- ==============================================================================

-- 1. VIBRATION BURST SAMPLES TABLE
CREATE TABLE vibration_burst_samples (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id UUID NOT NULL REFERENCES machines(id) ON DELETE CASCADE,
    plant_id UUID NULL REFERENCES plants(id) ON DELETE RESTRICT,
    axis VARCHAR(32) NOT NULL DEFAULT 'RADIAL_X',
    sample_rate_hz DOUBLE PRECISION NOT NULL DEFAULT 2048.0,
    sample_count INT NOT NULL DEFAULT 1024,
    running_speed_rpm DOUBLE PRECISION NULL,
    rms_velocity_mm_s DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    peak_acceleration_g DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    crest_factor DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    kurtosis DOUBLE PRECISION NOT NULL DEFAULT 3.0,
    bearing_temperature_c DOUBLE PRECISION NULL,
    raw_samples JSONB NULL,
    captured_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 2. VIBRATION SPECTRAL PEAKS TABLE
CREATE TABLE vibration_spectral_peaks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    burst_id UUID NOT NULL REFERENCES vibration_burst_samples(id) ON DELETE CASCADE,
    frequency_hz DOUBLE PRECISION NOT NULL,
    amplitude_mm_s DOUBLE PRECISION NOT NULL,
    order_multiple DOUBLE PRECISION NULL,
    fault_harmonic_type VARCHAR(64) NOT NULL DEFAULT 'NORMAL',
    confidence DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. MACHINE HEALTH ASSESSMENTS TABLE
CREATE TABLE machine_health_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id UUID NOT NULL REFERENCES machines(id) ON DELETE CASCADE,
    plant_id UUID NULL REFERENCES plants(id) ON DELETE RESTRICT,
    latest_burst_id UUID NULL REFERENCES vibration_burst_samples(id) ON DELETE SET NULL,
    health_score INT NOT NULL CHECK (health_score >= 0 AND health_score <= 100),
    health_status VARCHAR(32) NOT NULL,
    iso_severity_zone VARCHAR(16) NOT NULL,
    vibration_class VARCHAR(32) NOT NULL DEFAULT 'CLASS_II_MEDIUM',
    rms_velocity_mm_s DOUBLE PRECISION NOT NULL,
    spindle_temperature_c DOUBLE PRECISION NULL,
    dominant_fault_type VARCHAR(64) NULL,
    diagnosis_summary TEXT NOT NULL,
    recommended_action TEXT NULL,
    assessed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 4. INDEXES FOR HIGH-THROUGHPUT VIBRATION QUERYING
CREATE INDEX ix_vibration_bursts_machine_captured ON vibration_burst_samples(machine_id, captured_at DESC);
CREATE INDEX ix_vibration_bursts_plant ON vibration_burst_samples(plant_id);
CREATE INDEX ix_vibration_peaks_burst ON vibration_spectral_peaks(burst_id);
CREATE INDEX ix_vibration_peaks_frequency ON vibration_spectral_peaks(frequency_hz);
CREATE INDEX ix_machine_health_machine_assessed ON machine_health_assessments(machine_id, assessed_at DESC);
CREATE INDEX ix_machine_health_plant_score ON machine_health_assessments(plant_id, health_score);
CREATE INDEX ix_machine_health_status ON machine_health_assessments(health_status);
CREATE INDEX ix_machine_health_zone ON machine_health_assessments(iso_severity_zone);

-- 5. SEED BASELINE VIBRATION AND HEALTH ASSESSMENTS FOR EXISTING MACHINES
INSERT INTO vibration_burst_samples (
    id, machine_id, plant_id, axis, sample_rate_hz, sample_count, running_speed_rpm,
    rms_velocity_mm_s, peak_acceleration_g, crest_factor, kurtosis, bearing_temperature_c,
    captured_at, created_at, updated_at
)
SELECT 
    '00000000-0000-0000-0000-000000000901'::uuid,
    m.id,
    m.plant_id,
    'RADIAL_X',
    2048.0,
    1024,
    3000.0,
    1.42,
    0.85,
    2.1,
    2.95,
    42.5,
    now() - interval '10 minutes',
    now(),
    now()
FROM machines m
WHERE m.is_deleted = false
ORDER BY m.created_at ASC
LIMIT 1
ON CONFLICT (id) DO NOTHING;

INSERT INTO vibration_spectral_peaks (
    id, burst_id, frequency_hz, amplitude_mm_s, order_multiple, fault_harmonic_type, confidence
) VALUES
    ('00000000-0000-0000-0000-000000000911', '00000000-0000-0000-0000-000000000901', 50.0, 1.15, 1.0, '1X_RPM', 0.98),
    ('00000000-0000-0000-0000-000000000912', '00000000-0000-0000-0000-000000000901', 100.0, 0.22, 2.0, 'MISALIGNMENT_2X', 0.85),
    ('00000000-0000-0000-0000-000000000913', '00000000-0000-0000-0000-000000000901', 150.0, 0.08, 3.0, 'LOOSENESS_3X', 0.70)
ON CONFLICT (id) DO NOTHING;

INSERT INTO machine_health_assessments (
    id, machine_id, plant_id, latest_burst_id, health_score, health_status,
    iso_severity_zone, vibration_class, rms_velocity_mm_s, spindle_temperature_c,
    dominant_fault_type, diagnosis_summary, recommended_action, assessed_at, created_at, updated_at
)
SELECT
    '00000000-0000-0000-0000-000000000921'::uuid,
    m.id,
    m.plant_id,
    '00000000-0000-0000-0000-000000000901'::uuid,
    96,
    'EXCELLENT',
    'ZONE_A',
    'CLASS_II_MEDIUM',
    1.42,
    42.5,
    'NORMAL',
    'Spindle vibration velocity (1.42 mm/s RMS) well within ISO 10816 Zone A. Bearing temperatures nominal at 42.5°C.',
    'Continue standard operational monitoring. Next scheduled ultrasonic lubrication in 240 operating hours.',
    now() - interval '10 minutes',
    now(),
    now()
FROM machines m
WHERE m.is_deleted = false
ORDER BY m.created_at ASC
LIMIT 1
ON CONFLICT (id) DO NOTHING;
