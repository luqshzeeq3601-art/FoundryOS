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
- **Tasks:**
  - [ ] Enable TimescaleDB hypertables or ClickHouse storage adapter for `machine_telemetry_raw`.
  - [ ] Define continuous rollup views for operational KPIs.
  - [ ] Implement query API with date-range bucket downsampling (`/api/v2/telemetry/machines/{id}/series`).
  - [ ] Validate query response times $< 200$ms over 30-day telemetry intervals.

#### E4-S3 — Automated Micro-Stop & Downtime Detection
- **Story:** As a Production Supervisor, I want FactoryOS to automatically trigger downtime events when a machine stops cycling during an active production order, distinguishing micro-stops ($< 3$ minutes) from major breakdowns.
- **Acceptance Criteria:**
  - Transition from `ACTIVE` to `DOWN` automatically within 5 seconds of zero part sensor pulses.
  - Micro-stops ($< 180$ seconds) automatically logged and categorized as minor stoppages.
  - If stoppage exceeds 3 minutes, system alerts the line operator via touch UI to tag the downtime root cause.
- **Tasks:**
  - [ ] Build stream processing rule engine for machine heartbeats and cycle counter delta.
  - [ ] Update `DowntimeTransitionCoordinator` to accept both manual and automated trigger sources.
  - [ ] Add operator tablet prompt when an automated downtime exceeds micro-stop threshold.
  - [ ] Add integration test verifying automated downtime creation and race-free resolution.

---

### Epic 5 — Multi-Plant Federation & Enterprise Hierarchy

#### E5-S1 — Multi-Tenant & Multi-Site Organizational Hierarchy
- **Story:** As an Enterprise VP of Operations, I want to manage multiple manufacturing facilities within a unified organization tree so that plant managers see only their site while corporate leadership sees global metrics.
- **Acceptance Criteria:**
  - Hierarchy model: `Enterprise -> Plant / Site -> Production Area -> Line -> Work Cell -> Machine`.
  - Strict data partitioning: plant-scoped users cannot read or modify data belonging to other plants.
  - Multi-tenant role assignments (e.g., user is `PRODUCTION_MANAGER` at Plant A, but `VIEWER` at Plant B).
- **Tasks:**
  - [ ] Introduce `PlantEntity` and `EnterpriseOrganizationEntity` with hierarchical foreign keys.
  - [ ] Implement tenant context resolver filter (`TenantContextFilter`) and Row-Level Security (RLS) policies.
  - [ ] Update JWT claims to include authorized plant list and default active plant.
  - [ ] Add cross-tenant isolation security tests.

#### E5-S2 — Enterprise Fleet Analytics & Cross-Plant Benchmarking
- **Story:** As an Operations Director, I want a multi-plant executive comparison dashboard to benchmark OEE, availability, and scrap rates across plants and production lines.
- **Acceptance Criteria:**
  - Side-by-side comparison of plant OEE with drill-down to line-level bottlenecks.
  - Standardized OEE calculations across different shift models and regional timezones.
  - Exportable executive summaries in PDF and CSV formats.
- **Tasks:**
  - [ ] Implement enterprise aggregation endpoints (`/api/v2/analytics/enterprise/oee-matrix`).
  - [ ] Create multi-plant comparison view in frontend using high-contrast brutalist data grids.
  - [ ] Implement scheduled executive summary report generation via Spring Batch.

#### E5-S3 — Store-and-Forward Edge Resilience
- **Story:** As a Plant IT Specialist, I want the local factory floor to continue production order execution and barcode scanning even if the WAN connection to the cloud backend goes down.
- **Acceptance Criteria:**
  - Edge gateway caches active production orders, operator credentials, and machine configurations.
  - Offline transactions stored in local SQLite / RocksDB embedded storage.
  - Automatic reconciliation and conflict-free replay upon WAN reconnection.
- **Tasks:**
  - [ ] Implement local SQLite offline buffer on Edge Gateway.
  - [ ] Design idempotent batch sync protocol with vector clocks / transactional monotonic sequence IDs.
  - [ ] Rehearse 4-hour simulated WAN disconnect and verify zero loss of completed part counts.

---

### Epic 6 — Predictive Maintenance & Machine Health Analytics

#### E6-S1 — Spindle Vibration Spectral Analysis (FFT) & Anomaly Detection
- **Story:** As a Reliability Engineer, I want high-frequency vibration data analyzed for spectral frequency shifts (bearing inner/outer race pass frequencies) so that developing faults are caught before catastrophic failure.
- **Acceptance Criteria:**
  - Ingestion of tri-axial vibration accelerations at $\ge 1$ kHz burst sampling.
  - Fast Fourier Transform (FFT) computation on edge or stream engine to produce frequency spectra.
  - Statistical deviation alerts triggered when peak frequency amplitudes exceed ISO 10816 standards.
- **Tasks:**
  - [ ] Develop FFT computation pipeline on Edge Gateway.
  - [ ] Implement machine health score calculation ($0 - 100\%$) based on composite vibration and temperature.
  - [ ] Build spectral waterfall and time-waveform visualization in frontend machine detail view.

#### E6-S2 — Prescriptive Automated Maintenance Work Orders
- **Story:** As a Maintenance Manager, I want the system to automatically generate priority maintenance tickets with attached sensor telemetry diagnostics when an anomaly condition is verified.
- **Acceptance Criteria:**
  - Health score degradation below $60\%$ auto-creates a `HIGH` priority maintenance order.
  - Prescriptive order includes diagnostic snapshot: anomalous sensor values, suspected subsystem, and recommended spare parts.
  - Duplicate anomaly triggers within 24 hours are deduplicated under the same active ticket.
- **Tasks:**
  - [ ] Add automated trigger hook to `MaintenanceService`.
  - [ ] Store diagnostic time-series snapshot attachment on `MaintenanceOrderEntity`.
  - [ ] Implement notification push (WebSockets + Email/SMS Webhook) to on-duty technicians.

---

### Epic 7 — ERP & Supply Chain Bidirectional Integration

#### E7-S1 — ERP Production Order Synchronization (SAP S/4HANA / NetSuite)
- **Story:** As a Production Planner, I want planned production orders in our ERP system automatically imported into FactoryOS and executed order results pushed back so that inventory and finance remain synchronized.
- **Acceptance Criteria:**
  - Outbound/Inbound REST and webhook adapters for SAP OData / NetSuite REST Web Services.
  - Automatic creation of FactoryOS production orders upon ERP order release.
  - Real-time or batch confirmation of finished goods quantities, scrap, and labor hours back to ERP.
- **Tasks:**
  - [ ] Create `factoryos-erp-integration` module with generic integration interfaces.
  - [ ] Implement SAP OData v4 client adapter.
  - [ ] Map ERP Bill of Materials (BOM) components to FactoryOS production orders.
  - [ ] Add integration tests with mock ERP endpoints verifying idempotency and retry policies.

#### E7-S2 — Material Backflushing & Scrap Reconciliation
- **Story:** As an Inventory Controller, I want raw material lots consumed and backflushed automatically as parts are produced so that shop-floor inventory levels remain accurate.
- **Acceptance Criteria:**
  - Each reported good piece decrements raw material inventory based on engineering BOM ratios.
  - Scrap reasons require quantity input and post to ERP scrap cost centers.
  - Discrepancy warnings raised when actual consumption deviates $> 5\%$ from theoretical BOM.
- **Tasks:**
  - [ ] Implement `MaterialEntity`, `BomComponentEntity`, and `MaterialConsumptionRecordEntity`.
  - [ ] Integrate consumption transactions into `ProductionOrderService.recordOutput(...)`.
  - [ ] Build operator scrap classification dialog with touch-friendly reason codes.

---

### Epic 8 — Mobile Edge, Barcode & Digital Work Instructions

#### E8-S1 — Industrial Handheld & Camera 2D Barcode Scanning
- **Story:** As a Shop-Floor Operator, I want to scan 2D DataMatrix / QR barcodes on raw material bins, traveler cards, and finished parts using Zebra Android handhelds or tablet cameras to eliminate manual keyboard input.
- **Acceptance Criteria:**
  - Native support for hardware barcode wedge scanners (Zebra DataWedge, Honeywell, Keyence) via keyboard emulation and Web Broadcast Intents.
  - Camera-based fallback scanning using WebAssembly / HTML5 barcode detection.
  - Scan latency to order verification $< 300$ms.
- **Tasks:**
  - [ ] Build hardware scanner hook and broadcast receiver in frontend.
  - [ ] Add camera scan modal with high-speed ZXing-WASM decoder.
  - [ ] Add audio/haptic feedback tones for successful and failed scans.

#### E8-S2 — Digital Standard Operating Procedures (SOP) & Checklists
- **Story:** As an Assembler, I want interactive digital work instructions on my workstation tablet showing current step blueprints, quality inspection checklists, and mandatory sign-offs before completing a production run.
- **Acceptance Criteria:**
  - Rich step-by-step SOP viewer supporting CAD drawings, PDFs, and high-resolution images.
  - Quality sign-off gate requiring operator confirmation before advancing order state.
  - Photo upload capability for defect documentation using tablet camera.
- **Tasks:**
  - [ ] Design `StandardOperatingProcedureEntity` and `SopStepEntity`.
  - [ ] Build interactive industrial SOP viewer component in frontend.
  - [ ] Integrate inspection step validation with production order completion gate.

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

- [ ] All automated tests pass across unit, integration, and edge-simulator tiers.
- [ ] End-to-end edge-to-cloud latency meets defined SLAs ($< 100$ms event dispatch, $< 5$s automated downtime).
- [ ] Multi-tenant isolation verified with zero cross-plant data leakage.
- [ ] Offline store-and-forward tested under 4+ hour network severance with zero transaction loss.
- [ ] Ergonomic and touch-friendly UX adhering to Industrial Brutalist and touch accessibility standards.
- [ ] Graphify architecture knowledge graph updated with new modules and models.
- [ ] Migration and rollback procedures verified with zero production data loss.
