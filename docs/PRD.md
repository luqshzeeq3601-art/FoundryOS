# FactoryOS Product Requirements Document

## 1. Problem and goals

Manufacturing teams currently maintain disconnected spreadsheets for machines,
production, downtime, and maintenance. This creates conflicting records,
delayed reporting, and weak accountability.

FactoryOS provides a shared operational record that enables teams to:

1. Identify each machine's current reported condition.
2. Record and resolve downtime consistently.
3. Assign and complete maintenance work.
4. Track basic production execution and reported output.
5. Review operational KPIs using documented formulas.
6. Attribute consequential changes to authenticated users.

### MVP success criteria

- Every active machine has one authoritative reported status.
- A machine has at most one unresolved downtime event.
- Status changes and related downtime records never partially commit.
- Work orders have traceable ownership and lifecycle history.
- Dashboard values reconcile with underlying records.
- Unauthorized requests fail at the backend regardless of frontend controls.

## 2. Users and permissions

A user has exactly one active role in MVP. Roles apply across the plant. Access
requires an active, non-deleted user.

Legend: `✓` permitted; `Own` means assigned technician only; `—` denied.

| Permission | Admin | Production Manager | Engineer | Technician | Operator | Viewer |
|---|---:|---:|---:|---:|---:|---:|
| Read operational records/dashboard | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Manage users and roles | ✓ | — | — | — | — | — |
| Create/edit/archive machines | ✓ | ✓ | ✓ | — | — | — |
| Change machine status | ✓ | ✓ | ✓ | ✓ | ✓ | — |
| Open/resolve downtime | ✓ | ✓ | ✓ | ✓ | ✓ | — |
| Create/edit/release/cancel production orders | ✓ | ✓ | — | — | — | — |
| Start/report/complete production orders | ✓ | ✓ | — | — | ✓ | — |
| Create maintenance work orders | ✓ | ✓ | ✓ | ✓ | ✓ | — |
| Set priority, assign or reassign maintenance | ✓ | ✓ | ✓ | — | — | — |
| Start/complete maintenance work | ✓ | ✓ | ✓ | Own | — | — |
| Cancel maintenance work | ✓ | ✓ | ✓ | — | — | — |
| Read security/audit records through restricted operations tooling | ✓ | — | — | — | — | — |

All roles may log in, refresh a session, log out, and change their own
password. Backend service methods enforce permissions and assignment
constraints; frontend permissions only control presentation. A user cannot
deactivate, demote, or delete the final active Admin.

An assigned Technician cannot be deactivated, archived, or moved to another role while nonterminal assigned maintenance exists. Reassign or cancel the work first; concurrent account and assignment changes must preserve this rule.

## 3. Phased scope

| Phase | Included | Boundary |
|---|---|---|
| MVP | Auth, RBAC, machines, basic Production Orders, downtime, maintenance, dashboard, essential audit records | Manual entry; single plant; online only |
| Version 2 | Inventory, quality inspection, OEE, WebSockets, searchable audit UI, in-app notifications, optional Redis | OEE requires validated production-calendar and ideal-cycle-time inputs |
| Version 3 | Potential service extraction, Kafka, MQTT ingestion, Kubernetes | Requires demonstrated operational need and separate architecture decisions |

Version 3 items are options, not mandatory migrations.

### Production Order boundary

- One machine per order.
- Product code and description are entered directly.
- Planned quantity, cumulative good quantity, and cumulative scrap quantity are supported.
- No BOM, inventory reservation, scheduling engine, ERP integration, or multi-operation routing.

## 4. Functional requirements

| ID | Requirement |
|---|---|
| FR-01 | Authenticate active users and issue a short-lived access token plus refresh cookie. |
| FR-02 | Rotate refresh tokens and reject expired, revoked, or replayed tokens. |
| FR-03 | Allow Admins to create users, change roles, and deactivate users. |
| FR-04 | Enforce the permission matrix on every protected operation. |
| FR-05 | Create, search, update, and archive machines with unique serial numbers. |
| FR-06 | Entering `DOWN` creates exactly one unresolved downtime event in the same transaction. |
| FR-07 | Resolving downtime closes the event and sets the machine to `IDLE` in the same transaction. |
| FR-08 | Create and execute Production Orders through the defined lifecycle. |
| FR-09 | Validate non-negative cumulative output counts and prevent silent concurrent overwrites. |
| FR-10 | Create, assign, progress, complete, and cancel maintenance work orders. |
| FR-11 | Restrict Technician lifecycle updates to their assigned work orders. |
| FR-12 | Display machine counts, downtime duration, maintenance backlog, and production output. |
| FR-13 | Persist actor-attributed audit records for consequential business changes. |
| FR-14 | Support server-side pagination, approved filters, and sorting for record lists. |
| FR-15 | Present tablet-friendly operational forms and clear network/conflict errors. |
| FR-16 | Preserve historical references when users or machines are archived. |

### Non-functional requirements

| ID | Requirement and acceptance target |
|---|---|
| NFR-01 | Under the defined load profile, API p95 latency is below 500 ms, excluding password hashing endpoints and external network latency. |
| NFR-02 | Passwords use BCrypt cost 12; plaintext passwords never appear in storage or logs. |
| NFR-03 | Every permission matrix denial is covered by automated backend authorization tests. |
| NFR-04 | Successful business mutations and their audit entries commit atomically. |
| NFR-05 | Operational interfaces work at 768 px tablet and 1280 px desktop widths without page-level horizontal scrolling. |
| NFR-06 | Transaction boundaries and database constraints prevent duplicate open downtime and competing active orders. |
| NFR-07 | Monthly availability objective is 99.5%, excluding announced maintenance. |
| NFR-08 | Backup design targets RPO ≤15 minutes and RTO ≤4 hours, validated by restoration drills. |
| NFR-09 | Logs and metrics expose failures and latency without recording credentials or sensitive request bodies. |
| NFR-10 | Controls support keyboard navigation, visible focus, labeled inputs, and status indicators beyond color alone. |
| NFR-11 | Persisted timestamps use UTC; plant-local rendering uses configurable IANA timezone, default `Asia/Kuala_Lumpur`. |

### Performance acceptance profile

- Host: 4 vCPU, 8 GB RAM, SSD storage.
- Dataset: 500 machines, 100,000 downtime events, 100,000 work orders, and 100,000 production orders.
- Fifty concurrent virtual users for 15 minutes after warm-up.
- Mix: 70% operational reads, 20% mutations, and 10% dashboard reads.
- Server-error rate below 1%; no lost or inconsistent committed updates.
- Login p95 below 2 seconds under five concurrent login attempts.

## 5. Primary use cases

1. An Admin provisions an Operator and assigns their role.
2. An Operator starts an order on an available machine.
3. An Operator reports a breakdown; the machine becomes `DOWN`.
4. A Technician receives and completes a maintenance work order.
5. An authorized user resolves the downtime; the machine becomes `IDLE`.
6. An Operator resumes production and reports cumulative output.
7. A Production Manager reviews the dashboard and supporting records.

## 6. UI hierarchy and responsive behavior

### Design Standards & Aesthetic Framework

- Follow [.agents/skills/design-taste-frontend](../.agents/skills/design-taste-frontend/SKILL.md) and [.agents/skills/industrial-brutalist-ui](../.agents/skills/industrial-brutalist-ui/SKILL.md) for anti-slop layout discipline, purposeful typography, high contrast, and tactile industrial telemetry.

### Navigation

- Dashboard
- Machines → list → detail → status, downtime, orders, maintenance
- Production Orders → list → detail/execution
- Downtime → active/history → detail/resolve
- Maintenance → list → detail/update
- Administration → users
- Account → change password/logout

### Desktop

- Persistent navigation and filterable data tables.
- Dashboard summary cards link to supporting records.
- Detail pages present status, ownership, history, and permitted actions.

### Tablet

- Collapsible navigation and card-based operational lists.
- Minimum 48 px primary touch targets.
- Primary status/update action remains easy to reach.
- Short forms, large selectors, explicit save feedback.
- Destructive actions require confirmation; routine updates do not.

### Shared behavior

- Disable repeated submission while a request is pending.
- Preserve unsaved form input after recoverable network errors.
- On version conflict, show a refresh-and-review action.
- On connectivity loss, show an offline banner; do not queue writes.
- Poll visible dashboard/status views every 30 seconds and show last refresh time.
