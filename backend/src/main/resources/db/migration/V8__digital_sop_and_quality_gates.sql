-- ==============================================================================
-- FactoryOS V8 Database Schema Migration
-- Epic 8: Digital Standard Operating Procedures (SOP), Interactive Checklists & Quality Sign-Off Gates (E8-S2)
-- ==============================================================================

-- 1. STANDARD_OPERATING_PROCEDURES TABLE
CREATE TABLE standard_operating_procedures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sop_code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    product_code VARCHAR(100) NOT NULL,
    plant_id UUID NULL REFERENCES plants(id) ON DELETE RESTRICT,
    version VARCHAR(20) NOT NULL DEFAULT '1.0',
    category VARCHAR(50) NOT NULL DEFAULT 'ASSEMBLY' CHECK (category IN ('ASSEMBLY', 'MACHINING', 'QUALITY_INSPECTION', 'SAFETY', 'MAINTENANCE')),
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    description TEXT NULL,
    cad_drawing_url VARCHAR(500) NULL,
    safety_precautions TEXT NULL,
    estimated_duration_minutes INT NOT NULL DEFAULT 30,
    requires_quality_sign_off BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version_lock BIGINT NOT NULL DEFAULT 0 CHECK (version_lock >= 0)
);

-- 2. SOP_STEPS TABLE
CREATE TABLE sop_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sop_id UUID NOT NULL REFERENCES standard_operating_procedures(id) ON DELETE CASCADE,
    step_number INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    instruction_text TEXT NOT NULL,
    step_type VARCHAR(30) NOT NULL DEFAULT 'INSTRUCTION' CHECK (step_type IN ('INSTRUCTION', 'NUMERIC_MEASUREMENT', 'CHECKLIST_PASS_FAIL', 'PHOTO_CAPTURE', 'BARCODE_VERIFICATION')),
    image_url VARCHAR(500) NULL,
    cad_view_node VARCHAR(100) NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT true,
    nominal_value DOUBLE PRECISION NULL,
    min_tolerance DOUBLE PRECISION NULL,
    max_tolerance DOUBLE PRECISION NULL,
    unit_of_measure VARCHAR(30) NULL,
    safety_alert VARCHAR(500) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_sop_step_number UNIQUE (sop_id, step_number)
);

-- 3. SOP_EXECUTION_SESSIONS TABLE
CREATE TABLE sop_execution_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sop_id UUID NOT NULL REFERENCES standard_operating_procedures(id) ON DELETE RESTRICT,
    production_order_id UUID NOT NULL REFERENCES production_orders(id) ON DELETE CASCADE,
    machine_id UUID NULL REFERENCES machines(id) ON DELETE RESTRICT,
    plant_id UUID NOT NULL REFERENCES plants(id) ON DELETE RESTRICT,
    session_status VARCHAR(25) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (session_status IN ('IN_PROGRESS', 'PASSED', 'FAILED', 'REJECTED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ NULL,
    operator_user_id UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    operator_name VARCHAR(100) NULL,
    quality_sign_off_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    quality_sign_off_name VARCHAR(100) NULL,
    quality_sign_off_at TIMESTAMPTZ NULL,
    quality_sign_off_notes TEXT NULL,
    total_steps INT NOT NULL DEFAULT 0,
    completed_steps INT NOT NULL DEFAULT 0,
    passed_steps INT NOT NULL DEFAULT 0,
    failed_steps INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 4. SOP_STEP_EXECUTION_RECORDS TABLE
CREATE TABLE sop_step_execution_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES sop_execution_sessions(id) ON DELETE CASCADE,
    step_id UUID NOT NULL REFERENCES sop_steps(id) ON DELETE RESTRICT,
    step_number INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PASSED', 'FAILED', 'SKIPPED')),
    numeric_value DOUBLE PRECISION NULL,
    is_within_tolerance BOOLEAN NULL,
    text_feedback VARCHAR(1000) NULL,
    photo_evidence_url VARCHAR(1000) NULL,
    barcode_scanned VARCHAR(100) NULL,
    verified_by_user_id UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    verified_by_name VARCHAR(100) NULL,
    verified_at TIMESTAMPTZ NULL,
    notes VARCHAR(1000) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_session_step UNIQUE (session_id, step_id)
);

-- 5. QUALITY_SIGN_OFF_GATES TABLE
CREATE TABLE quality_sign_off_gates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    production_order_id UUID NOT NULL UNIQUE REFERENCES production_orders(id) ON DELETE CASCADE,
    sop_id UUID NOT NULL REFERENCES standard_operating_procedures(id) ON DELETE RESTRICT,
    session_id UUID NULL REFERENCES sop_execution_sessions(id) ON DELETE SET NULL,
    gate_status VARCHAR(25) NOT NULL DEFAULT 'PENDING' CHECK (gate_status IN ('PENDING', 'PASSED', 'FAILED', 'BYPASSED')),
    requires_quality_role BOOLEAN NOT NULL DEFAULT false,
    signed_off_by_user_id UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    signed_off_by_name VARCHAR(100) NULL,
    signed_off_at TIMESTAMPTZ NULL,
    sign_off_comments TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 6. INDEXES
CREATE INDEX ix_sop_product ON standard_operating_procedures(product_code);
CREATE INDEX ix_sop_plant ON standard_operating_procedures(plant_id);
CREATE INDEX ix_sop_steps_sop ON sop_steps(sop_id, step_number);
CREATE INDEX ix_sop_sessions_order ON sop_execution_sessions(production_order_id);
CREATE INDEX ix_sop_sessions_status ON sop_execution_sessions(session_status);
CREATE INDEX ix_sop_records_session ON sop_step_execution_records(session_id);
CREATE INDEX ix_quality_gates_order ON quality_sign_off_gates(production_order_id, gate_status);

-- 7. SEED INITIAL DIGITAL SOPS & CHECKLIST BLUEPRINTS
INSERT INTO standard_operating_procedures (id, sop_code, title, product_code, version, category, status, description, cad_drawing_url, safety_precautions, estimated_duration_minutes, requires_quality_sign_off, created_at, updated_at) VALUES
    ('00000000-0000-0000-0000-000000000701', 'SOP-TURBINE-001', 'Turbine Blade Precision Assembly & Dynamic Balancing SOP', 'TURBINE-BLADE-V2', '2.1', 'ASSEMBLY', 'PUBLISHED', 'Complete 5-stage precision assembly, dynamic balancing, and coordinate measuring machine (CMM) dimensional verification.', '/cad/models/turbine_blade_v2.dwg', 'Mandatory safety goggles, steel-toe footwear, and hearing protection during high-speed balancing spin.', 35, true, now(), now()),
    ('00000000-0000-0000-0000-000000000702', 'SOP-CAST-AUSTIN-002', 'Cast Turbine Root High-Tolerance Machining & NDT Gate', 'CAST-TURBINE-AUSTIN', '1.4', 'MACHINING', 'PUBLISHED', 'Standard operating procedure for CNC root milling, ultrasonic non-destructive testing (NDT), and surface roughness inspection.', '/cad/models/cast_turbine_austin.dwg', 'Verify coolant flow prior to high-feed milling. Lock out machine enclosure during tool inspection.', 45, true, now(), now()),
    ('00000000-0000-0000-0000-000000000703', 'SOP-MOTOR-SHAFT-003', 'Electric Drive Motor Shaft Precision Turning & Runout Inspection', 'MOTOR-SHAFT-BERLIN', '3.0', 'ASSEMBLY', 'PUBLISHED', 'High-speed ceramic turning, shaft runout tolerance verification, and magnetic particle inspection (MPI).', '/cad/models/motor_shaft_berlin.dwg', 'Wear cut-resistant thermal gloves when handling hot workpieces straight off lathe spindle.', 25, true, now(), now())
ON CONFLICT (sop_code) DO NOTHING;

-- Seed Steps for SOP-TURBINE-001
INSERT INTO sop_steps (id, sop_id, step_number, title, instruction_text, step_type, image_url, cad_view_node, is_mandatory, nominal_value, min_tolerance, max_tolerance, unit_of_measure, safety_alert, created_at, updated_at) VALUES
    ('00000000-0000-0000-0000-000000000711', '00000000-0000-0000-0000-000000000701', 1, 'Pre-Assembly Material & Traveler Barcode Verification', 'Scan 2D DataMatrix on raw casting traveler card and verify alloy batch certification with Bill of Materials.', 'BARCODE_VERIFICATION', '/images/sop/turbine_scan.png', 'ROOT_TRAVELER_DATUM', true, NULL, NULL, NULL, NULL, 'Do not process uncertified titanium alloy lots.', now(), now()),
    ('00000000-0000-0000-0000-000000000712', '00000000-0000-0000-0000-000000000701', 2, 'Airfoil Root Thickness Measurement (CMM)', 'Measure root flange thickness across micrometer caliper points A1 through A4. Verify nominal 14.85mm within +/-0.03mm tolerance.', 'NUMERIC_MEASUREMENT', '/images/sop/cmm_root.png', 'AIRFOIL_ROOT_A1', true, 14.85, 14.82, 14.88, 'mm', 'Wipe contact points clean before placing digital micrometer anvil.', now(), now()),
    ('00000000-0000-0000-0000-000000000713', '00000000-0000-0000-0000-000000000701', 3, 'Spindle Dynamic Balance & Vibration Check', 'Mount blade assembly onto balancing arbor and spin up to 12,000 RPM. Measure peak radial vibration displacement.', 'NUMERIC_MEASUREMENT', '/images/sop/vibration_check.png', 'ROTOR_SPINDLE_HUB', true, 0.45, 0.00, 0.80, 'mm/s', 'Ensure balance chamber blast door is completely interlocked.', now(), now()),
    ('00000000-0000-0000-0000-000000000714', '00000000-0000-0000-0000-000000000701', 4, 'Fluorescent Penetrant Dye Visual Inspection', 'Inspect trailing edge under UV lamp for micro-cracks, porosity voids, or coating delamination. Confirm zero visible surface fissures.', 'CHECKLIST_PASS_FAIL', '/images/sop/dye_penetrant.png', 'TRAILING_EDGE_UV', true, NULL, NULL, NULL, NULL, 'Wear UV-filtering protective eyewear during lamp inspection.', now(), now()),
    ('00000000-0000-0000-0000-000000000715', '00000000-0000-0000-0000-000000000701', 5, 'Final Assembly Photographic Evidence Capture', 'Capture high-resolution tablet camera photo of finished serialized blade tip showing laser-etched serial number and laser coating stamp.', 'PHOTO_CAPTURE', '/images/sop/camera_capture.png', 'TIP_SHROUD_SERIAL', true, NULL, NULL, NULL, NULL, 'Ensure illumination lamp is active and serial engraving is sharply in focus.', now(), now())
ON CONFLICT (sop_id, step_number) DO NOTHING;
