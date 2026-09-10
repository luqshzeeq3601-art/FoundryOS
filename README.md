# FoundryOS

[![CI](https://github.com/luqshzeeq3601-art/FoundryOS/actions/workflows/ci.yml/badge.svg)](https://github.com/luqshzeeq3601-art/FoundryOS/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Manufacturing execution system for single-plant and multi-plant operations.
Tracks machines, downtime, production batches, maintenance work orders, and OEE
across the shop floor. Built as a Spring Boot modular monolith with a React
frontend designed for factory-floor touchscreens.

## Stack

| | |
|---|---|
| **Backend** | Java 21, Spring Boot 3.3, Spring Security (RS256 JWT), Flyway |
| **Database** | PostgreSQL 16 |
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS |
| **Infra** | Docker Compose, Nginx, GitHub Actions CI |

## Quickstart

```bash
git clone https://github.com/luqshzeeq3601-art/FoundryOS.git
cd FoundryOS
cp .env.example .env
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API | http://localhost:8080/api/v1 |
| Health check | http://localhost:8080/actuator/health |

Default login: `admin@foundryos.local` / `AdminBootstrap2026!Secure`
(password rotation enforced on first sign-in)

## Local development

Prerequisites: Node.js 20+, Java 21, PostgreSQL 16 (or `docker compose up -d postgres`)

```bash
# Backend
cd backend
./mvnw clean test          # run tests
./mvnw spring-boot:run     # start API on :8080

# Frontend
cd frontend
npm install
npm run dev                # Vite dev server on :5173
```

## What it does

**Machine management** — State machine with lifecycle transitions (idle, running,
down, maintenance, offline) and optimistic locking to prevent concurrent state
conflicts.

**Downtime tracking** — Categorized stoppages (mechanical, electrical,
operational, material shortage) with MTTR and MTBF calculations.

**OEE** — Real-time availability, performance, and quality metrics per line and
work center.

**Production** — Batch run tracking with target vs actual counts and scrap rate
monitoring.

**Maintenance** — Work orders with priority levels, technician assignment, and
sign-off checklists.

**Access control** — Six roles (`SYSTEM_ADMIN`, `PLANT_MANAGER`,
`PRODUCTION_SUPERVISOR`, `MAINTENANCE_ENGINEER`, `LINE_OPERATOR`, `AUDITOR`)
with RS256 JWTs and opaque refresh tokens.

**Barcode scanning** — Camera and hardware scanner support for machine lookup,
batch tracking, and work order assignment.

**Telemetry** — Simulated IIoT sensor ingestion (RPM, temperature, vibration,
power draw).

**Audit log** — Append-only event bus for compliance, shift handovers, and
configuration changes.

**Multi-tenant hierarchy** — Site → Plant → Line → Work Center structure for
multi-facility scaling.

## Project layout

```
backend/src/main/java/com/factoryos/
├── modules/auth/          # JWT auth, RBAC, user management
├── modules/machine/       # Machine registry, state machine
├── modules/downtime/      # Stoppage tracking, reason codes
├── modules/production/    # Batch orders, counters
├── modules/maintenance/   # Work orders, dispatch
├── modules/operations/    # Cross-module orchestration
├── modules/reporting/     # OEE, KPI summaries
├── modules/telemetry/     # Sensor metrics ingestion
├── modules/tenant/        # Multi-plant hierarchy
└── modules/audit/         # Append-only audit bus

frontend/src/
├── components/views/      # Page-level components
├── services/              # API client, auth interceptors
└── styles/                # Design system
```

## Documentation

Detailed docs live in [`docs/`](docs/):

- [Architecture](docs/ARCHITECTURE.md) — Module boundaries, transactional invariants, locking strategy
- [Architecture Essentials](docs/ARCHITECTURE-ESSENTIALS.md) — High-level system overview
- [PRD](docs/PRD.md) — Requirements, personas, permissions
- [Operations Runbook](docs/OPERATIONS_RUNBOOK.md) — Backup/restore, rollback, smoke tests
- [Security](docs/Security.md) — Crypto standards, auth guards, audit retention
- [Engineering Standards](docs/CODEX.md) — Code conventions, CI pipeline
- [v1.0.0 Release Notes](docs/RELEASE_NOTES_v1.0.0.md)
- [v2 Backlog](docs/BACKLOG_V2.md) — Multi-plant, IIoT, predictive maintenance roadmap

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Uses conventional commits and feature
branches.

## License

MIT — see [LICENSE](LICENSE).
