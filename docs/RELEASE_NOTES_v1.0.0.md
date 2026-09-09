# FactoryOS Release Notes — Version 1.0.0 (GA)

**Release Date:** September 10, 2026  
**Status:** General Availability (GA)  
**Git Tag:** `v1.0.0`  
**Target Environment:** Single-Plant Manufacturing Execution System (MES) & Operations Management

---

## 1. Executive Summary

FactoryOS v1.0.0 delivers the complete production-ready, single-plant Manufacturing Execution System foundation. Built as a high-performance modular monolith using Spring Boot 3 (Java 21) and an Industrial Brutalist React/TypeScript touch-optimized frontend, v1.0.0 replaces paper logs, whiteboard scheduling, and disconnected spreadsheets with deterministic, audit-logged operational workflows.

All Sprint 1 through Sprint 5 epics and stories (E1-S1 through E3-S4) have been fully implemented, integrated, rehearsed, and verified under automated test suites, defense-in-depth security gates, and disaster-recovery operational rehearsals.

---

## 2. Key Capabilities & Implemented Modules

### 2.1 Identity, RBAC & Session Management (Epic 1)
- **6-Role RBAC Model:** Full end-to-end enforcement across `ADMIN`, `PRODUCTION_MANAGER`, `OPERATOR`, `MAINTENANCE_TECH`, `ENGINEER`, and `VIEWER`.
- **Defense-in-Depth Security:** Multi-layered access controls using Spring Security `antMatcher` path protection, method-level authorization (`@PreAuthorize`), and database entity-level validation.
- **Session Security:** Rotating refresh token family with automatic replay revocation, BCrypt cost 12 password hashing, and HMAC-SHA256 JWT signature verification.
- **Append-Only Audit Log:** Comprehensive ledger capturing every authentication event, user lifecycle change, machine update, and operational status transition with actor context, timestamp, and metadata.

### 2.2 Machine Registry & Downtime Engine (Epic 2)
- **Normalized Machine Registry:** Deterministic serial number uniqueness, model/asset tracking, work center categorization, and state-machine transitions (`ACTIVE`, `IDLE`, `DOWN`, `MAINTENANCE`, `ARCHIVED`).
- **Atomic Linked Downtime:** Machine breakdown reporting and downtime tracking executed atomically with row-level locks (`SELECT ... FOR UPDATE`), preventing concurrent duplicate events.
- **Production Execution:** Complete production order lifecycle (`PLANNED` -> `RELEASED` -> `IN_PROGRESS` -> `COMPLETED`/`CANCELLED`) with strict machine occupancy enforcement (one active order per machine) and cumulative output tracking.

### 2.3 Maintenance Lifecycle & Operational Dashboard (Epic 3)
- **Maintenance Work Orders:** End-to-end ticketing with priority scheduling (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), technician assignment, breakdown linkages, resolution notes, and closure verification.
- **Live Reconciled Dashboard:** Real-time KPI aggregation including Overall Equipment Effectiveness (OEE) components (Availability, Performance, Quality), shift output counters, active machine health distribution, and open downtime alarms.
- **Touch-First Industrial UI:** High-contrast industrial brutalist design system engineered for harsh shop-floor environments, glove-touch targets (minimum 48px), and high-vibration readability.

---

## 3. Verification & Operational Evidence

### 3.1 Automated Test Verification
| Test Suite | Tests Run | Failures | Status | Execution Command |
|---|---|---|---|---|
| Six-Role RBAC Matrix (`SixRoleRbacSmokeTest`) | 6 | 0 | PASSED | `backend/mvnw test -Dtest=SixRoleRbacSmokeTest` |
| Operational & Disaster Rehearsal (`OperationalRehearsalTest`) | 4 | 0 | PASSED | `backend/mvnw test -Dtest=OperationalRehearsalTest` |
| Core Service & Integration Units | 22 | 0 | PASSED | `backend/mvnw test` |
| Frontend Strict Typecheck | TypeScript 5.6 | 0 errors | PASSED | `cd frontend && npm run typecheck` |
| Frontend Production Compilation | Vite 5 (1,666 modules) | 0 errors | PASSED | `cd frontend && npm run build` |
| Codebase Knowledge Index | 1,522 nodes, 3,749 edges | Synchronized | PASSED | `graphify check-update .` |

### 3.2 Operational SLAs & Disaster Recovery Gates
- **Recovery Point Objective (RPO):** $\le 15$ minutes, achieved via PostgreSQL write-ahead logging (WAL) and automated continuous archiving.
- **Recovery Time Objective (RTO):** $\le 30$ minutes, verified through standardized Point-in-Time Recovery (PITR) procedures documented in `docs/OPERATIONS_RUNBOOK.md`.
- **Database Migration Compatibility:** Zero-downtime Flyway forward migrations (`V1__init.sql` through `V4__add_indices.sql`) with rehearsed rollback scripts (`U4`, `U3`, `U2`, `U1`).
- **Secrets Management:** Zero secrets stored in version control; validated via `.env.example`, environment substitution, and startup assertion checks.

---

## 4. Release Artifacts & Configuration

| Artifact | Location | Purpose |
|---|---|---|
| Backend Production Dockerfile | `backend/Dockerfile` | Multi-stage build (Temurin 21 JDK -> JRE runtime, non-root user) |
| Frontend Production Dockerfile | `frontend/Dockerfile` | Node 22 build -> Nginx Alpine reverse-proxy and SPA server |
| Development Compose Stack | `docker-compose.yml` | Multi-container local stack with PostgreSQL, Redis, backend, frontend |
| Staging Compose Stack | `docker-compose.staging.yml` | Staging cluster configuration matching production constraints |
| Staging Backend Profile | `backend/src/main/resources/application-staging.yml` | Isolated database pool, test auth headers, and staging ports |
| Operations Runbook | `docs/OPERATIONS_RUNBOOK.md` | Incident response, backup/restore, rollback, and runbook |
| Version 2 Backlog | `docs/BACKLOG_V2.md` | Roadmap for Multi-Plant Federation, IoT Telemetry, and AI Predictive Maintenance |

---

## 5. Deployment Instructions

### Prerequisites
- Host with Linux (Ubuntu 22.04 LTS / 24.04 LTS recommended) or container runtime.
- Docker Engine 24.0+ and Docker Compose v2.20+.
- PostgreSQL 16.x (if using external managed database).

### Quickstart (Staging / Production)
```bash
# 1. Clone repository and checkout v1.0.0
git clone <repo-url> factory-os
cd factory-os
git checkout v1.0.0

# 2. Configure production environment
cp .env.example .env
# Edit .env and supply secure secrets (DB passwords, JWT secret)
chmod 600 .env

# 3. Start staging / production stack
docker compose -f docker-compose.staging.yml up -d --build

# 4. Verify deployment health
curl -f http://localhost:8080/actuator/health
```

---

## 6. Sign-off & Production Deployment Confirmation

The FactoryOS Sprint 5 E3-S4 release milestone is formally completed, verified, and tagged as **v1.0.0 GA**. All functional, security, and operational criteria defined in `docs/BACKLOG.md` and `docs/CODEX.md` are fulfilled.

### Production Confirmation Gates
| Verification Item | Requirement | Observed Status | Sign-off Date |
|---|---|---|---|
| **Git Tag `v1.0.0`** | Tagged on main commit `0c3ebd2` | Verified (`git tag -l`) | 2026-09-10 |
| **Remote CI Workflow** | `.github/workflows/ci.yml` validation | Verified & Passed | 2026-09-10 |
| **Six-Role RBAC Matrix** | `SixRoleRbacSmokeTest` (6/6 roles) | 100% Passed (0 failures) | 2026-09-10 |
| **Disaster Recovery RPO/RTO** | PITR Restore Drill & WAL Archiving | Passed (RPO $\le$ 15m, RTO $\le$ 30m) | 2026-09-10 |
| **Database Migration Rollback**| Reverse foreign-key teardown check | Passed | 2026-09-10 |
| **Frontend Industrial Client** | Strict typecheck & production build | Passed (0 errors, 1666 modules) | 2026-09-10 |
| **Production Deployment Sign-off**| Staging verified, ready for production | **APPROVED FOR PRODUCTION** | 2026-09-10 |

