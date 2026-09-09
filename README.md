# FactoryOS // Manufacturing Operations Platform

FactoryOS is a high-reliability single-plant manufacturing execution and operations platform engineered with a Spring Boot 3.3 modular monolith backend, PostgreSQL 16 database, and an industrial brutalist React/TypeScript frontend client.

---

## 1. Quickstart & Local Execution

To start the complete platform (PostgreSQL, Flyway migrations, Spring Boot backend, and Nginx frontend):

```bash
# 1. Clone & create environment file
cp .env.example .env

# 2. Launch container stack
docker compose up --build
```

### Access Endpoints
- **Frontend Dashboard:** [http://localhost](http://localhost) (or `http://localhost:5173` via `npm --prefix frontend run dev`)
- **Backend REST API:** [http://localhost:8080/api/v1](http://localhost:8080/api/v1)
- **Actuator Health:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### Default Bootstrap Administrator Credentials
- **Email:** `admin@factoryos.local`
- **Initial Password:** `AdminBootstrap2026!Secure`
- *(System will enforce immediate permanent password rotation upon initial sign-in).*

---

## 2. Core Architecture

```
Factory OS/
├── backend/                  # Spring Boot 3.3.2 + Java 21 Modular Monolith
│   ├── src/main/java/com/factoryos/
│   │   ├── modules/auth/     # JWT RS256 + Opaque Refresh Tokens, IAM & RBAC
│   │   ├── modules/machine/  # Machine Fleet Registry & Status State Machine
│   │   ├── modules/downtime/ # Stoppages, Reason Codes, Outage Duration
│   │   ├── modules/production/# Batch Orders, Good vs Scrap Counters
│   │   ├── modules/maintenance/# Work Orders, Priority, Technician Dispatch
│   │   ├── modules/operations/# Cross-domain Transaction Orchestrator
│   │   ├── modules/reporting/# Plant KPI Summaries & Real-time OEE
│   │   └── modules/audit/    # Transactional Append-Only Audit Bus
│   └── src/test/             # Automated Unit & Service Test Suite
├── frontend/                 # React 18 + TypeScript + Vite + Tailwind CSS
│   ├── src/components/views/ # Dashboard, Machines, Downtime, Production, MRO, IAM, Audit
│   └── src/styles/           # Industrial Brutalist Design System
├── docker-compose.yml        # Multi-container local & production orchestration
└── docs/                     # Comprehensive Architecture, PRD, Codex, and Runbooks
```

---

## 3. Documentation & Operational Runbooks

| Document | Description |
|---|---|
| [RELEASE_NOTES_v1.0.0](docs/RELEASE_NOTES_v1.0.0.md) | FactoryOS v1.0.0 GA Release Notes, SLAs, and verification evidence |
| [OPERATIONS_RUNBOOK](docs/OPERATIONS_RUNBOOK.md) | Backup/Restore (RPO &le; 15m, RTO &le; 30m), Rollback Runbook, 6-Role Smoke Test Matrix |
| [PRD](docs/PRD.md) | Product Requirements, User Personas, Permissions, and Scope |
| [Architecture](docs/ARCHITECTURE.md) | Domain module boundaries, transactional invariants, optimistic locking |
| [CODEX](docs/CODEX.md) | Engineering standards, code conventions, CI/CD pipeline |
| [Security](docs/Security.md) | Cryptography standards, authentication guards, audit retention |
| [BACKLOG](docs/BACKLOG.md) | MVP Epics, user stories, acceptance criteria (v1.0.0 GA Completed) |
| [BACKLOG_V2](docs/BACKLOG_V2.md) | FactoryOS 2.0 Roadmap: Multi-Plant, IIoT Telemetry, Predictive Maintenance |
