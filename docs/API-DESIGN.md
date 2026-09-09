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
