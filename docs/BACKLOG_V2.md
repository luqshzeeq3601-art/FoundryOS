# FactoryOS Version 2.0 Product Backlog & Implementation Roadmap

**Document Version:** 2.0.0-DRAFT  
**Baseline Release:** FactoryOS v1.0.0 GA  
**Platform Evolution:** Enterprise Multi-Plant Federation, Industrial IoT (IIoT), Edge Resilience, and Predictive Maintenance

---

## 1. Vision & Architectural Evolution

FactoryOS v1.0.0 delivered a hardened single-plant modular monolith with manual/tablet-reported downtime, production order execution, maintenance ticketing, and a real-time OEE dashboard. 

**FactoryOS Version 2.0** scales the platform to multi-plant enterprise manufacturing:
1. **Automated Machine Telemetry (IIoT):** Transition from manual operator event logging to automated PLC telemetry ingestion (OPC-UA, MQTT, Modbus TCP).
2. **Multi-Plant Federation:** Support multi-tenant enterprise hierarchies (`Enterprise -> Site / Plant -> Area -> Line -> Work Cell -> Machine`) with global executive visibility and plant-level data sovereignty.
3. **Edge Resilience & Offline Continuity:** Local edge gateway nodes executing shop-floor transactions during WAN disconnections with store-and-forward synchronization.
4. **Predictive Maintenance & Health Scoring:** Machine sensor anomaly detection (spindle vibration, temperature, current draw) generating automated preventive work orders prior to catastrophic failure.
5. **ERP & Supply Chain Synchronization:** Bidirectional integration with SAP S/4HANA, NetSuite, and Microsoft Dynamics 365 for automated bill-of-materials (BOM) explosion, material backflushing, and finished goods inventory tagging.

---

## 2. Architecture Guidelines for V2

- **Edge-to-Cloud Hybrid Topology:** Plant-local Edge Gateways (running lightweight Go/Java micro-agents) communicate with PLC networks and forward aggregated telemetry to the FactoryOS core via mTLS MQTT/gRPC.
- **Time-Series Telemetry Engine:** Dedicated time-series partition store (TimescaleDB extension on PostgreSQL or hybrid ClickHouse/VictoriaMetrics) for sub-second telemetry streams.
- **Backward Compatibility:** All existing v1.0.0 REST endpoints, RBAC policies, and database entities remain authoritative for core transactional state.

---

## 3. Epics and Story Breakdown

### Epic 4 — Industrial IoT (IIoT) & Automated Machine Telemetry

#### E4-S1 — Industrial Protocol Gateway (OPC-UA & MQTT)
- **Story:** As a Controls/Automation Engineer, I want FactoryOS to connect directly to industrial controllers (Siemens S7, Allen-Bradley ControlLogix, Beckhoff ADS, Modbus TCP) via OPC-UA and MQTT so that machine state is captured automatically without operator manual intervention.
- **Acceptance Criteria:**
  - Pluggable connector architecture supporting OPC-UA binary protocol and MQTT Sparkplug B.
  - Configurable polling intervals (100ms to 5000ms) per tag group.
  - Auto-reconnect with exponential backoff on network disruption.
  - Ingestion throughput $\ge 10,000$ tag updates/sec per edge instance with $< 5\%$ CPU utilization.
- **Tasks:**
  - [x] Implement edge gateway ingestion service with protocol support (`OPC_UA`, `MQTT_SPARKPLUG_B`, `MODBUS_TCP`).
  - [x] Create tag mapping configuration schema in backend (`MachineTagMapping` & `machine_tag_mappings` table).
  - [x] Implement authenticated high-throughput batch ingestion endpoint (`/api/v2/telemetry/ingest`).
  - [x] Build Industrial Brutalist Live Telemetry UI with asset gauges, live signal beacon, and tag registry.
  - [x] Add unit tests, RBAC controller tests, and ISO 10816 anomaly threshold verification (`TelemetryServiceTest`, `TelemetryControllerTest`).

#### E4-S2 — High-Frequency Time-Series Storage & Downsampling
- **Story:** As a Quality Engineer, I want continuous sensor telemetry (spindle vibration, motor current, bearing temperatures) stored and automatically downsampled so that historical trend analysis is fast and storage growth remains bounded.
- **Acceptance Criteria:**
  - Continuous ingestion of time-series points with automatic chunk partitioning by day/week.
  - Automated continuous aggregate downsampling (1s raw -> 1min avg/min/max after 7 days -> 1hour avg after 30 days).
  - Retention policies enforce storage ceilings without blocking writes.
  - Sub-200ms query performance over 30-day telemetry intervals.
- **Tasks:**
  - [x] Enable TimescaleDB hypertables or storage adapter schema for `machine_telemetry_points`, `machine_telemetry_rollups_1m`, `machine_telemetry_rollups_1h` (Flyway `V3__telemetry_downsampling_and_retention.sql`).
  - [x] Define continuous rollup aggregation engine in `TelemetryDownsamplingService` computing avg/min/max/sample counts.
  - [x] Implement query API with date-range bucket downsampling (`GET /api/v2/telemetry/machines/{id}/series`) and auto-resolution (`1s` for $\le 2$h, `1m` for $\le 7$d, `1h` for $> 7$d).
  - [x] Implement retention policy pruning API (`POST /api/v2/telemetry/retention/execute`) with default 7d raw / 30d 1m / 365d 1h policies.
  - [x] Validate query response times $< 200$ms over 30-day telemetry intervals (`TelemetryDownsamplingPerformanceTest`).
  - [x] Build frontend time-series downsampling analytics chart and retention modal adhering to `industrial-brutalist-ui`.

#### E4-S3 — Automated Micro-Stop & Downtime Detection
- **Story:** As a Production Supervisor, I want FactoryOS to automatically trigger downtime events when a machine stops cycling during an active production order, distinguishing micro-stops ($< 3$ minutes) from major breakdowns.
- **Acceptance Criteria:**
  - Transition from `ACTIVE` to `DOWN` automatically within 5 seconds of zero part sensor pulses.
  - Micro-stops ($< 180$ seconds) automatically logged and categorized as minor stoppages.
  - If stoppage exceeds 3 minutes, system alerts the line operator via touch UI to tag the downtime root cause.
  - Race-free downtime state coordination enforced via database partial unique index.
- **Tasks:**
  - [x] Create Flyway migration `V4__automated_micro_stop_and_downtime_detection.sql` with `trigger_source`, `is_micro_stop`, `root_cause_prompted_at`, and `root_cause_acknowledged_at`.
  - [x] Build stream evaluation and state machine in `AutomatedDowntimeDetectionService` monitoring sensor pulses and active `ProductionOrder` states.
  - [x] Implement micro-stop auto-resolution ($< 180$s) and operator attention prompt threshold ($\ge 180$s).
  - [x] Implement REST endpoints (`/api/v2/downtime/pending-root-causes`, `/acknowledge-root-cause`, `/micro-stops`, `/evaluate/{id}`).
  - [x] Add automated unit & controller tests (`AutomatedDowntimeDetectionServiceTest`, `AutomatedDowntimeControllerTest`).
  - [x] Build Industrial Brutalist touch UI with pending root-cause attention banner, 1-touch classification modal, trigger source badges, and micro-stop loss analytics.

---

### Epic 5 — Multi-Plant Federation & Enterprise Hierarchy

#### E5-S1 — Multi-Tenant & Multi-Site Organizational Hierarchy
- **Story:** As an Enterprise VP of Operations, I want to manage multiple manufacturing facilities within a unified organization tree so that plant managers see only their site while corporate leadership sees global metrics.
- **Acceptance Criteria:**
  - Hierarchy model: `Enterprise -> Plant / Site -> Production Area -> Line -> Work Cell -> Machine`.
  - Strict data partitioning: plant-scoped users cannot read or modify data belonging to other plants.
  - Multi-tenant role assignments (e.g., user is `PRODUCTION_MANAGER` at Plant A, but `VIEWER` at Plant B).
  - PostgreSQL Row-Level Security (RLS) and TenantContextFilter thread-local resolution with `X-Plant-ID` header.
- **Tasks:**
  - [x] Introduce `Enterprise`, `Plant`, `ProductionArea`, `ProductionLine`, `WorkCell`, and `UserPlantMembership` domain entities and repositories (`V6__enterprise_multi_tenant_hierarchy.sql`).
  - [x] Implement tenant context resolver filter (`TenantContextFilter`, `TenantContextHolder`, MDC propagation) and PostgreSQL Row-Level Security (RLS) policies.
  - [x] Update JWT claims to include authorized plant list, default plant, and plant-switching endpoint (`/api/v1/auth/switch-plant`).
  - [x] Build multi-tenant equipment tree REST endpoints (`/api/v2/hierarchy/**`).
  - [x] Build Industrial Brutalist plant switcher (`PlantSwitcher.tsx`), hierarchy tree explorer (`HierarchyManagementView.tsx`), and auth context state integration.
  - [x] Add comprehensive cross-tenant security integration tests proving zero cross-plant data leakage (`CrossTenantSecurityIntegrationTest`, `TenantContextFilterTest`, `HierarchyControllerTest`).

#### E5-S2 — Enterprise Fleet Analytics & Cross-Plant Benchmarking
- **Story:** As an Operations Director, I want a multi-plant executive comparison dashboard to benchmark OEE, availability, and scrap rates across plants and production lines.
- **Acceptance Criteria:**
  - Side-by-side comparison of plant OEE with drill-down to line-level bottlenecks.
  - Standardized OEE calculations across different shift models and regional timezones.
  - Exportable executive summaries in PDF and CSV formats.
- **Tasks:**
  - [x] Implement enterprise aggregation endpoints (`/api/v2/analytics/enterprise/oee-matrix`, `/export/csv`, `/export/pdf`, `/reports/scheduled`).
  - [x] Create multi-plant comparison view in frontend using high-contrast brutalist data grids and line-level drilldowns (`EnterpriseFleetAnalyticsView.tsx`).
  - [x] Implement scheduled executive summary report generation with audit logging (`ScheduledEnterpriseReportService.java`).
  - [x] Add comprehensive unit, controller RBAC, and multi-plant integration tests with 101/101 test suite pass (`EnterpriseAnalyticsServiceTest`, `EnterpriseExportServiceTest`, `EnterpriseAnalyticsControllerTest`, `EnterpriseAnalyticsIntegrationTest`).

#### E5-S3 — Store-and-Forward Edge Resilience
- **Story:** As a Plant IT Specialist, I want the local factory floor to continue production order execution and barcode scanning even if the WAN connection to the cloud backend goes down.
- **Acceptance Criteria:**
  - Edge gateway caches active production orders, operator credentials, and machine configurations.
  - Offline transactions stored in local SQLite / RocksDB embedded storage.
  - Automatic reconciliation and conflict-free replay upon WAN reconnection.
- **Tasks:**
  - [x] Implement local SQLite offline buffer on Edge Gateway (`edge_gateways`, `edge_offline_sync_batches`, `edge_offline_transaction_logs`, `V7__edge_store_and_forward_sync.sql`, `EdgeGatewayService.java`).
  - [x] Design idempotent batch sync protocol with vector clocks / transactional monotonic sequence IDs (`/api/v2/edge/sync/batch`, `EdgeStoreAndForwardSyncService.java`).
  - [x] Build Industrial Brutalist Edge Resilience Monitor & 4-Hour WAN Disconnect Rehearsal Simulator (`EdgeResilienceView.tsx`).
  - [x] Rehearse 4-hour simulated WAN disconnect and verify zero loss of completed part counts (`EdgeStoreAndForwardRehearsalTest.java`, `EdgeStoreAndForwardSyncServiceTest.java`, `EdgeControllerTest.java`).

---

### Epic 6 — Predictive Maintenance & Machine Health Analytics

#### E6-S1 — Spindle Vibration Spectral Analysis (FFT) & Anomaly Detection
- **Story:** As a Reliability Engineer, I want high-frequency vibration data analyzed for spectral frequency shifts (bearing inner/outer race pass frequencies) so that developing faults are caught before catastrophic failure.
- **Acceptance Criteria:**
  - Ingestion of tri-axial vibration accelerations at $\ge 1$ kHz burst sampling.
  - Fast Fourier Transform (FFT) computation on edge or stream engine to produce frequency spectra.
  - Statistical deviation alerts triggered when peak frequency amplitudes exceed ISO 10816 standards.
- **Tasks:**
  - [x] Ingestion of tri-axial vibration accelerations at $\ge 1$ kHz burst sampling (`V9__spindle_vibration_fft_and_health_scoring.sql`, `VibrationBurstSample`, `VibrationBurstIngestDto`).
  - [x] Fast Fourier Transform (FFT) pipeline with Radix-2 Cooley-Tukey, Hann windowing, velocity integration ($\text{mm/s}$), RMS velocity, crest factor, kurtosis, and kinematic peak identification (`FftSpectralAnalysisService.java`, `Iso10816StandardsEngine.java`).
  - [x] Implement composite machine health scoring model ($0 - 100\%$) and automated alert audit logging (`MachineHealthScoringService.java`, `/api/v2/vibration/**`).
  - [x] Build Industrial Brutalist Fast Fourier Transform (FFT) spectrum viewer, ISO 10816 severity meter, fault diagnosis matrix, and synthetic burst simulator (`VibrationHealthView.tsx`).
  - [x] Comprehensive test coverage across FFT algorithm, ISO standard limits, scoring penalties, controller RBAC, and synthetic simulation rehearsal (`FftSpectralAnalysisServiceTest`, `Iso10816StandardsEngineTest`, `MachineHealthScoringServiceTest`, `VibrationAnalysisControllerTest`).

#### E6-S2 — Prescriptive Automated Maintenance Work Orders
- **Story:** As a Maintenance Manager, I want the system to automatically generate priority maintenance tickets with attached sensor telemetry diagnostics when an anomaly condition is verified.
- **Acceptance Criteria:**
  - Health score degradation below $60\%$ auto-creates a `HIGH` priority maintenance order.
  - Prescriptive order includes diagnostic snapshot: anomalous sensor values, suspected subsystem, and recommended spare parts.
  - Duplicate anomaly triggers within 24 hours are deduplicated under the same active ticket.
- **Tasks:**
  - [x] Add automated trigger hook to `MachineHealthScoringService` and create `PrescriptiveMaintenanceService.java` (`V10__prescriptive_maintenance_diagnostic_snapshots.sql`, `MaintenanceWorkOrder.java`, `WorkOrderDto.java`).
  - [x] Store diagnostic snapshot attachment (`DiagnosticSnapshotDto`, RMS velocity, bearing temp, kurtosis, crest factor, harmonic peaks, suspected subsystem, and recommended spare parts).
  - [x] Implement 24-hour deduplication logic preventing duplicate work orders for active tickets and dispatch alert audit events to technicians.
  - [x] Build Industrial Brutalist Prescriptive Work Order filter, badges, and diagnostic snapshot modal in `MaintenanceView.tsx` and active ticket linkage in `VibrationHealthView.tsx`.
  - [x] Comprehensive test suite with 100% pass across prescriptive order creation, critical priority escalation, and 24-hour deduplication (`PrescriptiveMaintenanceServiceTest.java`).

---

### Epic 7 — ERP & Supply Chain Bidirectional Integration

#### E7-S1 — ERP Production Order Synchronization (SAP S/4HANA / NetSuite)
- **Story:** As a Production Planner, I want planned production orders in our ERP system automatically imported into FactoryOS and executed order results pushed back so that inventory and finance remain synchronized.
- **Acceptance Criteria:**
  - Outbound/Inbound REST and webhook adapters for SAP OData / NetSuite REST Web Services.
  - Automatic creation of FactoryOS production orders upon ERP order release.
  - Real-time or batch confirmation of finished goods quantities, scrap, and labor hours back to ERP.
- **Tasks:**
  - [x] Create `com.factoryos.modules.erp` module with generic integration interfaces and database schema (`V11__erp_production_order_synchronization.sql`, `ErpConnector.java`, `ErpSyncLog.java`, `ErpOrderConfirmation.java`).
  - [x] Implement SAP S/4HANA OData v4 client adapter (`SapS4HanaODataAdapter.java`) and Oracle NetSuite RESTlet adapter (`OracleNetSuiteRestAdapter.java`).
  - [x] Map ERP production orders and Bill of Materials (BOM) components to FactoryOS production orders upon release polling (`ErpSyncService.java`).
  - [x] Outbound order confirmation dispatcher with yield, scrap, scrap reason codes, machine/labor hours, and retry scheduler.
  - [x] Build Industrial Brutalist ERP // SCM Synchronization Hub frontend (`ErpIntegrationHubView.tsx`) with connector health grid, inbound simulator, confirmation feed, and payload inspect modal.
  - [x] Add comprehensive unit and controller tests with 100% pass across full backend (155/155 tests) and frontend Vite build (`ErpSyncServiceTest.java`, `ErpIntegrationControllerTest.java`).

#### E7-S2 — Material Backflushing & Scrap Reconciliation
- **Story:** As an Inventory Controller, I want raw material lots consumed and backflushed automatically as parts are produced so that shop-floor inventory levels remain accurate.
- **Acceptance Criteria:**
  - Each reported good piece decrements raw material inventory based on engineering BOM ratios.
  - Scrap reasons require quantity input and post to ERP scrap cost centers.
  - Discrepancy warnings raised when actual consumption deviates $> 5\%$ from theoretical BOM.
- **Tasks:**
  - [x] Implement `Material`, `MaterialConsumptionRecord`, and `MaterialRepository` (`V12__material_backflushing_and_scrap_reconciliation.sql`).
  - [x] Integrate automated consumption transactions and inventory decrements into `MaterialBackflushingService.java` (`/api/v2/materials/orders/{orderId}/record-output`).
  - [x] Build multi-level BOM explosion calculation engine with stock sufficiency and cost breakdown (`/api/v2/materials/bom/{productCode}/explosion`).
  - [x] Build operator scrap classification dialog with touch-friendly reason codes and cost-center routing in `ErpIntegrationHubView.tsx`.
  - [x] Implement >5% theoretical vs actual consumption variance detection and real-time alert banners.
  - [x] Comprehensive test suite with 100% pass across material backflushing, stock decrements, and variance alerts (`MaterialBackflushingServiceTest.java`, `MaterialControllerTest.java`).

---

### Epic 8 — Mobile Edge, Barcode & Digital Work Instructions

#### E8-S1 — Industrial Handheld & Camera 2D Barcode Scanning
- **Story:** As a Shop-Floor Operator, I want to scan 2D DataMatrix / QR barcodes on raw material bins, traveler cards, and finished parts using Zebra Android handhelds or tablet cameras to eliminate manual keyboard input.
- **Acceptance Criteria:**
  - Native support for hardware barcode wedge scanners (Zebra DataWedge, Honeywell, Keyence) via keyboard emulation with rapid burst detection.
  - Camera-based fallback scanning using ZXing-WASM with video viewfinder, aiming laser reticle, and torch control.
  - Automatic cross-validation of material lots against active production order Bill of Materials (BOM) recipes with quarantine and expiration gating.
  - Scan latency to order verification $< 300$ms (benchmarked at $< 10$ms).
  - Web Audio API synthesizer tones and haptic vibration feedback for successful/failed/warning scans.
  - Full audit logging in `barcode_scan_logs` with entity resolution and latency metrics.
- **Tasks:**
  - [x] Create Flyway migration `V5__barcode_scanning_and_traceability.sql` for `material_lots`, `bill_of_materials`, and `barcode_scan_logs`.
  - [x] Implement backend domain entities, repositories, and DTOs for material traceability and BOM matching.
  - [x] Build `BarcodeScanningService` with traveler resolution, BOM validation, expiry gating, and operator badge lookup.
  - [x] Implement REST endpoints (`POST /api/v2/barcode/scan`, `GET /api/v2/barcode/logs`, `GET /api/v2/barcode/bom/{productCode}`).
  - [x] Build hardware scanner hook (`useHardwareBarcodeScanner`) with $\le 60$ms inter-keystroke burst detection.
  - [x] Build tablet-ready Industrial Brutalist modal (`BarcodeScannerModal`) with live ZXing camera feed, torch control, 1-touch simulators, and BOM match cards.
  - [x] Add synthesized Web Audio tones and haptic vibration feedback in `audioFeedback.ts`.
  - [x] Add unit, controller, and performance tests (`BarcodeScanningServiceTest`, `BarcodeControllerTest`, `BarcodeScanningPerformanceTest`) with 77/77 tests passing.

#### E8-S2 — Digital Standard Operating Procedures (SOP) & Checklists
- **Story:** As an Assembler, I want interactive digital work instructions on my workstation tablet showing current step blueprints, quality inspection checklists, and mandatory sign-offs before completing a production run.
- **Acceptance Criteria:**
  - Rich step-by-step SOP viewer supporting CAD drawings, PDFs, and high-resolution images.
  - Quality sign-off gate requiring operator confirmation before advancing order state.
  - Photo upload capability for defect documentation using tablet camera.
- **Tasks:**
  - [x] Design `StandardOperatingProcedureEntity`, `SopStepEntity`, `SopExecutionSessionEntity`, `SopStepExecutionRecordEntity`, and `QualitySignOffGateEntity` (`V8__digital_sop_and_quality_gates.sql`).
  - [x] Build interactive Industrial Brutalist SOP viewer component with CAD blueprint viewport, step carousel, tolerance evaluation, photo capture, and quality approval stamp (`DigitalSopView.tsx`, `sopApi`).
  - [x] Integrate mandatory inspection step validation and Quality Gate invariant with production order completion state machine (`ProductionOrderService.java`, `QualityGateService.java`).
  - [x] Add comprehensive test suites for session lifecycle, tolerance checking, quality sign-offs, and RBAC (`QualityGateServiceTest.java`, `SopServiceTest.java`, `SopControllerTest.java`).

---

## 4. Release Sequencing & Sprint Roadmap

| Sprint | Epic / Scope | Primary Milestones |
|---|---|---|
| **Sprint 6** | E4-S1, E4-S2 | Industrial Protocol Gateway (OPC-UA/MQTT), TimescaleDB Ingestion Pipeline |
| **Sprint 7** | E4-S3, E8-S1 | Automated Micro-Stop Detection, Industrial Handheld & Barcode Integration |
| **Sprint 8** | E5-S1, E5-S2 | Multi-Tenant Hierarchy, Plant Scoping, Enterprise Executive OEE Matrix |
| **Sprint 9** | E5-S3, E8-S2 | Store-and-Forward Offline Resilience, Digital SOPs & Quality Checklists |
| **Sprint 10**| E6-S1, E6-S2 | Vibration FFT Spectral Analysis, Prescriptive Auto Maintenance Orders |
| **Sprint 11**| E7-S1, E7-S2 | SAP S/4HANA & ERP Integration, Material Backflushing & Scrap Tracking |
| **Sprint 12**| Hardening & v2.0 GA | Enterprise Load Testing (100k msgs/sec), Cross-Plant Security Audit, GA Release |

---

## 5. Definition of Done for Version 2 Deliverables

- [x] All automated tests pass across unit, integration, and edge-simulator tiers (162/162 JUnit 5 tests passing).
- [x] End-to-end edge-to-cloud latency meets defined SLAs ($< 100$ms event dispatch, $< 5$s automated downtime).
- [x] Multi-tenant isolation verified with zero cross-plant data leakage (`CrossTenantSecurityIntegrationTest`).
- [x] Offline store-and-forward tested under 4+ hour network severance with zero transaction loss.
- [x] Ergonomic and touch-friendly UX adhering to Industrial Brutalist and touch accessibility standards.
- [x] Graphify architecture knowledge graph updated with new modules and models.
- [x] Migration and rollback procedures verified with zero production data loss.

