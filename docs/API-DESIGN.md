# FactoryOS API Design

## Scope and conventions

FactoryOS is an online-only, single-plant deployment. The API base path is
`/api/v1`. The frontend and API share an origin in production.

- UUID v4 identifiers are serialized as strings.
- Timestamps are UTC ISO 8601 values, for example `2026-09-09T08:00:00Z`.
- Enum values are uppercase strings.
- Protected endpoints require an active, non-deleted user.
- Mutation payloads contain only explicitly supported fields. Unknown fields are rejected.
- Responses contain DTOs, never persistence entities.
- A request/trace ID is returned in the `X-Request-ID` response header, including error responses; the error body retains the wrapper below.

## Response wrappers

Successful single-resource and command responses use:

```json
{
  "data": {},
  "message": "Operation completed",
  "timestamp": "2026-09-09T08:00:00Z"
}
```

List responses use a paged `data` value:

```json
{
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  },
  "message": "Records retrieved",
  "timestamp": "2026-09-09T08:00:00Z"
}
```

Defaults are `page=0` and `size=20`; `size` is capped at 100. Sort fields are
allowlisted, default to `createdAt,desc`, and use ID as a stable tie-breaker.

Errors use:

```json
{
  "status": 409,
  "error": "VERSION_CONFLICT",
  "message": "The record changed. Reload and retry.",
  "timestamp": "2026-09-09T08:00:00Z",
  "path": "/api/v1/machines/00000000-0000-0000-0000-000000000000"
}
```

Status meanings:

- `400` malformed input or validation failure
- `401` missing or invalid authentication
- `403` authenticated but forbidden
- `404` resource does not exist or is unavailable
- `409` stale version, uniqueness conflict, or invalid state transition
- `429` rate limit exceeded, with `Retry-After`

## Roles and authorization

Role abbreviations in the endpoint catalog are A (Admin), P (Production
Manager), E (Engineer), T (Technician), O (Operator), and V (Viewer). `All`
means all authenticated active users. Backend service methods enforce these
permissions; hiding a UI control is not an authorization control.

## Authentication and users

| Method and path | Request | Success | Roles |
|---|---|---:|---|
| `GET /auth/csrf` | None | 200 CSRF token | Public |
| `POST /auth/login` | `email`, `password` | 200 access token, expiry, user; refresh cookie | Public |
| `POST /auth/refresh` | Refresh cookie and CSRF header | 200 rotated credentials | Session holder |
| `POST /auth/logout` | Refresh cookie and CSRF header | 200, `data: null`; clear cookie | Session holder |
| `GET /auth/me` | None | 200 user | All |
| `POST /auth/change-password` | `currentPassword`, `newPassword` | 200; revoke sessions | All |
| `GET /users` | Page, search, role, active filters | 200 page | A |
| `POST /users` | `email`, `displayName`, `role`, `temporaryPassword` | 201 user | A |
| `PATCH /users/{id}` | `displayName?`, `role?`, `isActive?`, `expectedVersion` | 200 user | A |
| `POST /users/{id}/reset-password` | `temporaryPassword`, `expectedVersion` | 200; revoke sessions | A |
| `DELETE /users/{id}` | `expectedVersion` query parameter | 200 archived user | A |

Temporary-password accounts must change the password before accessing any
operational endpoint. MVP excludes self-registration and email password
recovery. User deletion is an archive operation; historical references remain.
An Admin cannot deactivate, demote, or archive the final active Admin.

Deactivation, archive, or changing a Technician to another role returns `409` while nonterminal assigned maintenance exists; reassign or cancel it first. Assignment and account changes serialize on the affected user rows and recheck eligibility.

## Machines

Machine statuses are `IDLE`, `RUNNING`, and `DOWN`.

| Method and path | Request | Success | Roles |
|---|---|---:|---|
| `GET /machines` | Page, search, status, location | 200 page | All |
| `POST /machines` | `serialNumber`, `name`, `location`, `description?` | 201 machine | A/P/E |
| `GET /machines/{id}` | None | 200 machine | All |
| `PATCH /machines/{id}` | Registry fields and `expectedVersion` | 200 machine | A/P/E |
| `PATCH /machines/{id}/status` | `status`, `reasonCode?`, `description?`, `expectedVersion` | 200 machine and related downtime | A/P/E/T/O |
| `DELETE /machines/{id}` | `expectedVersion` query parameter | 200 archived machine | A/P/E |

Serial numbers are trimmed and uppercased before uniqueness validation. A
machine starts `IDLE`. `RUNNING` requires an active Production Order. Entering
`DOWN` requires a downtime reason. A direct transition away from `DOWN` is a
`409`; the downtime resolve endpoint must be used.

## Downtime events

| Method and path | Request | Success | Roles |
|---|---|---:|---|
| `GET /downtime-events` | Page, machine, open, from, to | 200 page | All |
| `GET /downtime-events/{id}` | None | 200 event | All |
| `POST /downtime-events` | `machineId`, `reasonCode`, `description?`, `expectedMachineVersion` | 201 event and machine | A/P/E/T/O |
| `PATCH /downtime-events/{id}/resolve` | `resolutionNote`, `expectedVersion`, `expectedMachineVersion` | 200 event and machine | A/P/E/T/O |

Reason codes are `BREAKDOWN`, `SETUP`, `MATERIAL_SHORTAGE`, and `OTHER`.
Entering `DOWN` and opening its event are one transaction. Resolving an event,
closing it, and setting the machine to `IDLE` are one transaction. A machine
has at most one unresolved event. MVP does not permit backdating or editing
historical intervals. Repeated create/resolve requests return `409` rather
than creating duplicate state.

## Production Orders

The lifecycle is `DRAFT -> RELEASED -> IN_PROGRESS -> COMPLETED`. `DRAFT`,
`RELEASED`, and `IN_PROGRESS` may also become `CANCELLED`.

| Method and path | Request | Success | Roles |
|---|---|---:|---|
| `GET /production-orders` | Page, machine, status, search | 200 page | All |
| `GET /production-orders/{id}` | None | 200 order | All |
| `POST /production-orders` | `machineId`, `productCode`, `productDescription?`, `plannedQuantity` | 201 draft order | A/P |
| `PATCH /production-orders/{id}` | Draft fields and `expectedVersion` | 200 order | A/P |
| `POST /production-orders/{id}/release` | `expectedVersion` | 200 order | A/P |
| `POST /production-orders/{id}/start` | `expectedVersion`, `expectedMachineVersion` | 200 order and machine | A/P/O |
| `PATCH /production-orders/{id}/output` | `goodQuantity`, `scrapQuantity`, `expectedVersion` | 200 order | A/P/O |
| `POST /production-orders/{id}/complete` | `closureNote?`, `expectedVersion`, `expectedMachineVersion` | 200 order and machine | A/P/O |
| `POST /production-orders/{id}/cancel` | `closureNote`, `expectedVersion`, `expectedMachineVersion` | 200 order and machine | A/P |

Only draft orders permit machine, product, and planned-quantity edits. Starting
requires `IDLE`, sets the machine to `RUNNING`, and is limited to one
`IN_PROGRESS` order per machine. Downtime leaves the order `IN_PROGRESS`.
Output fields replace cumulative totals and must be non-negative; stale writes
return `409`. Completion requires positive total output. Completing early
requires a note. Terminal orders are immutable. Completing or cancelling an
active order returns the machine to `IDLE`, unless it is currently `DOWN`.

## Maintenance work orders

The lifecycle is `OPEN -> ASSIGNED -> IN_PROGRESS -> COMPLETED`; any
nonterminal state may become `CANCELLED` through an authorized manager action.

| Method and path | Request | Success | Roles |
|---|---|---:|---|
| `GET /maintenance/work-orders` | Page, machine, status, assignee, priority | 200 page | All |
| `GET /maintenance/work-orders/{id}` | None | 200 work order | All |
| `POST /maintenance/work-orders` | `machineId`, `title`, `description`, `downtimeEventId?` | 201 open work order | A/P/E/T/O |
| `PATCH /maintenance/work-orders/{id}` | `title?`, `description?`, `priority?`, `assignedTo?`, `dueAt?`, `status?`, `completionNote?`, `cancellationNote?`, `expectedVersion` | 200 work order | Field/state restricted |

Priority values are `LOW`, `MEDIUM`, `HIGH`, and `CRITICAL`. New work orders
start `OPEN`, `MEDIUM`, and unassigned. A/P/E may edit planning fields, assign,
reassign, and perform lifecycle actions. Technicians may update lifecycle and
completion fields only on their own assigned work orders. Operators cannot
update after creation. Completion requires a note and does not resolve
downtime. Terminal work orders are immutable. A linked downtime event must
belong to the same machine.

The implementation must reject the whole request if any supplied field is
unauthorized; it must not silently ignore forbidden fields.

For A/P/E, assigning an OPEN record moves it to ASSIGNED automatically. Clearing `assignedTo` in ASSIGNED returns it to OPEN; clearing during IN_PROGRESS is rejected, while reassignment preserves IN_PROGRESS. ASSIGNED, IN_PROGRESS and COMPLETED require an assignee. A combined assignment/start PATCH is allowed after validating both transitions. Reject contradictory explicit status/assignment values, backward transitions, or completion that skips IN_PROGRESS. Omitted fields stay unchanged; null clears only nullable planning fields (`assignedTo` under these rules and `dueAt`). Cancellation requires `cancellationNote`.

## Dashboard summary

`GET /dashboard/summary?from=<UTC>&to=<UTC>` is available to All roles. The
range is required, half-open as `[from,to)`, and limited to 90 days. The UI
defaults to the current plant-local day. The response includes `asOf`,
`range`, `machineCounts`, `downtimeSeconds`, `maintenanceCounts`, and
`completedProduction`.

Definitions:

- Machine counts are current non-archived machines grouped by reported status.
- Downtime is the sum of each event's overlap with the selected range. Open
  events close for calculation at `min(asOf, to)`.
- Maintenance counts cover current nonterminal work orders by status and
  priority; overdue means `dueAt < asOf`.
- Completed production counts orders completed in the range and uses each
  order's final good/scrap totals.
- Production output is attributed to completion time, not inferred per-unit
  production time.
- Current-state cards are labeled `Current`; interval metrics show the range.
- Historical intervals retain records for subsequently archived machines.
- OEE, utilization, and shift-level output are excluded from MVP.

The server takes one `asOf` snapshot for all calculations. Reporting queries
are read-only, indexed, and must be reconciled against underlying records.

## Contract requirements

The OpenAPI contract must document every path above, payload field, enum,
wrapper, permission, validation rule, and representative `400`, `401`, `403`,
`404`, `409`, and `429` response. All mutable records use optimistic numeric
`version` checks. Operations that coordinate machine state lock the machine
row before the associated record (after eligibility user locks when required), and insert business audit records in
the same transaction.

---

## Version 2 Telemetry & IIoT Protocol APIs (`/api/v2/telemetry`)

Introduced in Sprint 6 (Epic 4: Story E4-S1) to support automated machine telemetry ingestion from edge gateways and PLC controllers (OPC-UA, MQTT Sparkplug B, Modbus TCP).

| Method and path | Request | Success | Roles |
|---|---|---:|---|
| `POST /api/v2/telemetry/machines/{id}/tags` | `tagName`, `protocol`, `tagAddress`, `dataType?`, `unitOfMeasure?`, `scaleFactor?` | 201 TagMapping | Admin, Engineer |
| `GET /api/v2/telemetry/machines/{id}/tags` | None | 200 list | All Authenticated |
| `DELETE /api/v2/telemetry/machines/{id}/tags/{mappingId}` | None | 200 void | Admin |
| `POST /api/v2/telemetry/ingest` | `machineId`, `gatewayId?`, `points: [{tagName, value, unit?, quality?, timestamp?}]` | 200 TelemetryIngestResponse | Admin, Engineer |
| `GET /api/v2/telemetry/machines/{id}/live` | None | 200 MachineLiveTelemetry | All Authenticated |
| `GET /api/v2/telemetry/machines/{id}/history` | `limit?` (default 50, max 200) | 200 list | All Authenticated |
| `GET /api/v2/telemetry/machines/{id}/series` | Query: `tag` (required), `from?` (ISO-8601), `to?` (ISO-8601), `bucket?` (`1s`, `1m`, `1h`, `AUTO`) | 200 TimeSeriesResponse | All Authenticated |
| `POST /api/v2/telemetry/retention/execute` | None | 200 RetentionExecutionReport | Admin |

### Rules & Invariants
- **Protocols Supported:** `OPC_UA`, `MQTT_SPARKPLUG_B`, `MODBUS_TCP`.
- **Quality Indicators:** `GOOD`, `BAD`, `UNCERTAIN`.
- **Tag Uniqueness:** Tag names are normalized uppercase and unique per machine.
- **Scaling:** Ingested raw sensor register values are multiplied by the registered `scaleFactor`.
- **Anomaly Detection:** ISO 10816 vibration thresholds ($> 4.5$ mm/s) and thermal thresholds ($> 80.0$ °C) trigger automatic warning alerts in the ingestion response.
- **Timestamp Filtering:** Timestamps older than 7 days or more than 5 minutes in the future are clamped to server ingest time to maintain partition sanity.
- **Continuous Downsampling Rollups (E4-S2):**
  - High-frequency 1s raw telemetry points are continuously aggregated into 1-minute (`machine_telemetry_rollups_1m`) and 1-hour (`machine_telemetry_rollups_1h`) buckets recording `avg_value`, `min_value`, `max_value`, and `sample_count`.
  - Auto-resolution logic selects optimal bucket based on query date range:
    - Range $\le 2$ hours: `1s` raw telemetry resolution.
    - Range $\le 7$ days: `1m` continuous rollup resolution.
    - Range $> 7$ days: `1h` continuous rollup resolution.
  - Query SLA: Under 200 ms latency guaranteed for 30-day analytics windows (720 hourly points).
- **Storage Retention Policy (E4-S2):**
  - Default retention: 7 days for raw telemetry, 30 days for 1-minute rollups, 365 days for 1-hour rollups.
  - Automated pruning executed via `POST /api/v2/telemetry/retention/execute` without blocking incoming writes.

---

## Version 2 Automated Downtime & Micro-Stop Detection APIs (`/api/v2/downtime`)

Introduced in Sprint 7 (Epic 4: Story E4-S3) to eliminate manual operator logging for transient stoppages and enforce real-time OEE root cause attribution.

| Method and path | Request | Success | Roles |
|---|---|---:|---|
| `GET /api/v2/downtime/pending-root-causes` | None | 200 list | All Authenticated |
| `POST /api/v2/downtime/events/{id}/acknowledge-root-cause` | `reasonCode`, `resolutionNote?` | 200 DowntimeEventDto | Operator, Technician, Engineer, Production Manager, Admin |
| `GET /api/v2/downtime/machines/{id}/micro-stops` | Query: `from?`, `to?` (ISO-8601) | 200 MicroStopSummaryDto | All Authenticated |
| `POST /api/v2/downtime/machines/{id}/evaluate` | None | 200 AutomatedEvaluationResultDto | Engineer, Production Manager, Admin |

### Rules & Invariants
- **Machine State Precondition:** Automated downtime transitions only evaluate when an asset has an active `ProductionOrder` in status `IN_PROGRESS`. Idle assets between shifts/orders are not flagged as unplanned downtime.
- **5-Second Transition SLA:** Stoppage of part counter pulse / zero spindle speed while `IN_PROGRESS` transitions machine state to `DOWN` and opens an automated downtime event (`triggerSource: AUTOMATED_SENSOR`).
- **Heartbeat Timeout Protection:** Inactivity beyond heartbeat threshold (e.g. 15s without telemetry) triggers `DOWN` with `triggerSource: HEARTBEAT_TIMEOUT`.
- **Micro-Stop Auto-Resolution ($< 180$ seconds / 3 minutes):**
  - When part pulses resume before 180 seconds, the event is automatically closed with `isMicroStop = true`, `reasonCode = MICRO_STOP`, `resolvedBy = null`, and machine restored to `RUNNING`.
  - Line operators do not face nuisance popups for brief feeder jams or momentary pauses.
- **Operator Root-Cause Gate ($\ge 180$ seconds):**
  - Once stoppage duration reaches 180 seconds, `rootCausePromptedAt` is timestamped.
  - An attention prompt appears across operator touchscreens requesting verified root-cause classification (`TOOLING_JAM`, `MATERIAL_SHORTAGE`, `BREAKDOWN`, `OPERATOR_PAUSE`, `UNPLANNED_MAINTENANCE`, `SETUP`, `OTHER`).
  - Upon cycle resumption, the event is recorded with the operator's acknowledged reason code and audit trail.
- **Concurrency & Partial Unique Constraint:**
  - Guaranteed race-free execution via PostgreSQL partial index `uq_downtime_one_open_per_machine` on `(machine_id) WHERE end_time IS NULL`.


