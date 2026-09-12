# FoundryOS Release Notes — Version 2.0.0 (GA)

**Release Date:** September 12, 2026  
**Status:** General Availability (GA)  
**Git Tag:** `v2.0.0`  
**Target Environment:** Distributed Enterprise Manufacturing Execution System (MES) & Multi-Plant IIoT Platform

---

## 1. Executive Summary

FoundryOS Version 2.0.0 marks the milestone transition from a hardened single-plant MES monolith to a distributed, multi-plant enterprise manufacturing platform. Built upon Spring Boot 3 (Java 17/21) and an Industrial Brutalist React/TypeScript touch interface, v2.0 introduces automated IIoT PLC telemetry ingestion, PostgreSQL Row-Level Security multi-tenancy, WAN-disconnected edge resilience, spindle vibration spectral analysis (FFT), prescriptive maintenance work orders, bidirectional ERP synchronization (SAP S/4HANA & Oracle NetSuite), and automated BOM material backflushing.

All Sprint 6 through Sprint 12 epics and stories (Epic 4 through Epic 8) have been fully developed, integrated, verified, and stress-tested with 100% automated test pass rates.

---

## 2. Key Capabilities & Implemented Epics

### 2.1 Industrial IoT (IIoT) & Automated Telemetry (Epic 4)
- **Industrial Protocol Ingestion (E4-S1):** Pluggable protocol gateway supporting OPC-UA, MQTT Sparkplug B, and Modbus TCP with sub-second polling and batch telemetry endpoints (`/api/v2/telemetry/ingest`).
- **High-Frequency Downsampling & Retention (E4-S2):** Automated continuous aggregate downsampling (1s raw $\rightarrow$ 1m rollups $\rightarrow$ 1h rollups) and automated retention policies bounding storage growth (`/api/v2/telemetry/series`).
- **Automated Micro-Stop & Downtime Detection (E4-S3):** Stream state-machine evaluating sensor pulse streams, classifying stoppages $< 180$s as micro-stops, and prompting touch root-cause classification for extended halts.

### 2.2 Multi-Plant Federation & Enterprise Hierarchy (Epic 5)
- **Multi-Tenant Equipment Tree (E5-S1):** 6-tier hierarchy (`Enterprise -> Site / Plant -> Area -> Line -> Work Cell -> Machine`) with PostgreSQL Row-Level Security (RLS), `TenantContextFilter`, and plant switching (`/api/v2/hierarchy/**`).
- **Enterprise Fleet Analytics & Benchmarking (E5-S2):** Cross-plant benchmarking matrix aggregating OEE, availability, performance, and scrap rates across plants with PDF/CSV executive export (`/api/v2/analytics/enterprise/**`).
- **Store-and-Forward Edge Resilience (E5-S3):** Edge Gateway offline transaction buffer with vector clock batch synchronization, validated under 4+ hour simulated WAN network severance (`/api/v2/edge/**`).

### 2.3 Predictive Maintenance & Spindle Vibration Analytics (Epic 6)
- **Spindle Vibration Spectral Analysis FFT (E6-S1):** Radix-2 Cooley-Tukey FFT with Hann windowing, kinematic harmonic peak identification (1X unbalance, 2X misalignment, 3X looseness, BPFO/BPFI bearing faults), and ISO 10816 vibration severity zones (`/api/v2/vibration/**`).
- **Prescriptive Maintenance Work Orders (E6-S2):** Automated priority work order dispatch triggered on health scores $< 60\%$ with structured diagnostic snapshots, recommended spare parts, and 24-hour deduplication.

### 2.4 ERP & Supply Chain Bidirectional Integration (Epic 7)
- **ERP Order Synchronization (E7-S1):** Inbound adapters for SAP S/4HANA OData v4 and Oracle NetSuite SuiteTalk REST, automatic production order release, and outbound execution confirmations (`/api/v2/erp/**`).
- **BOM Material Backflushing & Scrap Tracking (E7-S2):** Automated raw material inventory decrements upon piece production, multi-level BOM explosion calculation with stock sufficiency, $> 5\%$ consumption variance anomaly detection, and cost-center scrap routing (`/api/v2/materials/**`).

### 2.5 Mobile Edge, Barcode & Digital Work Instructions (Epic 8)
- **Industrial Handheld & Camera Barcode Scanning (E8-S1):** Hardware wedge scanner listener ($\le 60$ms burst) and ZXing camera viewfinder for 2D DataMatrix/QR codes with real-time BOM traveler verification (`/api/v2/barcode/**`).
- **Digital SOPs & Quality Sign-Off Gates (E8-S2):** Interactive CAD/blueprint step-by-step assembly instructions, parametric tolerance checks, defect photo evidence capture, and mandatory quality sign-off gates blocking order completion.

---

## 3. Platform Capability & Routing Matrix

| Epic / Feature | Backend Service | API Route | Frontend View |
|---|---|---|---|
| **E4-S1: IIoT Ingestion** | `TelemetryIngestService` | `/api/v2/telemetry/**` | `LiveTelemetryView.tsx` |
| **E4-S2: Downsampling** | `TelemetryDownsamplingService` | `/api/v2/telemetry/series` | `LiveTelemetryView.tsx` |
| **E4-S3: Micro-Stops** | `AutomatedDowntimeDetectionService` | `/api/v2/downtime/**` | `DowntimeView.tsx` |
| **E5-S1: Multi-Tenant** | `TenantContextFilter`, `HierarchyService` | `/api/v2/hierarchy/**` | `HierarchyManagementView.tsx` |
| **E5-S2: Fleet OEE** | `EnterpriseAnalyticsService` | `/api/v2/analytics/enterprise/**` | `EnterpriseFleetAnalyticsView.tsx` |
| **E5-S3: Edge Sync** | `EdgeStoreAndForwardSyncService` | `/api/v2/edge/**` | `EdgeResilienceView.tsx` |
| **E6-S1: Spindle FFT** | `FftSpectralAnalysisService` | `/api/v2/vibration/**` | `VibrationHealthView.tsx` |
| **E6-S2: Prescriptive WO**| `PrescriptiveMaintenanceService` | `/api/v1/maintenance/work-orders` | `MaintenanceView.tsx` |
| **E7-S1: ERP Sync** | `ErpSyncService`, `SapS4HanaAdapter` | `/api/v2/erp/**` | `ErpIntegrationHubView.tsx` |
| **E7-S2: Backflushing** | `MaterialBackflushingService` | `/api/v2/materials/**` | `ErpIntegrationHubView.tsx` |
| **E8-S1: Barcode Scan** | `BarcodeScanningService` | `/api/v2/barcode/**` | `BarcodeScannerModal.tsx` |
| **E8-S2: Digital SOP** | `QualityGateService`, `SopService` | `/api/v2/sop/**` | `DigitalSopView.tsx` |

---

## 4. Quality Gates & Verification Summary

### 4.1 Backend JUnit 5 Automated Tests
- **Total Tests Executed:** 162
- **Pass Rate:** 100% (162 Passed, 0 Failures, 0 Errors, 0 Skipped)
- **Key Suites:**
  - `CrossTenantSecurityIntegrationTest`: 7/7 passed (zero cross-plant data leakage verified).
  - `MaterialBackflushingServiceTest`: 3/3 passed (inventory decrements, >5% variance alerts).
  - `MaterialControllerTest`: 4/4 passed (BOM explosion, output recording, variance endpoints).
  - `PrescriptiveMaintenanceServiceTest`: 4/4 passed (auto-tickets, 24h deduplication).
  - `FftSpectralAnalysisServiceTest`: Passed (bearing defect peak detection).
  - `EdgeStoreAndForwardRehearsalTest`: Passed (4h offline buffer replay).
  - `SixRoleRbacSmokeTest`: Passed (6-role RBAC enforcement).

### 4.2 Frontend Production Build
- **Compiler:** TypeScript 5.6 & Vite 5.4.21
- **Result:** Built in 3.92s with 0 TypeScript compiler errors across 1,917 modules.
- **Output:** Production assets compiled to `frontend/dist/`.

### 4.3 Architecture Knowledge Graph
- Re-indexed via `graphify` AST code-only extraction:
  - **Nodes:** 5,411
  - **Edges:** 13,616
  - **Communities:** 207

---

## 5. Database Migration Sequence

| Migration Script | Scope |
|---|---|
| `V1__init.sql` through `V4__add_indices.sql` | v1.0.0 Base Schema (users, machines, orders, downtime, maintenance) |
| `V3__telemetry_downsampling_and_retention.sql` | IIoT high-frequency points and rollups |
| `V4__automated_micro_stop_and_downtime_detection.sql` | Automated downtime evaluation & micro-stop flags |
| `V5__barcode_scanning_and_traceability.sql` | Barcode lots, BOM recipes, and scan logs |
| `V6__enterprise_multi_tenant_hierarchy.sql` | Enterprise, plants, areas, lines, cells, RLS |
| `V7__edge_store_and_forward_sync.sql` | Edge gateway offline buffers & sync batches |
| `V8__digital_sop_and_quality_gates.sql` | Digital SOP templates, steps, and quality sign-off gates |
| `V9__spindle_vibration_fft_and_health_scoring.sql` | Vibration bursts, spectral peaks, and health scores |
| `V10__prescriptive_maintenance_diagnostic_snapshots.sql` | Prescriptive work orders & diagnostic snapshots |
| `V11__erp_production_order_synchronization.sql` | ERP connectors, sync logs, and confirmations |
| `V12__material_backflushing_and_scrap_reconciliation.sql`| Raw materials, inventory decrements, and scrap records |

---

## 6. Deployment & Upgrade Instructions

1. **Docker Compose Staging Deployment:**
   ```bash
   docker compose -f docker-compose.staging.yml up --build -d
   ```
2. **Database Migration:**
   Flyway executes all migrations automatically on startup (`V1` through `V12`).
3. **Health Verification:**
   - Backend Actuator: `http://localhost:8080/actuator/health`
   - Frontend Web App: `http://localhost:3000`
