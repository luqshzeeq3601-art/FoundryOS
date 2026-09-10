<div align="center">

# FoundryOS

**Enterprise-Grade Manufacturing Execution & Operations Platform**

[![CI/CD Pipeline](https://github.com/luqshzeeq3601-art/FoundryOS/actions/workflows/ci.yml/badge.svg)](https://github.com/luqshzeeq3601-art/FoundryOS/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring_Boot-3.3.2-6DB33F.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![React 18](https://img.shields.io/badge/React-18.3-61DAFB.svg?logo=react)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.5-3178C6.svg?logo=typescript)](https://www.typescriptlang.org/)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-4169E1.svg?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?logo=docker)](https://www.docker.com/)

<p align="center">
  A high-reliability, single-to-multi-plant manufacturing execution system (MES) engineered with a Spring Boot modular monolith backend, PostgreSQL transactional persistence with Flyway migrations, and an industrial brutalist React/TypeScript frontend client.
</p>

[Quickstart](#quickstart--local-execution) • [Key Features](#key-features) • [Architecture](#core-architecture) • [Documentation](#documentation--runbooks) • [Contributing](#contributing)

</div>

---

## Overview

**FoundryOS** bridges the gap between raw shop-floor activity and executive decision-making. Built for high-throughput discrete and batch manufacturing environments, it provides real-time machine telemetry, stoppage tracking, overall equipment effectiveness (OEE) metrics, maintenance work order dispatch, barcode asset scanning, and immutable audit logs.

Designed with an **Industrial Brutalist UI** for high contrast and fast tactile feedback on factory floor touchscreens and rugged tablets.

---

## Key Features

- ⚙️ **Machine Fleet State Machine**: Strict lifecycle state transitions (`IDLE`, `RUNNING`, `DOWN`, `MAINTENANCE`, `OFFLINE`) with optimistic locking preventing race conditions.
- ⏱️ **Downtime & Stoppage Tracking**: Granular categorization (Mechanical, Electrical, Operational, Material Shortage) with automated MTTR/MTBF calculations.
- 📊 **Real-Time OEE Engine**: Live computation of Availability, Performance, and Quality indices across production lines and work centers.
- 📦 **Production Batch Runs**: Job order scheduling, target vs actual count tracking, and real-time scrap rate monitoring.
- 🛠️ **MRO Maintenance Work Orders**: Priority triage (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), technician dispatch, and checklist sign-off workflows.
- 🔒 **6-Role Granular RBAC**: Role-based access control (`SYSTEM_ADMIN`, `PLANT_MANAGER`, `PRODUCTION_SUPERVISOR`, `MAINTENANCE_ENGINEER`, `LINE_OPERATOR`, `AUDITOR`) powered by RS256 JWTs and opaque refresh tokens.
- 📷 **Barcode & Asset Scanner**: Camera-based and hardware scanner integration for instant machine lookup, batch tracking, and work order assignment.
- 📡 **Live Telemetry & IoT Ingestion**: Simulated & real IIoT sensor stream ingestion (RPM, temperature, vibration, power consumption).
- 📜 **Append-Only Audit Bus**: Immutable event recording for compliance, safety incidents, shift handovers, and system configuration adjustments.
- 🏢 **Multi-Plant Tenant Hierarchy**: Site -> Plant -> Line -> Work Center hierarchy ready for multi-facility enterprise scaling.

---

## Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| **Backend** | Spring Boot 3.3.2, Java 21 | Modular monolith API, domain events, validation |
| **Database** | PostgreSQL 16, Flyway | Relational persistence, schema versioning, ACID guarantees |
| **Security** | Spring Security 6, JJWT (RS256) | Role-based IAM, token revocation, stateless auth |
| **Frontend** | React 18, TypeScript 5.5, Vite | Single-page application, type safety, sub-second HMR |
| **Styling** | Tailwind CSS, Lucide Icons | Industrial brutalist high-contrast design system |
| **Containerization** | Docker, Docker Compose, Nginx | Multi-stage production builds and local dev orchestration |
| **CI/CD** | GitHub Actions | Automated linting, typechecking, Maven test suite, Docker packaging |

---

## Quickstart & Local Execution

### 1. Run with Docker Compose (Recommended)

To spin up the full production stack (PostgreSQL, Flyway migrations, Spring Boot API, and Nginx frontend):

```bash
# Clone the repository
git clone https://github.com/luqshzeeq3601-art/FoundryOS.git
cd FoundryOS

# Create environment configuration
cp .env.example .env

# Launch the container stack
docker compose up --build
```

### Access URLs
| Service | URL |
|---|---|
| **Frontend Application** | [http://localhost:3000](http://localhost:3000) (or port configured in `.env`) |
| **Backend REST API** | [http://localhost:8080/api/v1](http://localhost:8080/api/v1) |
| **Actuator Health & Metrics** | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) |

### Default Bootstrap Credentials
- **Email:** `admin@foundryos.local`
- **Password:** `AdminBootstrap2026!Secure`
- *(The system enforces an immediate password rotation upon initial sign-in).*

---

## Development Setup

For independent frontend or backend development without running all containers:

### Prerequisites
- **Node.js**: v20+
- **JDK**: Java 21
- **PostgreSQL**: 16+ running locally on port 5432 (or run `docker compose up -d postgres`)

### Backend Setup
```bash
cd backend

# Run automated tests
./mvnw clean test

# Launch backend application
./mvnw spring-boot:run
```

### Frontend Setup
```bash
cd frontend

# Install dependencies
npm install

# Typecheck and lint
npm run typecheck
npm run lint

# Start Vite development server
npm run dev
```
The frontend dev server will be available at `http://localhost:5173`.

---

## Core Architecture

```
FoundryOS/
├── backend/                      # Spring Boot 3.3.2 + Java 21 Modular Monolith
│   ├── src/main/java/com/factoryos/
│   │   ├── modules/auth/         # JWT RS256 + Refresh Tokens, IAM & RBAC
│   │   ├── modules/machine/      # Fleet Registry & Status State Machine
│   │   ├── modules/downtime/     # Stoppages, Reason Codes, Outage Duration
│   │   ├── modules/production/   # Batch Orders, Good vs Scrap Counters
│   │   ├── modules/maintenance/  # Work Orders, Priority, Technician Dispatch
│   │   ├── modules/operations/   # Cross-domain Transaction Orchestrator
│   │   ├── modules/reporting/    # Plant KPI Summaries & Real-time OEE
│   │   ├── modules/telemetry/    # IIoT Sensor Metrics & Ingestion
│   │   ├── modules/tenant/       # Multi-Plant & Line Hierarchy
│   │   └── modules/audit/        # Transactional Append-Only Audit Bus
│   └── src/test/                 # Automated Unit & Integration Test Suite
├── frontend/                     # React 18 + TypeScript + Vite + Tailwind CSS
│   ├── src/components/views/     # Dashboard, Machines, Downtime, Production, MRO, IAM, Telemetry
│   ├── src/services/             # Axios API client with auto-refresh interceptors
│   └── src/styles/               # Industrial Brutalist Design System
├── docker-compose.yml            # Multi-container local & production orchestration
└── docs/                         # Architecture specifications, PRD, and runbooks
```

---

## Documentation & Runbooks

Comprehensive technical documentation is maintained in the [`docs/`](docs/) directory:

| Document | Description |
|---|---|
| [RELEASE_NOTES_v1.0.0](docs/RELEASE_NOTES_v1.0.0.md) | FoundryOS v1.0.0 GA Release Notes, SLAs, and verification evidence |
| [OPERATIONS_RUNBOOK](docs/OPERATIONS_RUNBOOK.md) | Backup/Restore (RPO &le; 15m, RTO &le; 30m), Disaster Recovery, 6-Role Smoke Test Matrix |
| [PRD](docs/PRD.md) | Product Requirements Document, User Personas, Permissions, and Scope |
| [Architecture Essentials](docs/ARCHITECTURE-ESSENTIALS.md) | High-level system architecture, module contracts, and invariants |
| [Architecture Deep-Dive](docs/ARCHITECTURE.md) | Domain boundaries, transactional invariants, optimistic locking |
| [CODEX](docs/CODEX.md) | Engineering standards, code conventions, CI/CD pipeline |
| [Security](docs/Security.md) | Cryptography standards, authentication guards, audit retention |
| [BACKLOG](docs/BACKLOG.md) | MVP Epics, user stories, acceptance criteria (v1.0.0 GA Completed) |
| [BACKLOG_V2](docs/BACKLOG_V2.md) | FoundryOS 2.0 Roadmap: Multi-Plant, IIoT Telemetry, Predictive Maintenance |

---

## Roadmap

- [x] **v1.0.0 (GA)**: Single-plant core MES, 6-Role RBAC, Machine State Machine, Downtime Tracking, OEE Engine, Production Batching, Work Orders, Audit Bus.
- [x] **v1.1.0**: Barcode & QR asset scanner, live sensor telemetry simulation, dark industrial brutalist UI upgrade.
- [ ] **v2.0.0**: Multi-plant enterprise hierarchy, MQTT/OPC-UA IIoT broker connectivity, AI-driven predictive maintenance anomalies.

---

## Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) and check out open issues before opening a pull request.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feat/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: add some amazing feature'`)
4. Push to the Branch (`git push origin feat/AmazingFeature`)
5. Open a Pull Request

---

## License

Distributed under the MIT License. See [`LICENSE`](LICENSE) for more information.

---

<div align="center">
  <sub>Built for precision manufacturing operations. Maintained by <a href="https://github.com/luqshzeeq3601-art">luqshzeeq3601-art</a> and the FoundryOS Community.</sub>
</div>
