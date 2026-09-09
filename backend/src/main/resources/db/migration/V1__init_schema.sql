-- ==============================================================================
-- FactoryOS V1 Initial Database Schema Migration
-- Standard PostgreSQL 16+ DDL
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. ROLES TABLE
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(32) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 2. USERS TABLE
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    must_change_password BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- Add Role audit foreign keys back to users
ALTER TABLE roles 
    ADD CONSTRAINT fk_roles_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_roles_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE RESTRICT;

-- 3. MACHINES TABLE
CREATE TABLE machines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    serial_number VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    location VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'IDLE' CHECK (status IN ('IDLE', 'RUNNING', 'DOWN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 4. DOWNTIME_EVENTS TABLE
CREATE TABLE downtime_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id UUID NOT NULL REFERENCES machines(id) ON DELETE RESTRICT,
    reason_code VARCHAR(32) NOT NULL CHECK (reason_code IN ('BREAKDOWN', 'SETUP', 'MATERIAL_SHORTAGE', 'OTHER')),
    description VARCHAR(2000) NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NULL CHECK (end_time IS NULL OR end_time >= start_time),
    resolution_note VARCHAR(2000) NULL,
    resolved_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uq_downtime_event_machine UNIQUE (id, machine_id),
    CONSTRAINT chk_downtime_resolution CHECK (
        (end_time IS NULL AND resolution_note IS NULL AND resolved_by IS NULL) OR
        (end_time IS NOT NULL AND resolution_note IS NOT NULL AND resolved_by IS NOT NULL)
    )
);

-- 5. PRODUCTION_ORDERS TABLE
CREATE TABLE production_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(40) NOT NULL UNIQUE,
    machine_id UUID NOT NULL REFERENCES machines(id) ON DELETE RESTRICT,
    product_code VARCHAR(100) NOT NULL,
    product_description VARCHAR(500) NULL,
    planned_quantity INTEGER NOT NULL CHECK (planned_quantity > 0),
    good_quantity INTEGER NOT NULL DEFAULT 0 CHECK (good_quantity >= 0),
    scrap_quantity INTEGER NOT NULL DEFAULT 0 CHECK (scrap_quantity >= 0),
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'RELEASED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    started_at TIMESTAMPTZ NULL,
    completed_at TIMESTAMPTZ NULL,
    closed_at TIMESTAMPTZ NULL,
    closure_note VARCHAR(2000) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 6. MAINTENANCE_WORK_ORDERS TABLE
CREATE TABLE maintenance_work_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    work_order_number VARCHAR(40) NOT NULL UNIQUE,
    machine_id UUID NOT NULL REFERENCES machines(id) ON DELETE RESTRICT,
    downtime_event_id UUID NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    assigned_to UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    due_at TIMESTAMPTZ NULL,
    started_at TIMESTAMPTZ NULL,
    completed_at TIMESTAMPTZ NULL,
    closed_at TIMESTAMPTZ NULL,
    completion_note VARCHAR(4000) NULL,
    cancellation_note VARCHAR(2000) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT fk_maintenance_downtime_machine FOREIGN KEY (downtime_event_id, machine_id) 
        REFERENCES downtime_events(id, machine_id) ON DELETE RESTRICT,
    CONSTRAINT chk_maintenance_assignment CHECK (
        (status = 'OPEN' AND assigned_to IS NULL) OR
        (status IN ('ASSIGNED', 'IN_PROGRESS', 'COMPLETED') AND assigned_to IS NOT NULL) OR
        (status = 'CANCELLED')
    ),
    CONSTRAINT chk_maintenance_completion CHECK (
        (status != 'COMPLETED' OR completion_note IS NOT NULL)
    ),
    CONSTRAINT chk_maintenance_cancellation CHECK (
        (status != 'CANCELLED' OR cancellation_note IS NOT NULL)
    )
);

-- 7. REFRESH_SESSIONS TABLE
CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    family_id UUID NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ NULL,
    revoked_at TIMESTAMPTZ NULL,
    replacement_id UUID NULL REFERENCES refresh_sessions(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 8. AUDIT_EVENTS TABLE (Append-Only)
CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID NOT NULL,
    before_data JSONB NULL,
    after_data JSONB NULL,
    trace_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by UUID NULL REFERENCES users(id) ON DELETE RESTRICT
);

-- ==============================================================================
-- INDEXES
-- ==============================================================================

-- Unique normalized email
CREATE UNIQUE INDEX uq_users_email_normalized ON users (lower(trim(email)));

-- Partial unique: At most one open downtime event per machine
CREATE UNIQUE INDEX uq_downtime_one_open_per_machine ON downtime_events (machine_id) WHERE end_time IS NULL;

-- Partial unique: At most one IN_PROGRESS production order per machine
CREATE UNIQUE INDEX uq_production_one_active_per_machine ON production_orders (machine_id) WHERE status = 'IN_PROGRESS';

-- Query optimization indexes
CREATE INDEX ix_users_role_id ON users(role_id);
CREATE INDEX ix_downtime_machine_start ON downtime_events (machine_id, start_time DESC);
CREATE INDEX ix_maintenance_machine_status ON maintenance_work_orders (machine_id, status);
CREATE INDEX ix_maintenance_assignee_status_due ON maintenance_work_orders (assigned_to, status, due_at);
CREATE INDEX ix_production_machine_status_created ON production_orders (machine_id, status, created_at DESC);
CREATE INDEX ix_audit_entity_created ON audit_events (entity_type, entity_id, created_at DESC);
CREATE INDEX ix_audit_actor_created ON audit_events (actor_id, created_at DESC);
CREATE INDEX ix_refresh_sessions_user ON refresh_sessions (user_id);
CREATE INDEX ix_refresh_sessions_family ON refresh_sessions (family_id);

-- ==============================================================================
-- SEED DATA
-- ==============================================================================

INSERT INTO roles (id, name, created_at, updated_at) VALUES
    ('00000000-0000-0000-0000-000000000001', 'ADMIN', now(), now()),
    ('00000000-0000-0000-0000-000000000002', 'PRODUCTION_MANAGER', now(), now()),
    ('00000000-0000-0000-0000-000000000003', 'ENGINEER', now(), now()),
    ('00000000-0000-0000-0000-000000000004', 'TECHNICIAN', now(), now()),
    ('00000000-0000-0000-0000-000000000005', 'OPERATOR', now(), now()),
    ('00000000-0000-0000-0000-000000000006', 'VIEWER', now(), now())
ON CONFLICT (name) DO NOTHING;
